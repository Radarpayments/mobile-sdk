package net.payrdr.mobile.payment.sdk.payment.live

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.payrdr.mobile.payment.sdk.core.SdkCore
import net.payrdr.mobile.payment.sdk.payment.api.PaymentApiImpl
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.flow.PaymentFlowController
import net.payrdr.mobile.payment.sdk.payment.http.defaultHttpClientFactory
import net.payrdr.mobile.payment.sdk.payment.keyprovider.RemoteKeyProvider
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsChallenge
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsResult
import net.payrdr.mobile.payment.sdk.payment.threeds.WebChallengeHost
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * A one-leg AFT transfer paid through the SDK on a real gateway. Skipped unless `PAYRDR_AFT_BASE_URL` (the payment
 * base URL, `https://<host>/payment`), `PAYRDR_AFT_LOGIN` and `PAYRDR_AFT_PASSWORD` name a merchant allowed transfers
 * and `PAYRDR_AFT_PAN` / `PAYRDR_AFT_EXPIRY` (`MM/YY`) / `PAYRDR_AFT_CVC` its test card. The bank page needs a browser:
 * with `PAYRDR_AFT_3DS_DIR` set, its form is written there as `challenge.html` and the test waits for the final URL
 * of the page in `redirect.txt`; without it the test stops at the bank page.
 */
class AftVerifyLiveTest {
    private val base = System.getenv("PAYRDR_AFT_BASE_URL").orEmpty().trimEnd('/')
    private val login = System.getenv("PAYRDR_AFT_LOGIN").orEmpty()
    private val password = System.getenv("PAYRDR_AFT_PASSWORD").orEmpty()
    private val pan = System.getenv("PAYRDR_AFT_PAN").orEmpty()
    private val expiry = System.getenv("PAYRDR_AFT_EXPIRY").orEmpty()
    private val cvc = System.getenv("PAYRDR_AFT_CVC").orEmpty()
    private val http = HttpClient(OkHttp)
    private val clients = mutableListOf(http)

    private val clientId = "kmp-verify-${System.currentTimeMillis()}"

    private suspend fun registerTransfer(
        amount: Int,
        features: String,
    ): String {
        val body =
            http
                .post("$base/rest/api/p2p/registerP2P.do") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"username":"$login","password":"$password","amount":$amount,"clientId":"$clientId","currency":"978",
                        |"orderNumber":"KMP-VRF-${System.currentTimeMillis()}","returnUrl":"sdk://done",
                        |"transactionTypeIndicator":"A","features":{"feature":[$features]}}
                        """.trimMargin(),
                    )
                }.bodyAsText()
        println("registerP2P: $body")
        return Json
            .parseToJsonElement(body)
            .jsonObject
            .getValue("orderId")
            .jsonPrimitive.content
    }

    /** Hands the bank page to a browser (see the class docs), or stops at it. */
    private class BankPage : WebChallengeHost {
        val challenges = mutableListOf<ThreeDsChallenge>()

        override suspend fun performChallenge(challenge: ThreeDsChallenge): ThreeDsResult {
            challenges += challenge
            val dir = System.getenv("PAYRDR_AFT_3DS_DIR")?.let(::File) ?: return ThreeDsResult.Cancelled
            val redirect = File(dir, "redirect.txt").apply { delete() }
            File(dir, "challenge.txt").writeText("${challenge.acsUrl}\n${challenge.md}\n${challenge.paReq}\n${challenge.termUrl}\n")
            repeat(TIMEOUT_S) {
                if (redirect.exists()) {
                    val url = redirect.readText().trim()
                    println("bank page ended at $url")
                    return if (challenge.isReturnUrl(url)) ThreeDsResult.Succeeded(emptyMap()) else ThreeDsResult.Interrupted(url)
                }
                kotlinx.coroutines.delay(1_000)
            }
            return ThreeDsResult.TimedOut
        }
    }

    private fun CoroutineScope.controller(bankPage: WebChallengeHost): PaymentFlowController {
        val config = SdkPaymentConfig(baseUrl = base)
        val client = defaultHttpClientFactory().create(config).also { clients += it }
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

    @Test fun `a zero VERIFY transfer saves the card`() =
        runBlocking<Unit> {
            assumeTrue(listOf(base, login, password, pan, expiry, cvc).all { it.isNotEmpty() })
            val mdOrder = registerTransfer(0, "\"WITHOUT_TO_CARD\",\"VERIFY\",\"FORCE_CREATE_BINDING\"")
            val bankPage = BankPage()
            val controller = controller(bankPage)
            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(mdOrder)))
            val form = withTimeout(TIMEOUT_S * 1_000L) { controller.state.filterIsInstance<PaymentState.CardFormShown>().first() }
            println("form: transfer=${form.transfer} saved=${form.savedCards} order=${controller.order.value}")
            println("payer fields VISA: ${form.payerData.fieldsFor("VISA").map { it.field }}")

            controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = pan,
                    expiryMMYY = expiry,
                    cvc = cvc,
                    cardHolder = "TEST TEST",
                    saveCard = true,
                    payerData =
                        mapOf(
                            PayerField.EMAIL to "test@test.com",
                            PayerField.MOBILE_PHONE to "+35799123456",
                            PayerField.PAYER_FIRST_NAME to "Test",
                            PayerField.PAYER_LAST_NAME to "User",
                            PayerField.PAYER_BIRTHDAY to "19901212",
                            PayerField.PAYER_ACCOUNT to "account",
                            PayerField.BILLING_COUNTRY to "CY",
                            PayerField.BILLING_CITY to "Nicosia",
                            PayerField.BILLING_ADDRESS_LINE1 to "123 Test Street",
                            PayerField.RECIPIENT_FIRST_NAME to "Test",
                            PayerField.RECIPIENT_LAST_NAME to "Recipient",
                            PayerField.RECIPIENT_ACCOUNT to "test",
                            PayerField.RECIPIENT_ACCOUNT_NUMBER_TYPE to "1",
                            PayerField.RECIPIENT_COUNTRY to "CY",
                        ),
                ),
            )
            val done = withTimeout(TIMEOUT_S * 1_000L) { controller.state.filterIsInstance<PaymentState.Completed>().first() }
            println("verify $mdOrder ended: ${done.result} ${done.receipt}")
            println("challenges: ${bankPage.challenges.size}")

            val next = registerTransfer(100, "\"WITHOUT_TO_CARD\"")
            val second = controller(BankPage())
            second.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(next)))
            val nextForm = withTimeout(TIMEOUT_S * 1_000L) { second.state.filterIsInstance<PaymentState.CardFormShown>().first() }
            println(
                "saved cards for $clientId on the next transfer: ${nextForm.savedCards.map {
                    it.maskedPan + " " + it.paymentSystem + " " + it.expiry
                }}",
            )
            second.handle(PaymentIntent.Cancel)
        }

    @AfterTest fun closeClients() = clients.forEach { it.close() }

    private companion object {
        const val TIMEOUT_S = 300
    }
}
