package net.payrdr.mobile.payment.sdk.core.model

/**
 * New card payment without mdOrder (instant-pay / direct auth).
 */
public data class CardInstantParams(
    override val pan: String,
    override val cvc: String,
    override val expiryMMYY: String,
    override val cardHolder: String?,
    override val pubKey: String,
) : PaymentCardParams.NewCardPaymentParams(pan, cvc, expiryMMYY, cardHolder, pubKey)
