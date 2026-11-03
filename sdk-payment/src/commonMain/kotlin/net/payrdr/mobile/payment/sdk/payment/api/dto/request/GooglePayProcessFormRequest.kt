package net.payrdr.mobile.payment.sdk.payment.api.dto.request

import kotlinx.serialization.Serializable

/**
 * Parameters for `google/paymentOrder.do`. Sent as JSON.
 */
@Serializable
internal data class GooglePayProcessFormRequest(
    val paymentToken: String,
    val mdOrder: String,
)
