package net.payrdr.mobile.payment.sdk.core.utils

import net.payrdr.mobile.payment.sdk.core.model.ExpiryDate

/**
 * Strips PEM armor (`-----BEGIN/END PUBLIC KEY-----`, `-----BEGIN/END CERTIFICATE-----`)
 * and all whitespace from a PEM-encoded key, returning only the Base64 body.
 */
public fun String.pemKeyContent(): String =
    replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("-----BEGIN CERTIFICATE-----", "")
        .replace("-----END CERTIFICATE-----", "")
        .replace(Regex("\\s+"), "")

/**
 * Returns the receiver with all non-digit characters removed. If [maxLength] is set,
 * the resulting string is truncated to that many leading characters.
 */
public fun String.digitsOnly(maxLength: Int? = null): String {
    val digits = replace(Regex("[^\\d.]"), "")
    return if (maxLength != null) digits.take(maxLength) else digits
}

/**
 * Returns the receiver with all whitespace characters removed. If [maxLength] is set,
 * the resulting string is truncated to that many leading characters.
 */
public fun String.noSpaces(maxLength: Int? = null): String {
    val trimmed = replace(Regex("[\\s.]"), "")
    return if (maxLength != null) trimmed.take(maxLength) else trimmed
}

/**
 * Parses an `MM/YY` string into an [ExpiryDate] with the year resolved to `2000 + YY`.
 *
 * @throws IllegalArgumentException if the input does not match `^\d{2}/\d{2}$`.
 */
public fun String.toExpDate(): ExpiryDate {
    require(matches(Regex("\\d{2}/\\d{2}"))) { "Incorrect format, should be MM/YY." }
    val month = substring(0, 2).toInt()
    val year = substring(3, 5).toInt() + EXP_YEAR_OFFSET
    return ExpiryDate(expYear = year, expMonth = month)
}

private const val EXP_YEAR_OFFSET = 2000
