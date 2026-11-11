package net.payrdr.mobile.payment.sdk.core.model

/**
 * Saved (bound) card payment with explicit mdOrder. Without an order, use [BindingInstantParams].
 */
public data class BindingParams(
    val mdOrder: String,
    val bindingID: String,
    override val cvc: String?,
    override val pubKey: String,
) : PaymentCardParams.StoredCardPaymentParams(cvc = cvc, pubKey = pubKey)
