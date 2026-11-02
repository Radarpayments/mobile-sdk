package net.payrdr.mobile.payment.sdk.payment.ui.util

private const val REGIONAL_INDICATOR_A = 0x1F1E6
private const val SUPPLEMENTARY_BASE = 0x10000

/** Flag emoji of a country: `GB` → 🇬🇧 (two regional indicator symbols). Empty for a malformed code. */
internal fun flagEmoji(iso: String): String {
    val code = iso.uppercase()
    if (code.length != 2 || code.any { it !in 'A'..'Z' }) return ""
    return buildString {
        code.forEach { letter ->
            val point = REGIONAL_INDICATOR_A + (letter - 'A') - SUPPLEMENTARY_BASE
            append((0xD800 + (point shr 10)).toChar())
            append((0xDC00 + (point and 0x3FF)).toChar())
        }
    }
}
