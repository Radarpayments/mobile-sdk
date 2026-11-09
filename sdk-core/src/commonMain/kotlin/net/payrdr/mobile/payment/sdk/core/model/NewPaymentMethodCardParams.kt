package net.payrdr.mobile.payment.sdk.core.model

/**
 * New card tokenization targeting the payment-method creation flow.
 */
public data class NewPaymentMethodCardParams(
    override val pan: String,
    override val cvc: String,
    override val expiryMMYY: String,
    override val cardHolder: String?,
    override val pubKey: String,
) : PaymentCardParams.NewCardPaymentParams(pan, cvc, expiryMMYY, cardHolder, pubKey)
