package net.payrdr.mobile.payment.sdk.payment.gpay

/** What Google Pay tells about the buyer along with the token. */
internal data class GooglePayPayer(
    val email: String? = null,
    val phone: String? = null,
) {
    // Payer data must not reach logs or crash reports: only whether each contact came.
    override fun toString(): String = "GooglePayPayer(email=${email != null}, phone=${phone != null})"
}
