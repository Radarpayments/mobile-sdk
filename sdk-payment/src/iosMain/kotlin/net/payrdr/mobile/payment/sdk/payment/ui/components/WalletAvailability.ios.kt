package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import platform.PassKit.PKPaymentAuthorizationController

@Composable
internal actual fun rememberWalletAvailability(config: SdkPaymentConfig): WalletAvailability? {
    val applePay = config.applePay != null
    // Same check the Apple Pay provider makes before presenting its sheet.
    return remember(applePay) {
        WalletAvailability(googlePay = false, applePay = applePay && PKPaymentAuthorizationController.canMakePayments())
    }
}
