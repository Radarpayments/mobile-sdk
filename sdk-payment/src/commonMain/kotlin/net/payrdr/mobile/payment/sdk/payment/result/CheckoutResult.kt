package net.payrdr.mobile.payment.sdk.payment.result

import net.payrdr.mobile.payment.sdk.payment.exception.SdkException

/**
 * Terminal outcome of a checkout, delivered to the merchant exactly once.
 *
 * Every variant carries [orderId] (the gateway's `mdOrder`), so the merchant can always check the order on its
 * server — the server, not this result, is the source of truth before goods are released.
 */
public sealed interface CheckoutResult {
    /** The order the checkout was paying. */
    public val orderId: String

    /** The gateway confirmed the payment (the order is approved or deposited), including an order paid earlier. */
    public data class Completed(
        override val orderId: String,
    ) : CheckoutResult

    /** The payment definitively did not happen: declined, expired, out of attempts, or failed before it was sent. */
    public data class Failed(
        override val orderId: String,
        val error: SdkException,
    ) : CheckoutResult

    /** The buyer closed the checkout. */
    public data class Canceled(
        override val orderId: String,
    ) : CheckoutResult

    /**
     * The outcome is not known: the payment was sent, but the checkout could not learn how it ended (no network,
     * or the order is still in progress at the gateway). The merchant learns it from the gateway's callback or by
     * checking the order status on its server. [cause] is the last error seen while checking, if any.
     */
    public data class Unknown(
        override val orderId: String,
        val cause: SdkException? = null,
    ) : CheckoutResult
}

/** This result naming [orderId] when it names none (the checkout was closed before it learned the order). */
internal fun CheckoutResult.withOrderId(orderId: String): CheckoutResult =
    if (this.orderId.isNotEmpty()) {
        this
    } else {
        when (this) {
            is CheckoutResult.Completed -> copy(orderId = orderId)
            is CheckoutResult.Failed -> copy(orderId = orderId)
            is CheckoutResult.Canceled -> copy(orderId = orderId)
            is CheckoutResult.Unknown -> copy(orderId = orderId)
        }
    }
