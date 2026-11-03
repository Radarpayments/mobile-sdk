package net.payrdr.mobile.payment.sdk.payment.utils

/**
 * Formats the session amount for display. `getSessionStatus.do` returns `amount` either as minor
 * units (`"4990"`) or already formatted (`"49.90"`, `"1.00 EUR"`); both are normalised to the same
 * look: currency sign instead of the ISO code (`€`, `₺`, ...) and the fraction always written out
 * (`€1.00`, `€1.34`). A string that cannot be parsed is passed through untouched.
 */
public object MoneyFormatter {
    private val PREFIX_SYMBOLS = mapOf("EUR" to "€", "USD" to "$", "GBP" to "£", "JPY" to "¥", "CNY" to "¥")
    private val SUFFIX_SYMBOLS =
        mapOf(
            "KZT" to "₸",
            "UAH" to "₴",
            "TRY" to "₺",
            "INR" to "₹",
            "GEL" to "₾",
            "AMD" to "֏",
            "AZN" to "₼",
            "BYN" to "Br",
        )
    private val ZERO_DECIMAL = setOf("JPY", "KRW", "VND", "CLP", "ISK", "UGX", "XAF", "XOF")
    private val THREE_DECIMAL = setOf("BHD", "KWD", "OMR", "JOD", "TND", "IQD", "LYD")

    /**
     * @param amount raw `amount` value from the session
     * @param currency ISO 4217 alpha code, e.g. `EUR`; null = no currency mark unless [amount] carries one
     * @return display string such as `€49.90` / `1,250 ₺`, or null when [amount] is blank
     */
    public fun format(
        amount: String?,
        currency: String?,
    ): String? = format(amount, currency, null)

    /**
     * [format] in the conventions of [language] (`"de"`, `"fr"`, …): its decimal and thousands separators, and the
     * currency sign where that language puts it — `€1,234.50` in English, `1.234,50 €` in German. null or a
     * language without its own conventions here keeps the English look.
     */
    public fun format(
        amount: String?,
        currency: String?,
        language: String?,
    ): String? {
        val raw = amount?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val parsed = PREFORMATTED.matchEntire(raw)
        val code = (parsed?.groupValues?.get(2)?.ifEmpty { null } ?: currency)?.trim()?.uppercase().orEmpty()
        val exponent = exponentOf(code)
        val minor = raw.toLongOrNull() ?: parsed?.let { toMinor(it.groupValues[1], exponent) } ?: return raw
        val style = Style.of(language)
        val number = formatMinor(minor, exponent, style)
        return when {
            code.isEmpty() -> number
            code in PREFIX_SYMBOLS && style.signFirst -> PREFIX_SYMBOLS.getValue(code) + number
            code in PREFIX_SYMBOLS -> number + style.space + PREFIX_SYMBOLS.getValue(code)
            code in SUFFIX_SYMBOLS -> number + style.space + SUFFIX_SYMBOLS.getValue(code)
            else -> number + style.space + code
        }
    }

    /**
     * The session amount in minor units (`"49.90 EUR"` → `4990`), for wallet sheets that need a number;
     * null when [amount] is blank or cannot be parsed.
     */
    internal fun minorUnits(
        amount: String?,
        currency: String?,
    ): Long? {
        val raw = amount?.trim().orEmpty()
        raw.toLongOrNull()?.let { return it }
        val parsed = PREFORMATTED.matchEntire(raw) ?: return null
        val code = (parsed.groupValues[2].ifEmpty { null } ?: currency)?.trim()?.uppercase().orEmpty()
        return toMinor(parsed.groupValues[1], exponentOf(code))
    }

    /**
     * [minor] units of [currency] as the plain major-unit number wallet sheets take: dot-decimal, no grouping, the
     * currency's own number of decimals (`10055 EUR` → `100.55`, `1500 JPY` → `1500`, `1500 KWD` → `1.500`).
     */
    internal fun majorString(
        minor: Long,
        currency: String,
    ): String = formatMinor(minor, exponentOf(currency.trim().uppercase()), Style.PLAIN)

    /** `1.00 EUR`, `1 250,50 TRY`, `-49.90`: number (digits with separators) and an optional ISO code. */
    private val PREFORMATTED = Regex("""(-?\d[\d\s.,\u00A0]*)\s*([A-Za-z]{3})?""")

    /**
     * Converts a formatted number to minor units. The last `.` or `,` is the decimal separator only if it
     * is followed by exactly [exponent] digits; otherwise every separator groups thousands (`1,000`).
     */
    private fun toMinor(
        text: String,
        exponent: Int,
    ): Long? {
        val number = text.filterNot { it.isWhitespace() || it == '\u00A0' }
        val negative = number.startsWith("-")
        val body = number.removePrefix("-")
        val separator = body.indexOfLast { it == '.' || it == ',' }
        val hasFraction = exponent > 0 && separator >= 0 && body.length - separator - 1 == exponent
        val whole = (if (hasFraction) body.substring(0, separator) else body).filter { it.isDigit() }
        val fraction = if (hasFraction) body.substring(separator + 1) else ""
        if (whole.isEmpty() || !fraction.all { it.isDigit() }) return null
        val minor = (whole + fraction.padEnd(exponent, '0')).toLongOrNull() ?: return null
        return if (negative) -minor else minor
    }

    private fun exponentOf(code: String): Int =
        when (code) {
            in ZERO_DECIMAL -> 0
            in THREE_DECIMAL -> 3
            else -> 2
        }

    /** Whole part grouped by thousands, then the fraction in full, zeros included (none for zero-decimal currencies). */
    private fun formatMinor(
        minor: Long,
        exponent: Int,
        style: Style,
    ): String {
        val negative = minor < 0
        val digits = (if (negative) -minor else minor).toString().padStart(exponent + 1, '0')
        val whole = digits.dropLast(exponent).ifEmpty { "0" }
        val fraction = digits.takeLast(exponent)
        val grouped =
            if (whole.length < style.groupFrom) {
                whole
            } else {
                whole
                    .reversed()
                    .chunked(3)
                    .joinToString(style.group.reversed())
                    .reversed()
            }
        val sign = if (negative) "-" else ""
        return if (exponent == 0) sign + grouped else sign + grouped + style.decimal + fraction
    }

    /**
     * How a language writes amounts: thousands [group] separator (from [groupFrom] whole digits), [decimal]
     * separator, whether the currency sign comes first, and the [space] before a sign or code after the number.
     */
    private class Style(
        val group: String,
        val decimal: String,
        val signFirst: Boolean,
        val space: String,
        val groupFrom: Int = 4,
    ) {
        companion object {
            private const val NBSP = "\u00A0"
            private val ENGLISH = Style(",", ".", signFirst = true, space = " ")

            /** No grouping at all: `groupFrom` past any [Long]'s digits. */
            val PLAIN = Style("", ".", signFirst = true, space = "", groupFrom = Int.MAX_VALUE)

            fun of(language: String?): Style =
                when (language?.lowercase()?.substringBefore('-')) {
                    "de", "ro", "el" -> Style(".", ",", signFirst = false, space = NBSP)
                    "es" -> Style(".", ",", signFirst = false, space = NBSP, groupFrom = 5)
                    "ru", "bg" -> Style(NBSP, ",", signFirst = false, space = NBSP)
                    "fr" -> Style("\u202F", ",", signFirst = false, space = NBSP)
                    "ar" -> Style(",", ".", signFirst = false, space = NBSP)
                    else -> ENGLISH
                }
        }
    }
}
