package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

@Serializable
internal data class BindingItem(
    val id: String,
    val label: String,
    val paymentSystem: String,
    val cardHolder: String = "",
    val createdDate: Long = 0L,
    val payerEmail: String = "",
    val payerPhone: String = "",
    val bindingBillingData: BillingPayerData? = null,
    val isMaestro: Boolean = false,
    /** What a transfer may do with the card: `D` debit it (the sender's card), `CR` credit it; `C`, `R`, `I` e-commerce. */
    val bindingCategory: String? = null,
)
