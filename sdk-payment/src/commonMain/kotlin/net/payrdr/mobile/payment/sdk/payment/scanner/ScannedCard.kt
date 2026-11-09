package net.payrdr.mobile.payment.sdk.payment.scanner

/**
 * Card data extracted from a camera frame. Only returned when PAN passes Luhn and expiry is not in the past.
 *
 * @property pan 13..19 digit string (no separators)
 * @property expiryMonth 1..12
 * @property expiryYear 4-digit year, e.g. 2026
 * @property cardholder null if `CardScannerConfig.extractCardholder` is false or not detected
 */
public data class ScannedCard(
    val pan: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cardholder: String?,
)
