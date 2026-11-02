package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState

/** Wallet buttons to show: configured by the merchant and able to pay on this device. */
internal data class WalletAvailability(
    val googlePay: Boolean,
    val applePay: Boolean,
) {
    val any: Boolean get() = googlePay || applePay

    /** The wallets of these the gateway offers for the order of [form]. */
    fun offeredIn(form: PaymentState.CardFormShown): WalletAvailability =
        WalletAvailability(googlePay = googlePay && form.googlePay != null, applePay = applePay && form.applePay != null)

    companion object {
        val None = WalletAvailability(googlePay = false, applePay = false)
    }
}

/**
 * Which wallets of [config] the device can pay with, or null while the platform check runs.
 * A button for an unavailable wallet only ends the checkout with an error, so it is not shown.
 */
@Composable
internal expect fun rememberWalletAvailability(config: SdkPaymentConfig): WalletAvailability?
