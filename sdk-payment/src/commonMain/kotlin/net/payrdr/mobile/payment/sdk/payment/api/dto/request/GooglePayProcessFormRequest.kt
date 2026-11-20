package net.payrdr.mobile.payment.sdk.payment.api.dto.request

import kotlinx.serialization.Serializable

/**
 * Parameters for `google/paymentOrder.do`. Sent as JSON; [additionalParameters] is left out when Google Pay gave no
 * payer data.
 */
@Serializable
internal data class GooglePayProcessFormRequest(
    val paymentToken: String,
    val mdOrder: String,
    val additionalParameters: GooglePayAdditionalParameters? = null,
) {
    // The token pays for the order: it must not reach logs or crash reports.
    override fun toString(): String =
        "GooglePayProcessFormRequest(paymentToken=••••, mdOrder=$mdOrder, additionalParameters=$additionalParameters)"
}

/** The buyer's contacts from Google Pay, as the gateway's own payment page sends them; each left out when unknown. */
@Serializable
internal data class GooglePayAdditionalParameters(
    val email: String? = null,
    val phone: String? = null,
) {
    // Payer data must not reach logs or crash reports: only whether each contact goes.
    override fun toString(): String = "GooglePayAdditionalParameters(email=${email != null}, phone=${phone != null})"
}
