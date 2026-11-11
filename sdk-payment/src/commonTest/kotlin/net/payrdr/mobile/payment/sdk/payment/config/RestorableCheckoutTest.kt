package net.payrdr.mobile.payment.sdk.payment.config

import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayEnvironment
import net.payrdr.mobile.payment.sdk.payment.logger.NoopLogTransport
import net.payrdr.mobile.payment.sdk.payment.ssl.SslPinningConfig
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentColor
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import kotlin.test.Test

class RestorableCheckoutTest {
    private fun roundTrip(
        config: SdkPaymentConfig,
        checkout: CheckoutConfig = CheckoutConfig.MdOrder("md-1"),
    ) = RestorableCheckout.decode(RestorableCheckout.encode(config, checkout))

    @Test fun `the gateway and language and result screen and built-in theme and order survive`() {
        val config =
            SdkPaymentConfig(
                baseUrl = "https://pay.example.com",
                keyProviderUrl = "https://keys.example.com",
                locale = "de",
                theme = PaymentTheme.Dark,
                showResultScreen = true,
            )

        roundTrip(config) shouldBe (config to CheckoutConfig.MdOrder("md-1"))
    }

    @Test fun `a session id survives as a session id`() {
        roundTrip(SdkPaymentConfig(baseUrl = "https://pay.example.com"), CheckoutConfig.SessionId("ps_1")).second shouldBe
            CheckoutConfig.SessionId("ps_1")
    }

    @Test fun `a custom theme keeps its colors`() {
        val colors =
            PaymentColors(
                primary = PaymentColor(0xFF112233),
                onPrimary = PaymentColor.White,
                background = PaymentColor(0xFFFAFAFA),
                onBackground = PaymentColor.Black,
                surface = PaymentColor.White,
                onSurface = PaymentColor.Black,
                error = PaymentColor(0xFFFF0000),
                onError = PaymentColor.White,
                outline = PaymentColor(0xFF888888),
                success = PaymentColor(0xFF00FF00),
                scrim = PaymentColor(0x80000000),
            )
        val restored = roundTrip(SdkPaymentConfig(baseUrl = "https://pay.example.com", theme = PaymentTheme.Custom(colors))).first

        restored.theme shouldBe PaymentTheme.Custom(colors)
    }

    @Test fun `wallets and SSL pinning survive`() {
        val config =
            SdkPaymentConfig(
                baseUrl = "https://pay.example.com",
                googlePay =
                    GooglePayConfig(
                        merchantId = "m",
                        merchantFullName = "Shop",
                        gateway = "g",
                        gatewayMerchantId = "gm",
                        environment = GooglePayEnvironment.PRODUCTION,
                        countryCode = "DE",
                    ),
                applePay = ApplePayConfig(merchantId = "merchant.x", countryCode = "DE"),
                sslPinning = SslPinningConfig.Builder().addPin("pay.example.com", PIN).build(),
            )
        val restored = roundTrip(config).first

        restored.googlePay shouldBe config.googlePay
        restored.applePay shouldBe config.applePay
        restored.sslPinning?.entries shouldBe config.sslPinning?.entries
    }

    @Test fun `google pay left to the gateway stays left to the gateway`() {
        val config = SdkPaymentConfig(baseUrl = "https://pay.example.com", googlePay = GooglePayConfig())

        roundTrip(config).first.googlePay shouldBe GooglePayConfig()
    }

    @Test fun `what cannot be written down comes back as the default`() {
        val restored = roundTrip(SdkPaymentConfig(baseUrl = "https://pay.example.com", logTransport = NoopLogTransport)).first

        restored.logTransport shouldBe null
        restored.cardScannerProvider shouldBe null
    }

    private companion object {
        const val PIN = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    }
}
