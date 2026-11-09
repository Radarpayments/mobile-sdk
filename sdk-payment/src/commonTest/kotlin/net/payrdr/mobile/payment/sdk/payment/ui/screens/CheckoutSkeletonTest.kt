package net.payrdr.mobile.payment.sdk.payment.ui.screens

import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import kotlin.test.Test

class CheckoutSkeletonTest {
    // Both wallets: the platform's own one is configured on Android and on iOS alike.
    private val config =
        SdkPaymentConfig(
            baseUrl = "https://pay.test",
            googlePay = GooglePayConfig(),
            applePay = ApplePayConfig(merchantId = "merchant.test"),
        )

    @Test fun `a configured wallet is drawn until a session says it is not offered`() {
        skeletonLayout(config, cachedCards = 0, walletOffered = null).wallet shouldBe true
        skeletonLayout(config, cachedCards = 0, walletOffered = true).wallet shouldBe true
    }

    @Test fun `a wallet the last session did not offer is not drawn`() {
        skeletonLayout(config, cachedCards = 0, walletOffered = false).wallet shouldBe false
    }

    @Test fun `a wallet the merchant did not configure is never drawn`() {
        skeletonLayout(SdkPaymentConfig(baseUrl = "https://pay.test"), cachedCards = 0, walletOffered = true).wallet shouldBe false
    }
}
