package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

/**
 * One entry of `payerDataParamsNeedToBeFilled`: `{"VISA": [{"name": "EMAIL", "mandatory": true, "editable": true}]}`.
 */
@Serializable
internal data class PayerParam(
    val name: String,
    val mandatory: Boolean = false,
    val editable: Boolean = true,
)
