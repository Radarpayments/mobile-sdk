package net.payrdr.mobile.payment.sdk.qa.launcher

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkPaymentApiException
import net.payrdr.mobile.payment.sdk.payment.logger.LogLevel
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.qa.config.OrderConfig
import net.payrdr.mobile.payment.sdk.qa.config.OrderSource
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.merchant.FakeGateway
import net.payrdr.mobile.payment.sdk.qa.merchant.OrderStatus
import net.payrdr.mobile.payment.sdk.qa.merchant.V1
import net.payrdr.mobile.payment.sdk.qa.merchant.V2
import net.payrdr.mobile.payment.sdk.qa.merchant.ok
import kotlin.test.Test

class QaRunTest {
    private val register = QaConfig(profile = "v1")
    private val session = QaConfig(profile = "v2")

    private fun existing(mdOrder: String) = QaConfig(profile = "v1", order = OrderConfig(source = OrderSource.MD_ORDER, mdOrder = mdOrder))

    private fun existingSession(sessionId: String) =
        QaConfig(profile = "v1", order = OrderConfig(source = OrderSource.SESSION_ID, sessionId = sessionId))

    private class Harness(
        val gateway: FakeGateway,
        private val scope: CoroutineScope,
        present: (OpenOrder) -> Unit,
    ) {
        val opened = mutableListOf<OpenOrder>()
        val run =
            QaRun(gateway.merchant(), gateway.journal, scope) {
                opened += it
                present(it)
            }

        fun requests(path: String): List<String> = gateway.requests.map { it.url }.filter { it.endsWith(path) }

        suspend fun finished(): RunOutcome = finishedState().outcome

        suspend fun finishedState(): RunState.Finished = run.state.first { it is RunState.Finished } as RunState.Finished

        suspend fun inSheet(): OpenOrder = (run.state.first { it is RunState.InSheet } as RunState.InSheet).order

        // The run's coroutines are children of the test; the gateway answers on a real IO thread, which the test
        // scheduler's advanceUntilIdle does not wait for, so a stale answer is awaited by joining them.
        suspend fun idle() =
            scope.coroutineContext.job.children
                .toList()
                .joinAll()
    }

    private fun TestScope.harness(
        gateway: FakeGateway = FakeGateway { answer(it) },
        scope: CoroutineScope = this,
        present: (OpenOrder) -> Unit = {},
    ) = Harness(gateway, scope, present)

    @Test
    fun `a missing profile is reported and nothing is sent`() =
        runTest {
            val h = harness()

            h.run.launch(register, null)
            h.idle()

            h.run.state.value shouldBe RunState.Invalid(Problem("profile", "select a stored profile"))
            h.gateway.requests.size shouldBe 0
            h.opened shouldBe emptyList()
        }

    @Test
    fun `a registered order opens the sheet with its mdOrder`() =
        runTest {
            val h = harness()

            h.run.launch(register, V1)

            h.inSheet() shouldBe OpenOrder("abc", null)
            h.opened shouldBe listOf(OpenOrder("abc", null))
            h.requests("/rest/register.do").size shouldBe 1
        }

    @Test
    fun `a session order opens the sheet with its session id`() =
        runTest {
            val h = harness()

            h.run.launch(session, V2)

            h.inSheet() shouldBe OpenOrder(null, "session-1")
            h.opened shouldBe listOf(OpenOrder(null, "session-1"))
        }

    @Test
    fun `an existing mdOrder opens without registering`() =
        runTest {
            val h = harness()

            h.run.launch(existing("given"), V1.copy(login = "", password = ""))

            h.inSheet() shouldBe OpenOrder("given", null)
            h.gateway.requests.size shouldBe 0
        }

    @Test
    fun `an existing session opens without registering`() =
        runTest {
            val h = harness()

            h.run.launch(existingSession("session-2"), V1)

            h.inSheet() shouldBe OpenOrder(null, "session-2")
            h.gateway.requests.size shouldBe 0
        }

    @Test
    fun `a failed registration finishes with the gateway's answer`() =
        runTest {
            val h = harness(FakeGateway { ok("""{"errorCode":"5","errorMessage":"Access denied"}""") })

            h.run.launch(register, V1)

            h.finished() shouldBe
                RunOutcome(ResultType.REGISTRATION_FAILED, null, null, "Access denied", 200, OrderStatus.NotQueried)
            h.opened shouldBe emptyList()
        }

    @Test
    fun `a finished run keeps the config and the profile it was launched with`() =
        runTest {
            val h = harness()
            val config = register.copy(order = register.order.copy(amount = 4_200))

            h.run.launch(config, V1)
            h.inSheet()
            h.run.onResult(CheckoutResult.Completed("abc"))

            val finished = h.finishedState()
            finished.config shouldBe config
            finished.profile shouldBe V1
        }

    @Test
    fun `a failed registration keeps the config and the profile it was launched with`() =
        runTest {
            val h = harness(FakeGateway { ok("""{"errorCode":"5","errorMessage":"Access denied"}""") })

            h.run.launch(register, V1)

            val finished = h.finishedState()
            finished.config shouldBe register
            finished.profile shouldBe V1
        }

    @Test
    fun `an sdk error keeps the config and the profile it was launched with`() =
        runTest {
            val h = harness(present = { error("boom") })

            h.run.launch(existing("given"), V2)

            val finished = h.finishedState()
            finished.config shouldBe existing("given")
            finished.profile shouldBe V2
        }

    @Test
    fun `a finished state prints no credentials`() =
        runTest {
            val h = harness()

            h.run.launch(register, V1)
            h.inSheet()
            h.run.onResult(CheckoutResult.Completed("abc"))

            val text = h.finishedState().toString()
            text shouldContain "dev.bpcbt.com"
            text shouldNotContain "login-1"
            text shouldNotContain "password-1"
            RunState.Finished(h.finished(), register, V2).toString() shouldNotContain "key-1"
        }

    @Test
    fun `a second launch during registration is ignored`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val h =
                harness(
                    FakeGateway {
                        gate.await()
                        answer(it)
                    },
                )

            h.run.launch(register, V1)
            h.run.launch(register, V1)
            h.run.state.value shouldBe RunState.Registering
            gate.complete(Unit)

            h.inSheet()
            h.run.launch(register, V1)
            h.idle()
            h.requests("/rest/register.do").size shouldBe 1
            h.opened.size shouldBe 1
        }

    @Test
    fun `a launch while the sheet is open is ignored`() =
        runTest {
            val h = harness()

            h.run.launch(existing("given"), V1)
            h.inSheet()
            h.run.launch(register, V1)
            h.idle()

            h.run.state.value shouldBe RunState.InSheet(OpenOrder("given", null))
            h.opened.size shouldBe 1
            h.gateway.requests.size shouldBe 0
        }

    @Test
    fun `a launch while the status is asked is ignored`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val h =
                harness(
                    FakeGateway {
                        if (it.url.encodedPath.endsWith(STATUS)) gate.await()
                        answer(it)
                    },
                )

            h.run.launch(register, V1)
            h.inSheet()
            h.run.onResult(CheckoutResult.Completed("abc"))
            h.run.state.value shouldBe RunState.QueryingStatus
            h.run.launch(register, V1)
            gate.complete(Unit)

            h.finished().type shouldBe ResultType.COMPLETED
            h.requests("/rest/register.do").size shouldBe 1
        }

    @Test
    fun `a completed payment asks the status of the registered order`() =
        runTest {
            val h = harness()

            h.run.launch(register, V1)
            h.inSheet()
            h.run.onResult(CheckoutResult.Completed("abc"))

            h.finished() shouldBe
                RunOutcome(ResultType.COMPLETED, "abc", null, null, null, OrderStatus.Known(2, 0, "Approved"))
            h.gateway.requests
                .single { it.url.endsWith(STATUS) }
                .form()["orderId"] shouldBe "abc"
        }

    @Test
    fun `a session run asks the status of the result's order id`() =
        runTest {
            val h = harness()

            h.run.launch(existingSession("session-2"), V1)
            h.inSheet()
            h.run.onResult(CheckoutResult.Completed("from-result"))

            val outcome = h.finished()
            outcome.mdOrder shouldBe "from-result"
            outcome.sessionId shouldBe "session-2"
            h.gateway
                .single()
                .form()["orderId"] shouldBe "from-result"
        }

    @Test
    fun `a canceled sheet finishes as canceled`() =
        runTest {
            val h = harness()

            h.run.launch(register, V1)
            h.inSheet()
            h.run.onResult(CheckoutResult.Canceled("abc"))

            h.finished().type shouldBe ResultType.CANCELED
        }

    @Test
    fun `an unknown result finishes as unknown`() =
        runTest {
            val h = harness()

            h.run.launch(register, V1)
            h.inSheet()
            h.run.onResult(CheckoutResult.Unknown("abc", SdkException("no network")))

            val outcome = h.finished()
            outcome.type shouldBe ResultType.UNKNOWN
            outcome.error shouldBe "no network"
        }

    @Test
    fun `a failed result carries the sdk's error`() =
        runTest {
            val h = harness()

            h.run.launch(register, V1)
            h.inSheet()
            h.run.onResult(CheckoutResult.Failed("abc", SdkException("Payment declined: 116")))

            val outcome = h.finished()
            outcome.type shouldBe ResultType.FAILED
            outcome.error shouldBe "Payment declined: 116"
            outcome.status shouldBe OrderStatus.Known(2, 0, "Approved")
        }

    @Test
    fun `an exception from the sheet is an sdk error`() =
        runTest {
            val h = harness(present = { error("boom") })

            h.run.launch(register, V1)

            val outcome = h.finished()
            outcome.type shouldBe ResultType.SDK_ERROR
            outcome.error shouldBe "IllegalStateException: boom"
            outcome.mdOrder shouldBe "abc"
            outcome.status shouldBe OrderStatus.NotQueried
            h.gateway.journalText() shouldContain "IllegalStateException: boom"
        }

    @Test
    fun `an exception without a message is named by its class alone`() {
        IllegalStateException().sheetError() shouldBe "IllegalStateException"
        IllegalStateException("  \n").sheetError() shouldBe "IllegalStateException"
    }

    @Test
    fun `an exception of an anonymous class is named by the last segment of its binary name`() {
        val anonymous = object : RuntimeException("boom") {}

        anonymous.javaClass.simpleName shouldBe ""
        anonymous.sheetError() shouldBe "${anonymous.javaClass.name.substringAfterLast('.')}: boom"
        anonymous.sheetError() shouldStartWith "QaRunTest$"
    }

    @Test
    fun `a gateway error of the sdk carries its http status`() =
        runTest {
            val h = harness()

            h.run.launch(register, V1)
            h.inSheet()
            h.run.onResult(CheckoutResult.Failed("abc", SdkPaymentApiException(httpStatus = 502, message = "Bad gateway")))

            val outcome = h.finished()
            outcome.error shouldBe "Bad gateway"
            outcome.httpStatus shouldBe 502
        }

    @Test
    fun `a cancellation the sheet throws on its own is an sdk error`() =
        runTest {
            val h = harness(present = { throw CancellationException("gone") })

            h.run.launch(existing("given"), V1)

            val outcome = h.finished()
            outcome.type shouldBe ResultType.SDK_ERROR
            outcome.error shouldBe "CancellationException: gone"
            h.gateway.journalText() shouldContain "CancellationException: gone"
        }

    @Test
    fun `a cancellation of the run's own coroutine stops it without a word`() =
        runTest {
            val runs = CoroutineScope(coroutineContext + Job(coroutineContext.job))
            val h =
                harness(scope = runs, present = {
                    runs.cancel()
                    throw CancellationException("gone")
                })

            h.run.launch(existing("given"), V1)
            h.idle()

            h.run.state.value shouldBe RunState.InSheet(OpenOrder("given", null))
            h.gateway.journal.entries.value shouldBe emptyList()
        }

    @Test
    fun `an exception after the sheet delivered its result is still journaled`() =
        runTest {
            lateinit var h: Harness
            h =
                harness(present = {
                    h.run.onResult(CheckoutResult.Canceled("given"))
                    error("boom")
                })

            h.run.launch(existing("given"), V1)

            h.finished().type shouldBe ResultType.CANCELED
            h.gateway.journalText() shouldContain "IllegalStateException: boom"
        }

    @Test
    fun `the journal is cleared when a run starts`() =
        runTest {
            val h = harness()
            h.gateway.journal.record(LogLevel.INFO, "earlier", "a line of the previous run")

            h.run.launch(existing("given"), V1)
            h.inSheet()

            h.gateway.journal.entries.value shouldBe emptyList()
        }

    @Test
    fun `an invalid launch keeps the journal`() =
        runTest {
            val h = harness()
            h.gateway.journal.record(LogLevel.INFO, "earlier", "a line of the previous run")

            h.run.launch(register, null)

            h.gateway.journal.entries.value.size shouldBe 1
        }

    @Test
    fun `a result that arrives with no run in the sheet is ignored`() =
        runTest {
            val h = harness()

            h.run.onResult(CheckoutResult.Completed("abc"))
            h.idle()

            h.run.state.value shouldBe RunState.Idle
            h.gateway.requests.size shouldBe 0
        }

    @Test
    fun `a second result of one sheet is ignored`() =
        runTest {
            val h = harness()

            h.run.launch(register, V1)
            h.inSheet()
            h.run.onResult(CheckoutResult.Completed("abc"))
            h.run.onResult(CheckoutResult.Canceled("abc"))

            h.finished().type shouldBe ResultType.COMPLETED
            h.requests(STATUS).size shouldBe 1
        }

    @Test
    fun `a status that cannot be asked still finishes with the checkout's result`() =
        runTest {
            val h = harness()

            h.run.launch(session, V2)
            h.inSheet()
            h.run.onResult(CheckoutResult.Completed("abc"))

            h.finished() shouldBe
                RunOutcome(ResultType.COMPLETED, "abc", "session-1", null, null, OrderStatus.NotQueried)
        }

    @Test
    fun `a failed status query still finishes with the checkout's result`() =
        runTest {
            val h =
                harness(
                    FakeGateway {
                        if (it.url.encodedPath.endsWith(STATUS)) {
                            ok("""{"errorCode":"6","errorMessage":"Order not found"}""")
                        } else {
                            answer(it)
                        }
                    },
                )

            h.run.launch(register, V1)
            h.inSheet()
            h.run.onResult(CheckoutResult.Canceled("abc"))

            val outcome = h.finished()
            outcome.type shouldBe ResultType.CANCELED
            outcome.status shouldBe OrderStatus.Failed(200, "Order not found")
        }

    @Test
    fun `reset returns to idle`() =
        runTest {
            val h = harness()

            h.run.launch(existing("given"), V1)
            h.inSheet()
            h.run.reset()

            h.run.state.value shouldBe RunState.Idle
        }

    // The identity fence in QaRun's move has no test of its own: here the reset cancels the gated coroutine first, so a
    // stale answer never reaches it. It matters only on a multi-threaded scope, where an answer can be past its last
    // suspension point when the reset comes, and that interleaving cannot be forced in a test.
    @Test
    fun `a registration that answers after a reset does not touch the next run`() =
        runTest {
            val sent = CompletableDeferred<Unit>()
            val gate = CompletableDeferred<Unit>()
            val h =
                harness(
                    FakeGateway {
                        sent.complete(Unit)
                        gate.await()
                        answer(it)
                    },
                )

            h.run.launch(register, V1)
            sent.await()
            h.run.reset()
            h.run.launch(existing("second"), V1)
            h.inSheet() shouldBe OpenOrder("second", null)
            gate.complete(Unit)
            h.idle()

            h.run.state.value shouldBe RunState.InSheet(OpenOrder("second", null))
            h.opened shouldBe listOf(OpenOrder("second", null))
            h.gateway.journal.entries.value shouldBe emptyList()
        }

    @Test
    fun `a status that answers after a reset does not touch the next run`() =
        runTest {
            val sent = CompletableDeferred<Unit>()
            val gate = CompletableDeferred<Unit>()
            val h =
                harness(
                    FakeGateway {
                        if (it.url.encodedPath.endsWith(STATUS)) {
                            sent.complete(Unit)
                            gate.await()
                        }
                        answer(it)
                    },
                )

            h.run.launch(register, V1)
            h.inSheet()
            h.run.onResult(CheckoutResult.Completed("abc"))
            sent.await()
            h.run.reset()
            h.run.launch(existing("second"), V1)
            h.inSheet()
            gate.complete(Unit)
            h.idle()

            h.run.state.value shouldBe RunState.InSheet(OpenOrder("second", null))
            h.gateway.journal.entries.value shouldBe emptyList()
        }

    @Test
    fun `an exception from the sheet of a reset run does not finish the next run`() =
        runTest {
            lateinit var h: Harness
            var calls = 0
            h =
                harness(present = {
                    if (calls++ == 0) {
                        h.run.reset()
                        h.run.launch(existing("second"), V1)
                        error("boom")
                    }
                })

            h.run.launch(existing("first"), V1)
            h.idle()

            h.run.state.value shouldBe RunState.InSheet(OpenOrder("second", null))
            h.opened shouldBe listOf(OpenOrder("first", null), OpenOrder("second", null))
        }

    @Test
    fun `a result after a reset is ignored`() =
        runTest {
            val h = harness()

            h.run.launch(existing("given"), V1)
            h.inSheet()
            h.run.reset()
            h.run.onResult(CheckoutResult.Completed("given"))
            h.idle()

            h.run.state.value shouldBe RunState.Idle
            h.gateway.requests.size shouldBe 0
        }

    @Test
    fun `a finished run can be launched again`() =
        runTest {
            val h = harness()

            h.run.launch(register, V1)
            h.inSheet()
            h.run.onResult(CheckoutResult.Canceled("abc"))
            h.finished()
            h.run.launch(existing("second"), V1)

            h.inSheet() shouldBe OpenOrder("second", null)
        }

    @Test
    fun `the result type ids are the contract's`() {
        ResultType.entries.map { it.id } shouldBe
            listOf("completed", "failed", "canceled", "unknown", "registration_failed", "sdk_error")
    }

    private companion object {
        const val STATUS = "/rest/getOrderStatusExtended.do"

        fun MockRequestHandleScope.answer(request: HttpRequestData) =
            when {
                request.url.encodedPath.endsWith(STATUS) ->
                    ok("""{"errorCode":"0","orderStatus":2,"actionCode":0,"actionCodeDescription":"Approved"}""")
                request.url.encodedPath.endsWith("/sessions") -> ok("""{"id":"session-1"}""")
                else -> ok("""{"orderId":"abc"}""")
            }
    }
}
