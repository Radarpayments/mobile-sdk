package net.payrdr.mobile.payment.sdk.qa.launcher

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkPaymentApiException
import net.payrdr.mobile.payment.sdk.payment.logger.LogLevel
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.qa.config.OrderSource
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.config.launchProblem
import net.payrdr.mobile.payment.sdk.qa.journal.Journal
import net.payrdr.mobile.payment.sdk.qa.merchant.Merchant
import net.payrdr.mobile.payment.sdk.qa.merchant.OrderStatus
import net.payrdr.mobile.payment.sdk.qa.merchant.Registration

/** How a run ended; [id] is the `qa_result_type` text of the contract. */
internal enum class ResultType(
    val id: String,
) {
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELED("canceled"),
    UNKNOWN("unknown"),
    REGISTRATION_FAILED("registration_failed"),
    SDK_ERROR("sdk_error"),
}

/** The order the sheet is opened with: a session id when there is one, the mdOrder otherwise. */
internal data class OpenOrder(
    val mdOrder: String?,
    val sessionId: String?,
)

/**
 * What a run ended with. [error] is the SDK's error text, the gateway's answer to a failed registration, or the class
 * and message of an exception from the sheet; [httpStatus] is the gateway's HTTP status when it named one.
 */
internal data class RunOutcome(
    val type: ResultType,
    val mdOrder: String?,
    val sessionId: String?,
    val error: String?,
    val httpStatus: Int?,
    val status: OrderStatus,
)

internal sealed interface RunState {
    data object Idle : RunState

    data class Invalid(
        val problem: Problem,
    ) : RunState

    data object Registering : RunState

    data class InSheet(
        val order: OpenOrder,
    ) : RunState

    data object QueryingStatus : RunState

    data class Finished(
        val outcome: RunOutcome,
    ) : RunState
}

/**
 * One QA run: checks the configuration, clears the journal, registers the order (or takes the configured one), has
 * [present] open the sheet, takes its result in [onResult], asks the gateway for the order's status and finishes.
 * A launch while a run is under way is ignored.
 */
internal class QaRun(
    private val merchant: Merchant,
    private val journal: Journal,
    private val scope: CoroutineScope,
    private val present: (OpenOrder) -> Unit,
) {
    private val mutableState = MutableStateFlow<RunState>(RunState.Idle)
    val state: StateFlow<RunState> = mutableState.asStateFlow()

    private val lock = Any()

    // The run allowed to change the state. Its coroutines write only while it is still current, so a registration or
    // a status query that answers after a reset cannot overwrite the run started since.
    private var current: Attempt? = null

    fun launch(
        config: QaConfig,
        profile: StandProfile?,
    ) {
        val job =
            synchronized(lock) {
                if (mutableState.value.isBusy()) return
                val problem = launchProblem(config, profile)
                if (problem != null || profile == null) {
                    mutableState.value = RunState.Invalid(problem ?: Problem("profile", "select a stored profile"))
                    return
                }
                journal.clear()
                val attempt = Attempt(config, profile)
                current = attempt
                mutableState.value = config.givenOrder()?.let(RunState::InSheet) ?: RunState.Registering
                attempt.start { open(attempt) }
            }
        job.start()
    }

    /** The sheet's result; ignored unless a sheet of the current run is open. */
    fun onResult(result: CheckoutResult) {
        val job =
            synchronized(lock) {
                val attempt = current ?: return
                val sheet = mutableState.value as? RunState.InSheet ?: return
                mutableState.value = RunState.QueryingStatus
                attempt.start { finish(attempt, sheet.order, result) }
            }
        job.start()
    }

    fun reset() {
        synchronized(lock) {
            current?.job?.cancel()
            current = null
            mutableState.value = RunState.Idle
        }
    }

    private suspend fun open(attempt: Attempt) {
        // A given order is in the sheet's state since the launch; a registered one moves there only if still current.
        val given = attempt.config.givenOrder()
        val order =
            if (given != null) {
                given.takeIf { synchronized(lock) { current === attempt } }
            } else {
                registered(attempt)?.takeIf { attempt.move(RunState.InSheet(it)) }
            } ?: return
        val failure = runCatching { present(order) }.exceptionOrNull() ?: return
        if (failure is CancellationException) throw failure
        val error = listOfNotNull(failure.javaClass.simpleName.ifEmpty { failure.javaClass.name }, failure.message).joinToString(": ")
        val outcome = RunOutcome(ResultType.SDK_ERROR, order.mdOrder, order.sessionId, error, null, OrderStatus.NotQueried)
        synchronized(lock) {
            if (current !== attempt || mutableState.value !is RunState.InSheet) return
            journal.record(LogLevel.ERROR, TAG, "The sheet did not open: $error")
            mutableState.value = RunState.Finished(outcome)
        }
    }

    private suspend fun registered(attempt: Attempt): OpenOrder? =
        when (val registration = merchant.register(attempt.config.order, attempt.profile)) {
            is Registration.Registered -> OpenOrder(registration.mdOrder, registration.sessionId)
            is Registration.Failed -> {
                val outcome =
                    RunOutcome(
                        ResultType.REGISTRATION_FAILED,
                        null,
                        null,
                        registration.message,
                        registration.httpStatus,
                        OrderStatus.NotQueried,
                    )
                attempt.move(RunState.Finished(outcome))
                null
            }
        }

    private suspend fun finish(
        attempt: Attempt,
        order: OpenOrder,
        result: CheckoutResult,
    ) {
        // After a session the app knows the order only from the result; an empty orderId means the sheet closed first.
        val mdOrder = order.mdOrder ?: result.orderId.ifBlank { null }
        val status = merchant.status(mdOrder, attempt.profile)
        val error = result.error()
        val outcome = RunOutcome(result.type(), mdOrder, order.sessionId, error?.message, error?.httpStatus(), status)
        attempt.move(RunState.Finished(outcome))
    }

    private fun RunState.isBusy(): Boolean = this is RunState.Registering || this is RunState.InSheet || this is RunState.QueryingStatus

    private fun QaConfig.givenOrder(): OpenOrder? =
        when (order.source) {
            OrderSource.REGISTER -> null
            OrderSource.MD_ORDER -> OpenOrder(order.mdOrder, null)
            OrderSource.SESSION_ID -> OpenOrder(null, order.sessionId)
        }

    // Lazy, so the job is known before it runs: a scope on an immediate dispatcher would otherwise run it in place.
    private fun Attempt.start(block: suspend () -> Unit): Job = scope.launch(start = CoroutineStart.LAZY) { block() }.also { job = it }

    private fun Attempt.move(next: RunState): Boolean =
        synchronized(lock) {
            (current === this).also { if (it) mutableState.value = next }
        }

    private class Attempt(
        val config: QaConfig,
        val profile: StandProfile,
    ) {
        var job: Job? = null
    }

    private companion object {
        const val TAG = "qa"
    }
}

private fun CheckoutResult.type(): ResultType =
    when (this) {
        is CheckoutResult.Completed -> ResultType.COMPLETED
        is CheckoutResult.Failed -> ResultType.FAILED
        is CheckoutResult.Canceled -> ResultType.CANCELED
        is CheckoutResult.Unknown -> ResultType.UNKNOWN
    }

private fun CheckoutResult.error(): SdkException? =
    when (this) {
        is CheckoutResult.Failed -> error
        is CheckoutResult.Unknown -> cause
        is CheckoutResult.Completed, is CheckoutResult.Canceled -> null
    }

private fun SdkException.httpStatus(): Int? = (this as? SdkPaymentApiException)?.httpStatus
