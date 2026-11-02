package net.payrdr.mobile.payment.sdk.payment.result

import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig

/**
 * User/consumer-triggered action that `PaymentFlowController` processes.
 */
public sealed interface PaymentIntent {
    /** Start the checkout — always the first intent. */
    public data class Start(
        val checkout: CheckoutConfig,
    ) : PaymentIntent

    /**
     * Submit a freshly entered card.
     *
     * @property payerData values of the [PayerDataRequest] fields shown for this card; blank values are not sent
     */
    public data class SubmitNewCard(
        val pan: String,
        val expiryMMYY: String,
        val cvc: String,
        val cardHolder: String?,
        val saveCard: Boolean,
        val payerData: Map<PayerField, String> = emptyMap(),
    ) : PaymentIntent {
        // Card and payer data must not reach logs or crash reports: only the last 4 digits and which payer fields came.
        override fun toString(): String =
            "SubmitNewCard(pan=••••${pan.takeLast(PAN_TAIL)}, saveCard=$saveCard, payerFields=${payerData.keys})"
    }

    /**
     * Submit a previously saved binding with a CVC.
     *
     * @property payerData values of the [PayerDataRequest] fields shown for this card; blank values are not sent
     */
    public data class SubmitSavedCard(
        val bindingId: String,
        val cvc: String?,
        val payerData: Map<PayerField, String> = emptyMap(),
    ) : PaymentIntent {
        // The CVV and payer data must not reach logs or crash reports.
        override fun toString(): String = "SubmitSavedCard(bindingId=$bindingId, payerFields=${payerData.keys})"
    }

    /** Remove a saved binding from the server. */
    public data class UnbindSavedCard(
        val bindingId: String,
    ) : PaymentIntent

    /** User tapped the Google Pay button: the controller opens Google Pay with [PaymentState.CardFormShown.googlePay]. */
    public data object TapGooglePay : PaymentIntent

    /** User tapped the Apple Pay button: the controller opens Apple Pay with [PaymentState.CardFormShown.applePay]. */
    public data object TapApplePay : PaymentIntent

    /** Report the 3DS challenge outcome back to the flow. */
    public data class ThreeDsCompleted(
        val success: Boolean,
        val failureReason: String? = null,
    ) : PaymentIntent

    /**
     * User chose "Pay with another card" on a declined result. Returns the flow to the last
     * [PaymentState.CardFormShown]; ignored unless the current state is a failed [PaymentState.Completed].
     */
    public data object BackToPaymentMethods : PaymentIntent

    /** User cancelled; flow should move to a failed Completed state. */
    public data object Cancel : PaymentIntent
}

/** Digits of a card number a log may show, as receipts do. */
private const val PAN_TAIL = 4
