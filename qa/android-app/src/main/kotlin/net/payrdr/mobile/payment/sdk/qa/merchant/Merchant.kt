package net.payrdr.mobile.payment.sdk.qa.merchant

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.payrdr.mobile.payment.sdk.payment.logger.LogLevel
import net.payrdr.mobile.payment.sdk.qa.config.ApiVersion
import net.payrdr.mobile.payment.sdk.qa.config.Currencies
import net.payrdr.mobile.payment.sdk.qa.config.OrderConfig
import net.payrdr.mobile.payment.sdk.qa.config.OrderType
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.journal.Journal
import java.io.IOException

/**
 * The merchant's backend, played from the device: registers the order the SDK then pays, and asks its status
 * afterwards, with the stand profile's credentials. Every exchange is journaled under `merchant`, the password and the
 * API key masked. Never throws for a gateway or network problem: the result says what went wrong.
 */
internal class Merchant(
    private val http: HttpClient,
    private val journal: Journal,
    private val timeoutMillis: Long = 20_000,
    private val orderNumber: () -> String = ::newOrderNumber,
) {
    suspend fun register(
        order: OrderConfig,
        profile: StandProfile,
    ): Registration {
        val secrets = GatewaySecrets.of(profile)
        val failed = { message: String -> Registration.Failed(null, message) }
        val request =
            when {
                profile.api == ApiVersion.V2 && order.type == OrderType.AFT ->
                    return failed("An AFT order is registered with registerP2P.do of API v1, and ${profile.name} is an API v2 profile")
                profile.api == ApiVersion.V2 ->
                    GatewayRequest.Json(sessionUrlOrNull(profile) ?: return failed(notAUrl(profile)), sessionJson(order), profile.apiKey)
                Currencies.numeric(order.currency) == null -> return failed("Unknown currency ${order.currency}")
                order.type == OrderType.AFT -> GatewayRequest.Json(transferUrl(profile), transferJson(order, profile, orderNumber()))
                else -> GatewayRequest.Form(registerUrl(profile), registerForm(order, profile, orderNumber()))
            }
        val answer = if (profile.api == ApiVersion.V2) ::sessionAnswer else ::registerAnswer
        return exchange(request, secrets, failed) { status, body -> answer(status, body, secrets) }
    }

    /** Asks API v1 for the order's status; [OrderStatus.NotQueried] for API v2, without credentials or without [orderId]. */
    suspend fun status(
        orderId: String?,
        profile: StandProfile,
    ): OrderStatus {
        if (!profile.hasV1Credentials() || orderId.isNullOrBlank()) return OrderStatus.NotQueried
        val secrets = GatewaySecrets.of(profile)
        val request = GatewayRequest.Form(statusUrl(profile), statusForm(orderId, profile))
        return exchange(request, secrets, { OrderStatus.Failed(null, it) }) { status, body -> statusAnswer(status, body, secrets) }
    }

    private suspend fun <T> exchange(
        request: GatewayRequest,
        secrets: GatewaySecrets,
        failed: (String) -> T,
        answer: (Int, String) -> T,
    ): T {
        journal.record(LogLevel.INFO, TAG, request.printed(secrets))
        return when (val outcome = call(request, secrets)) {
            is Reply -> {
                val level = if (outcome.status in 200..299) LogLevel.INFO else LogLevel.WARNING
                val line = "← ${outcome.status} ${outcome.body}"
                journal.record(level, TAG, secrets.bounded(line, GatewayRequest.JOURNAL_LIMIT, GatewayRequest.CUT_MARKER))
                answer(outcome.status, outcome.body)
            }
            is NoAnswer -> {
                journal.record(LogLevel.WARNING, TAG, "← ${outcome.message}")
                failed(outcome.message)
            }
        }
    }

    // Real time on the IO dispatcher, as the sample's server calls: a stalled DNS lookup must not hold the caller.
    // The body is read inside the timeout too, since a gateway may send the headers and then stall.
    private suspend fun call(
        request: GatewayRequest,
        secrets: GatewaySecrets,
    ): Outcome =
        try {
            withContext(Dispatchers.IO) { withTimeoutOrNull(timeoutMillis) { send(request) } }
                ?: NoAnswer("The gateway did not answer in ${timeoutMillis / MILLIS_PER_SECOND} s")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: IOException) {
            noAnswer(e, secrets)
        } catch (e: IllegalStateException) {
            // Ktor reports a malformed URL and a closed client this way.
            noAnswer(e, secrets)
        } catch (e: IllegalArgumentException) {
            // An unresolvable address comes as UnresolvedAddressException.
            noAnswer(e, secrets)
        }

    // OkHttp may report the caller's cancellation as an IOException ("Canceled"): that is still a cancellation.
    private suspend fun noAnswer(
        error: Exception,
        secrets: GatewaySecrets,
    ): NoAnswer {
        currentCoroutineContext().ensureActive()
        val text = listOfNotNull(error::class.simpleName, error.message).joinToString(": ")
        return NoAnswer(secrets.bounded(text, MESSAGE_LIMIT))
    }

    private suspend fun send(request: GatewayRequest): Reply {
        val response =
            when (request) {
                is GatewayRequest.Form -> http.submitForm(request.url, request.parameters)
                is GatewayRequest.Json ->
                    http.post(request.url) {
                        request.apiKey?.let { header(API_KEY_HEADER, it) }
                        setBody(TextContent(request.body.toString(), ContentType.Application.Json))
                    }
            }
        return Reply(response.status.value, response.bodyAsText())
    }

    private fun StandProfile.hasV1Credentials(): Boolean = api == ApiVersion.V1 && login.isNotBlank() && password.isNotBlank()

    private fun sessionUrlOrNull(profile: StandProfile): String? = runCatching { sessionUrl(profile) }.getOrNull()

    private fun notAUrl(profile: StandProfile): String = "The gateway URL of ${profile.name} is not a URL: ${profile.gatewayUrl}"

    private sealed interface Outcome

    // The body may quote the credentials back; toString keeps it out of any accidental print.
    private class Reply(
        val status: Int,
        val body: String,
    ) : Outcome {
        override fun toString(): String = "Reply($status)"
    }

    private class NoAnswer(
        val message: String,
    ) : Outcome

    private companion object {
        const val TAG = "merchant"
        const val API_KEY_HEADER = "X-API-KEY"
        const val MILLIS_PER_SECOND = 1_000
        const val MESSAGE_LIMIT = 300
    }
}
