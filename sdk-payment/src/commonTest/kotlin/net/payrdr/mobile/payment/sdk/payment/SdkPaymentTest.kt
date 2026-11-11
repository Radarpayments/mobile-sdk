package net.payrdr.mobile.payment.sdk.payment

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.payrdr.mobile.payment.sdk.payment.api.PaymentApiImpl
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNetworkException
import net.payrdr.mobile.payment.sdk.payment.flow.PaymentFlowController
import net.payrdr.mobile.payment.sdk.payment.http.HttpClientFactory
import net.payrdr.mobile.payment.sdk.payment.http.defaultHttpClientFactory
import net.payrdr.mobile.payment.sdk.payment.keyprovider.RemoteKeyProvider
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.ssl.SslPinningConfig
import net.payrdr.mobile.payment.sdk.payment.support.FakeKeyProvider
import net.payrdr.mobile.payment.sdk.payment.support.FakePaymentApi
import net.payrdr.mobile.payment.sdk.payment.support.FakeWebChallengeHost
import net.payrdr.mobile.payment.sdk.payment.support.Fixtures
import net.payrdr.mobile.payment.sdk.payment.support.json
import net.payrdr.mobile.payment.sdk.payment.support.mockJsonClient
import net.payrdr.mobile.payment.sdk.payment.ui.util.deviceLanguage
import net.payrdr.mobile.payment.sdk.payment.utils.MoneyFormatter
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SdkPaymentTest {
    @Test
    fun `create returns SdkPayment with the provided config`() {
        val sdk = SdkPayment.create(SdkPaymentConfig(baseUrl = "https://pay.example.com"))
        sdk.version() shouldBe SDK_VERSION
    }

    @Test
    fun `the version is a semantic version`() {
        SDK_VERSION shouldMatch Regex("""\d+\.\d+\.\d+(-[0-9A-Za-z.]+)?""")
    }

    @Test
    fun `newFlow wires together a PaymentFlowController that runs happy path`() =
        runTest {
            val controller = buildFlow()

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = Fixtures.TEST_CARDHOLDER,
                    saveCard = false,
                ),
            )
            advanceUntilIdle()

            val terminal = controller.state.value
            terminal.shouldBeInstanceOf<PaymentState.Completed>()
            terminal.result shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test
    fun `a network passed at creation is what the flows use`() =
        runTest {
            val api = FakePaymentApi()
            val sdk = SdkPayment.create(CONFIG, SdkPaymentNetwork(api, FakeKeyProvider()))

            sdk.paymentFlow(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER), FakeWebChallengeHost(), this)
            advanceUntilIdle()

            api.getSessionStatusCalls shouldBe listOf(Fixtures.TEST_MD_ORDER)
        }

    @Test
    fun `completePayment returns the result the checkout ends with`() =
        runTest {
            val api = FakePaymentApi().apply { sessionStatusError = SdkNetworkException("offline") }
            val sdk = SdkPayment.create(CONFIG, SdkPaymentNetwork(api, FakeKeyProvider()))

            val result = sdk.completePayment(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER), FakeWebChallengeHost(), this)

            result.shouldBeInstanceOf<CheckoutResult.Failed>().error.shouldBeInstanceOf<SdkNetworkException>()
            result.orderId shouldBe Fixtures.TEST_MD_ORDER
        }

    @Test
    fun `the amount is written in the checkout's language or the device's when it names none`() =
        runTest {
            fun amountIn(locale: String?): String? {
                val controller =
                    SdkPayment
                        .create(CONFIG.copy(locale = locale), SdkPaymentNetwork(FakePaymentApi(), FakeKeyProvider()))
                        .newFlow(FakeWebChallengeHost(), this)
                controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
                advanceUntilIdle()
                return controller.order.value?.formattedAmount
            }

            amountIn("de") shouldBe "49,90\u00A0€"
            amountIn(" ") shouldBe MoneyFormatter.format("4990", "EUR", deviceLanguage())
        }

    @Test
    fun `without a network the SDK builds its own from the config once`() {
        val sdk = SdkPayment.create(CONFIG)

        sdk.api.shouldBeInstanceOf<PaymentApiImpl>()
        sdk.keyProvider.shouldBeInstanceOf<RemoteKeyProvider>()
        (sdk.api === sdk.api) shouldBe true
        (sdk.keyProvider === sdk.keyProvider) shouldBe true
    }

    @Test
    fun `the SDK builds its network from its own config so SSL pinning applies`() {
        val pinned = CONFIG.copy(sslPinning = pinning())
        val factory = RecordingFactory()
        val sdk = SdkPayment(pinned, network = null, httpClientFactory = factory)

        sdk.api
        sdk.keyProvider

        factory.seen shouldBe listOf(pinned)
    }

    @Test
    fun `nothing is built until a checkout needs it and a replaced network builds nothing`() =
        runTest {
            val factory = RecordingFactory()
            SdkPayment(CONFIG, network = null, httpClientFactory = factory)
            factory.seen shouldBe emptyList()

            SdkPayment(CONFIG, SdkPaymentNetwork(FakePaymentApi(), FakeKeyProvider()), httpClientFactory = factory)
                .newFlow(FakeWebChallengeHost(), this)
            factory.seen shouldBe emptyList()
        }

    @Test
    fun `SDKs with the same SSL pinning share one HTTP client however many there are`() {
        val factory = RecordingFactory()
        repeat(3) { i ->
            val sdk = SdkPayment(CONFIG.copy(baseUrl = "https://shop$i.example.com"), network = null, httpClientFactory = factory)
            sdk.api
            sdk.keyProvider
        }

        factory.seen.size shouldBe 1
    }

    @Test
    fun `a different SSL pinning gets its own HTTP client`() {
        val factory = RecordingFactory()
        SdkPayment(CONFIG, network = null, httpClientFactory = factory).api
        SdkPayment(CONFIG.copy(sslPinning = pinning()), network = null, httpClientFactory = factory).api

        factory.seen.size shouldBe 2
    }

    @Test
    fun `a shared client still sends each SDK's calls to its own gateway`() =
        runTest {
            val hosts = mutableListOf<String>()
            val (client, _) =
                mockJsonClient { request ->
                    hosts += request.url.host
                    json("{}")
                }
            val factory = RecordingFactory(client)
            SdkPayment(CONFIG.copy(baseUrl = "https://a.example.com"), network = null, httpClientFactory = factory)
                .api
                .getSessionStatus("md")
            SdkPayment(CONFIG.copy(baseUrl = "https://b.example.com"), network = null, httpClientFactory = factory)
                .api
                .getSessionStatus("md")

            hosts shouldBe listOf("a.example.com", "b.example.com")
        }

    @Test
    fun `the SDK's own network comes from the platform factory that applies SSL pinning`() {
        SdkPayment.create(CONFIG).httpClientFactory shouldBe defaultHttpClientFactory()
    }

    /** Builds clients ([client], or a plain mock) and records the configs it was asked for. */
    private class RecordingFactory(
        private val client: HttpClient? = null,
    ) : HttpClientFactory {
        val seen = mutableListOf<SdkPaymentConfig>()

        override fun create(
            config: SdkPaymentConfig,
            engineOverride: HttpClientEngine?,
        ): HttpClient {
            seen += config
            return client ?: HttpClient(MockEngine { respondOk() })
        }
    }

    private fun pinning() = SslPinningConfig.Builder().addPin("pay.example.com", PIN).build()

    private fun TestScope.buildFlow(): PaymentFlowController =
        SdkPayment
            .create(CONFIG, SdkPaymentNetwork(FakePaymentApi(), FakeKeyProvider()))
            .newFlow(webChallengeHost = FakeWebChallengeHost(), scope = this)

    private companion object {
        val CONFIG = SdkPaymentConfig(baseUrl = "https://pay.example.com")
        const val PIN = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    }
}
