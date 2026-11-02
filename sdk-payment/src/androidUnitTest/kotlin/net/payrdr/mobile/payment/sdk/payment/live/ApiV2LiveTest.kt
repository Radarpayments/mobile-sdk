package net.payrdr.mobile.payment.sdk.payment.live

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.payrdr.mobile.payment.sdk.core.SdkCore
import net.payrdr.mobile.payment.sdk.payment.api.PaymentApiImpl
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.exception.SdkDeclinedException
import net.payrdr.mobile.payment.sdk.payment.flow.PaymentFlowController
import net.payrdr.mobile.payment.sdk.payment.http.defaultHttpClientFactory
import net.payrdr.mobile.payment.sdk.payment.keyprovider.RemoteKeyProvider
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsChallenge
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsResult
import net.payrdr.mobile.payment.sdk.payment.threeds.WebChallengeHost
import net.payrdr.mobile.payment.sdk.payment.utils.SessionIdConverter
import org.junit.Assume.assumeTrue
import kotlin.test.Test

/**
 * API V2 sessions checked out through the SDK on a real gateway. Skipped unless `PAYRDR_API2_KEY` holds the merchant's
 * API key (`PAYRDR_API2_GATEWAY` overrides `https://dev.bpcbt.com`). The bank page needs a browser, so these stop at it.
 */
class ApiV2LiveTest {
    private val apiKey = System.getenv("PAYRDR_API2_KEY").orEmpty()
    private val gateway = System.getenv("PAYRDR_API2_GATEWAY") ?: "https://dev.bpcbt.com"
    private val http = HttpClient(OkHttp)

    /** `POST /api2/sessions` for 10.00 EUR; the merchant's pages are on its own site, as an API V2 integration has them. */
    private suspend fun createSession(): JsonObject {
        val body =
            http
                .post("$gateway/api2/sessions") {
                    header("X-API-KEY", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"amount":1000,"currency":"EUR","successUrl":"https://shop.example/ok","failureUrl":"https://shop.example/fail"}""",
                    )
                }.bodyAsText()
        return Json.parseToJsonElement(body).jsonObject
    }

    private class BankPage : WebChallengeHost {
        val challenges = mutableListOf<ThreeDsChallenge>()

        override suspend fun performChallenge(challenge: ThreeDsChallenge): ThreeDsResult {
            challenges += challenge
            return ThreeDsResult.Cancelled
        }
    }

    private fun CoroutineScope.controller(bankPage: WebChallengeHost): PaymentFlowController {
        val config = SdkPaymentConfig(baseUrl = "$gateway/payment")
        val client = defaultHttpClientFactory().create(config)
        return PaymentFlowController(
            api = PaymentApiImpl(config.baseUrl, client),
            keyProvider = RemoteKeyProvider(config.effectiveKeyProviderUrl(), client),
            core = SdkCore(),
            webChallengeHost = bankPage,
            logger = Logger(),
            scope = this,
            language = "en",
        )
    }

    @Test fun `a session is paid on the order of its payment url and returns from the bank page by its id`() =
        runBlocking<Unit> {
            assumeTrue(apiKey.isNotEmpty())
            val session = createSession()
            val sessionId = session.getValue("id").jsonPrimitive.content
            val mdOrder = SessionIdConverter.sessionIdToMdOrder(sessionId)
            val paymentUrl = session.getValue("paymentUrl").jsonPrimitive.content
            paymentUrl.substringAfter("mdOrder=").substringBefore('&') shouldBe mdOrder

            val bankPage = BankPage()
            val controller = controller(bankPage)
            controller.handle(PaymentIntent.Start(CheckoutConfig.SessionId(sessionId)))
            withTimeout(TIMEOUT_MS) { controller.state.filterIsInstance<PaymentState.CardFormShown>().first() }
            controller.order.value?.formattedAmount shouldBe "€10.00"
            controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = "5555555555555599",
                    expiryMMYY = "12/34",
                    cvc = "123",
                    cardHolder = "TEST",
                    saveCard = false,
                ),
            )
            withTimeout(TIMEOUT_MS) { controller.state.filterIsInstance<PaymentState.Completed>().first() }

            val challenge = bankPage.challenges.single()
            challenge.sessionId shouldBe sessionId
            // Where the gateway sends the buyer after the bank page (seen with a browser on the dev gateway).
            challenge.isReturnUrl("https://shop.example/ok?sessionId=$sessionId&lang=en") shouldBe true
            challenge.isReturnUrl("https://shop.example/fail?sessionId=$sessionId&lang=en") shouldBe true
        }

    @Test fun `a session the merchant expired fails as declined by the merchant`() =
        runBlocking<Unit> {
            assumeTrue(apiKey.isNotEmpty())
            val sessionId = createSession().getValue("id").jsonPrimitive.content
            http.post("$gateway/api2/sessions/$sessionId/expire") { header("X-API-KEY", apiKey) }

            val controller = controller(BankPage())
            controller.handle(PaymentIntent.Start(CheckoutConfig.SessionId(sessionId)))
            val result = withTimeout(TIMEOUT_MS) { controller.state.filterIsInstance<PaymentState.Completed>().first() }.result

            result.orderId shouldBe SessionIdConverter.sessionIdToMdOrder(sessionId)
            result
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkDeclinedException>()
                .declineCode shouldBe "DECLINED_BY_MERCHANT"
        }

    private companion object {
        const val TIMEOUT_MS = 60_000L
    }
}
