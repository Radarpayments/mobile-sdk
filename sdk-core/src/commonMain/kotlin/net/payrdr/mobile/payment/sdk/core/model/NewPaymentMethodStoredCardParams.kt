package net.payrdr.mobile.payment.sdk.core.model

/**
 * Saved card tokenization targeting payment-method creation (storedPaymentId format `pm_{base58}`).
 */
public data class NewPaymentMethodStoredCardParams(
    val storedPaymentId: String,
    override val cvc: String?,
    override val pubKey: String,
) : PaymentCardParams.StoredCardPaymentParams(cvc = cvc, pubKey = pubKey)
