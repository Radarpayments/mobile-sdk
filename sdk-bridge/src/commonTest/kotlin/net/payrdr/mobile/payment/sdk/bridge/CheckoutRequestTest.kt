package net.payrdr.mobile.payment.sdk.bridge

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
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
import net.payrdr.mobile.payment.sdk.payment.scanner.provider.MlKitVisionCardScannerProvider
import net.payrdr.mobile.payment.sdk.payment.ui.theme.DefaultDarkColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import kotlin.test.Test

class CheckoutRequestTest {
    private fun request(
        config: String,
        order: String = """"mdOrder":"md-1"""",
    ) = CheckoutRequest.parse("""{"config":$config,$order}""")

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
        val error = shouldThrow<InvalidBridgeRequest> { CheckoutRequest.parse("""{"config":{"baseUrl":"https://pay.example.com"}}""") }

        error.message shouldContain "mdOrder"
    }

    @Test fun `text that is not the contract is an invalid request and no crash`() {
        shouldThrow<InvalidBridgeRequest> { CheckoutRequest.parse("not json") }
        shouldThrow<InvalidBridgeRequest> { CheckoutRequest.parse("""{"mdOrder":"md-1"}""") }
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
        config.cardScannerProvider.shouldBeInstanceOf<MlKitVisionCardScannerProvider>()
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
                colors = DefaultDarkColors.copy(primary = Color(0xFFF3D9B1), scrim = Color(0x80000000)),
                shapes =
                    PaymentShapes.Default.copy(
                        field = RoundedCornerShape(18.dp),
                        button = RoundedCornerShape(999.dp),
                        card = RoundedCornerShape(18.dp),
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
}
