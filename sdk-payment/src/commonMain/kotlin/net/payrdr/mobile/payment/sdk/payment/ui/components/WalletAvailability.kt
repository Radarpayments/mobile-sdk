package net.payrdr.mobile.payment.sdk.payment.ui.components

import net.payrdr.mobile.payment.sdk.payment.presentation.PayrdrUiApi
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState

/** Wallet buttons to show: configured by the merchant and able to pay on this device. */
@PayrdrUiApi
public data class WalletAvailability(
    public val googlePay: Boolean,
    public val applePay: Boolean,
) {
    public val any: Boolean get() = googlePay || applePay

    /** The wallets of these the gateway offers for the order of [form]. */
    public fun offeredIn(form: PaymentState.CardFormShown): WalletAvailability =
        WalletAvailability(googlePay = googlePay && form.googlePay != null, applePay = applePay && form.applePay != null)

    /** Holds [None]. */
    @PayrdrUiApi
    public companion object {
        public val None: WalletAvailability = WalletAvailability(googlePay = false, applePay = false)
    }
}
