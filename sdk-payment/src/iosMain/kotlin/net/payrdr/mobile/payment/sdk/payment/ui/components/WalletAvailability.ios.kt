package net.payrdr.mobile.payment.sdk.payment.ui.components

import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import platform.PassKit.PKPaymentAuthorizationController

/** Which wallets of [config] this device can pay with; on iOS the check answers at once. */
internal fun deviceWalletAvailability(config: SdkPaymentConfig): WalletAvailability =
    // Same check the Apple Pay provider makes before presenting its sheet.
    WalletAvailability(googlePay = false, applePay = config.applePay != null && PKPaymentAuthorizationController.canMakePayments())
