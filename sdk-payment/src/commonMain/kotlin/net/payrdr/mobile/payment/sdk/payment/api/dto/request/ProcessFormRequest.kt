package net.payrdr.mobile.payment.sdk.payment.api.dto.request

import net.payrdr.mobile.payment.sdk.payment.result.PayerField

/**
 * Parameters for `processform.do` or `processBindingForm.do`. Sent form-urlencoded.
 *
 * @property paymentToken RSA-sealed cryptogram produced by `SdkCore.generate(...)`
 * @property mdOrder order number
 * @property holder cardholder name (defaults to `"CARDHOLDER"` if blank)
 * @property saveCard if true, the merchant should save the card as a binding
 * @property payerData payer fields collected for this card (email, phone, billing address)
 */
internal data class ProcessFormRequest(
    val paymentToken: String,
    val mdOrder: String,
    val holder: String,
    val saveCard: Boolean,
    val payerData: Map<PayerField, String> = emptyMap(),
)
