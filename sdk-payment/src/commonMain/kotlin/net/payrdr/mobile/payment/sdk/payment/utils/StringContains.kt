package net.payrdr.mobile.payment.sdk.payment.utils

/**
 * Returns `true` iff the receiver contains at least one of the provided [statuses] as a
 * substring, matched case-insensitively.
 */
internal fun String.containsAnyOfKeywordIgnoreCase(statuses: List<OrderStatus>): Boolean =
    statuses.any { this.contains(it.wireValue, ignoreCase = true) }
