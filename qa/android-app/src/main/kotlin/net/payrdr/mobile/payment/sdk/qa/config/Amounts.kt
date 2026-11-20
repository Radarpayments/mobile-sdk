package net.payrdr.mobile.payment.sdk.qa.config

/** [minor] units of a currency with [digits] minor digits as major units: 10000 with 2 is `100.00`, 5 is `0.05`. */
internal fun minorToMajor(
    minor: Long,
    digits: Int,
): String {
    if (digits == 0) return minor.toString()
    val text = minor.toString().padStart(digits + 1, '0')
    return text.dropLast(digits) + "." + text.takeLast(digits)
}

/**
 * Major units typed by a tester as minor units: ASCII digits with at most one `.` or `,` and at most [digits] digits after
 * it (`10`, `10.5`, `10,50`, `10.` all read). Null for any other text, a sign, a space or another script's digits included,
 * and for a value beyond 18 digits, which no amount of the contract reaches.
 */
internal fun majorToMinor(
    text: String,
    digits: Int,
): Long? {
    val separator = text.indexOfFirst { it == '.' || it == ',' }
    val whole = if (separator < 0) text else text.substring(0, separator)
    val fraction = if (separator < 0) "" else text.substring(separator + 1)
    val valid =
        whole.isNotEmpty() &&
            whole.all { it in '0'..'9' } &&
            fraction.all { it in '0'..'9' } &&
            fraction.length <= digits &&
            (separator < 0 || digits > 0)
    if (!valid) return null
    val minor = (whole + fraction.padEnd(digits, '0')).trimStart('0').ifEmpty { "0" }
    return minor.takeIf { it.length <= MAX_DIGITS }?.toLong()
}

private const val MAX_DIGITS = 18
