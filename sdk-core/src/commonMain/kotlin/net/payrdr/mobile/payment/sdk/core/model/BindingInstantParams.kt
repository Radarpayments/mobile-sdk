package net.payrdr.mobile.payment.sdk.core.model

/**
 * Saved card instant-pay variant (no mdOrder).
 */
public data class BindingInstantParams(
    val bindingID: String,
    override val cvc: String?,
    override val pubKey: String,
) : PaymentCardParams.StoredCardPaymentParams(cvc = cvc, pubKey = pubKey)
