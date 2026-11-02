package net.payrdr.mobile.payment.sdk.payment.utils

/**
 * Payment order lifecycle statuses returned by the gateway.
 * Values are matched case-insensitively against `getFinishedPaymentInfo.status` wire values.
 */
public enum class OrderStatus(
    public val wireValue: String,
) {
    CREATED("CREATED"),
    APPROVED("APPROVED"),
    DEPOSITED("DEPOSITED"),
    DECLINED("DECLINED"),
    REVERSED("REVERSED"),
    REFUNDED("REFUNDED"),
}

/** Statuses that indicate the payment was already accepted. */
public val ORDER_STATUS_PAID: List<OrderStatus> = listOf(OrderStatus.APPROVED, OrderStatus.DEPOSITED)

/** Statuses that indicate a definitive failure. */
public val ORDER_STATUS_FAILED: List<OrderStatus> = listOf(OrderStatus.DECLINED)
