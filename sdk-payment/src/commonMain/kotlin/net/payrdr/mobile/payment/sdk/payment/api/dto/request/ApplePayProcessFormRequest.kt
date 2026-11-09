package net.payrdr.mobile.payment.sdk.payment.api.dto.request

import kotlinx.serialization.Serializable

/**
 * Parameters for `applepay/paymentOrder.do`. Sent as JSON.
 */
@Serializable
internal data class ApplePayProcessFormRequest(
    val paymentToken: String,
    val mdOrder: String,
)
