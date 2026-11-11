package net.payrdr.mobile.payment.sdk.bridge

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayMerchantCapability
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayNetwork
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayAuthMethod
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayCardNetwork
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayEnvironment
import net.payrdr.mobile.payment.sdk.payment.logger.ConsoleLogTransport
import net.payrdr.mobile.payment.sdk.payment.ui.theme.DefaultDarkColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.DefaultLightColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentColor
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentCorners
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import kotlin.test.Test

class CheckoutRequestTest {
    private var scannerAsked = false

    private fun request(
        config: String,
        order: String = """"mdOrder":"md-1"""",
    ) = CheckoutRequest.parse("""{"config":$config,$order}""") {
        scannerAsked = true
        null
    }

    @Test fun `only the gateway is needed and everything else keeps the SDK defaults`() {
        val parsed = request("""{"baseUrl":"https://pay.example.com"}""")

        parsed.config shouldBe SdkPaymentConfig(baseUrl = "https://pay.example.com")
        parsed.checkout shouldBe CheckoutConfig.MdOrder("md-1")
        parsed.orderId shouldBe "md-1"
    }

    @Test fun `a session id opens the session and names no order`() {
        val parsed = request("""{"baseUrl":"https://pay.example.com"}""", order = """"sessionId":"ps_1"""")

        parsed.checkout shouldBe CheckoutConfig.SessionId("ps_1")
        parsed.orderId shouldBe ""
    }

    @Test fun `a request without an order says what is missing`() {
        val error =
            shouldThrow<InvalidBridgeRequest> { CheckoutRequest.parse("""{"config":{"baseUrl":"https://pay.example.com"}}""") { null } }

        error.message shouldContain "mdOrder"
    }

    @Test fun `text that is not the contract is an invalid request and no crash`() {
        shouldThrow<InvalidBridgeRequest> { CheckoutRequest.parse("not json") { null } }
        shouldThrow<InvalidBridgeRequest> { CheckoutRequest.parse("""{"mdOrder":"md-1"}""") { null } }
    }

    @Test fun `fields the native side does not know yet are ignored`() {
        request("""{"baseUrl":"https://pay.example.com","futureOption":true}""").config.baseUrl shouldBe "https://pay.example.com"
    }

    @Test fun `the plain options reach the SDK config`() {
        val config =
            request(
                """{"baseUrl":"https://pay.example.com","keyProviderUrl":"https://keys.example.com","locale":"de",
                   "showResultScreen":true,"cardScanner":true,"debugLogging":true,
                   "sslPins":[{"host":"pay.example.com","sha256":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="}]}""",
            ).config

        config.keyProviderUrl shouldBe "https://keys.example.com"
        config.locale shouldBe "de"
        config.showResultScreen shouldBe true
        scannerAsked shouldBe true
        config.logTransport shouldBe ConsoleLogTransport
        config.sslPinning!!
            .entries
            .single()
            .hostname shouldBe "pay.example.com"
    }

    @Test fun `stock themes are named by mode`() {
        request("""{"baseUrl":"u","theme":{"mode":"light"}}""").config.theme shouldBe PaymentTheme.Light
        request("""{"baseUrl":"u","theme":{"mode":"dark"}}""").config.theme shouldBe PaymentTheme.Dark
        request("""{"baseUrl":"u","theme":{"mode":"system"}}""").config.theme shouldBe PaymentTheme.System
    }

    @Test fun `a custom theme changes only the colours and corners it names`() {
        val theme =
            request(
                """{"baseUrl":"u","theme":{"mode":"custom","base":"dark",
                   "colors":{"primary":"#F3D9B1","scrim":"#80000000"},"cornerRadius":18,"buttonCornerRadius":999}}""",
            ).config.theme

        theme shouldBe
            PaymentTheme.Custom(
                colors = DefaultDarkColors.copy(primary = PaymentColor(0xFFF3D9B1), scrim = PaymentColor(0x80000000)),
                shapes =
                    PaymentShapes.Default.copy(
                        field = PaymentCorners.Radius(18f),
                        button = PaymentCorners.Radius(999f),
                        card = PaymentCorners.Radius(18f),
                    ),
            )
    }

    @Test fun `a colour that is not hex names the colour`() {
        val error =
            shouldThrow<InvalidBridgeRequest> {
                request("""{"baseUrl":"u","theme":{"mode":"custom","colors":{"onPrimary":"white"}}}""")
            }

        error.message shouldContain "onPrimary"
    }

    @Test fun `google pay options map one to one and absent ones come from the gateway`() {
        val full =
            request(
                """{"baseUrl":"u","googlePay":{"merchantId":"BCR2","merchantName":"Hearth & Co","gateway":"bpcpay",
                   "gatewayMerchantId":"shop","environment":"PRODUCTION","countryCode":"DE",
                   "cardNetworks":["VISA"],"authMethods":["CRYPTOGRAM_3DS"],"billingAddressRequired":true}}""",
            ).config.googlePay

        full shouldBe
            GooglePayConfig(
                merchantId = "BCR2",
                merchantFullName = "Hearth & Co",
                gateway = "bpcpay",
                gatewayMerchantId = "shop",
                environment = GooglePayEnvironment.PRODUCTION,
                allowedCardNetworks = setOf(GooglePayCardNetwork.VISA),
                allowedAuthMethods = setOf(GooglePayAuthMethod.CRYPTOGRAM_3DS),
                countryCode = "DE",
                billingAddressRequired = true,
            )
        request("""{"baseUrl":"u","googlePay":{}}""").config.googlePay shouldBe GooglePayConfig()
    }

    @Test fun `apple pay needs its merchant id and maps the rest`() {
        request(
            """{"baseUrl":"u","applePay":{"merchantId":"merchant.shop","countryCode":"DE",
               "networks":["AMEX"],"capabilities":["THREE_D_SECURE"]}}""",
        ).config.applePay shouldBe
            ApplePayConfig(
                merchantId = "merchant.shop",
                supportedNetworks = setOf(ApplePayNetwork.AMEX),
                merchantCapabilities = setOf(ApplePayMerchantCapability.THREE_D_SECURE),
                countryCode = "DE",
            )
        shouldThrow<InvalidBridgeRequest> { request("""{"baseUrl":"u","applePay":{}}""") }
    }

    @Test fun `an unknown card network is an invalid request`() {
        val error = shouldThrow<InvalidBridgeRequest> { request("""{"baseUrl":"u","googlePay":{"cardNetworks":["NOT_A_NETWORK"]}}""") }

        error.message shouldContain "NOT_A_NETWORK"
    }

    @Test fun `corners set once round the buttons too unless they have their own`() {
        val shapes =
            request("""{"baseUrl":"u","theme":{"mode":"custom","cornerRadius":12}}""")
                .config.theme
                .let { (it as PaymentTheme.Custom).shapes }

        shapes.button shouldBe PaymentCorners.Radius(12f)
        request("""{"baseUrl":"u","theme":{"mode":"custom"}}""").config.theme shouldBe
            PaymentTheme.Custom(colors = DefaultLightColors, shapes = PaymentShapes.Default)
    }

    @Test fun `a theme option outside the contract names the option and the value`() {
        listOf(
            """{"mode":"sepia"}""" to "sepia",
            """{"mode":"custom","base":"sepia"}""" to "theme.base",
            """{"mode":"custom","colors":{"accent":"#FFFFFF"}}""" to "accent",
            """{"mode":"custom","colors":{"primary":"#FFF"}}""" to "#FFF",
        ).forEach { (theme, named) ->
            shouldThrow<InvalidBridgeRequest> { request("""{"baseUrl":"u","theme":$theme}""") }.message shouldContain named
        }
    }

    @Test fun `a malformed SSL pin is an invalid request`() {
        val error =
            shouldThrow<InvalidBridgeRequest> {
                request("""{"baseUrl":"u","sslPins":[{"host":"pay.example.com","sha256":"not-a-pin"}]}""")
            }

        error.message shouldContain "sslPins"
    }

    @Test fun `an unknown wallet option names the field it came in`() {
        shouldThrow<InvalidBridgeRequest> { request("""{"baseUrl":"u","googlePay":{"environment":"STAGING"}}""") }
            .message shouldContain "googlePay.environment"
        shouldThrow<InvalidBridgeRequest> { request("""{"baseUrl":"u","applePay":{"merchantId":"m","networks":["NOPE"]}}""") }
            .message shouldContain "applePay.networks"
    }

    @Test fun `a malformed request says where it broke and never quotes it`() {
        val error = shouldThrow<InvalidBridgeRequest> { request("""{"baseUrl":"u","cardScanner":"secret-value"}""") }

        error.message shouldContain "$.config.cardScanner"
        error.message!! shouldNotContain "secret-value"
    }
}
