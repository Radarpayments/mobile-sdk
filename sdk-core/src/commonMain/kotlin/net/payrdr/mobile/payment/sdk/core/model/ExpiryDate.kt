package net.payrdr.mobile.payment.sdk.core.model

/**
 * Card expiration date — stored in full four-digit year plus numeric month.
 *
 * @property expYear full year, e.g. `2028`
 * @property expMonth month, 1..12
 */
public data class ExpiryDate(
    val expYear: Int,
    val expMonth: Int,
)
