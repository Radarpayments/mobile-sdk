package net.payrdr.mobile.payment.sdk.core.model

/**
 * New card payment with explicit `mdOrder` (classic Radar flow). Without an order, use [CardInstantParams].
 */
public data class CardParams(
    val mdOrder: String,
    override val pan: String,
    override val cvc: String,
    override val expiryMMYY: String,
    override val cardHolder: String?,
    override val pubKey: String,
) : PaymentCardParams.NewCardPaymentParams(pan, cvc, expiryMMYY, cardHolder, pubKey)
