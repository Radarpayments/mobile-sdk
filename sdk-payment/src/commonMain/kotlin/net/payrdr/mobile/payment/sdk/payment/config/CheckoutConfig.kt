package net.payrdr.mobile.payment.sdk.payment.config

/**
 * Identifies which payment session/order the consumer wants to check out.
 */
public sealed interface CheckoutConfig {
    /** Legacy order-number style (API V1). */
    public data class MdOrder(
        val value: String,
    ) : CheckoutConfig

    /** Modern session-id style (API V2) — a `ps_` prefixed Base58-encoded `ps:{mdOrder}` blob. */
    public data class SessionId(
        val value: String,
    ) : CheckoutConfig
}
