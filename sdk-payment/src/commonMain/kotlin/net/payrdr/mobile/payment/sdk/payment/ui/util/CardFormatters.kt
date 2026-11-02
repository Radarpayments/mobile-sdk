package net.payrdr.mobile.payment.sdk.payment.ui.util

/**
 * Pure-function helpers used by card-form text fields to format input on every keystroke
 * and to mask saved-card PANs.
 */
public object CardFormatters {
    /**
     * Returns only the digit characters of [value]. Used to strip input before validation and re-formatting.
     */
    public fun digitsOnly(value: String): String = value.filter { it.isDigit() }

    /**
     * Formats a PAN with whitespace grouping. Default groups are `4-4-4-4` (Visa, Mastercard, UnionPay);
     * if the resolved [CardNetwork] is Amex, groups are `4-6-5`. Anything else is treated as 4-4-4-4.
     */
    public fun formatPan(value: String): String {
        val digits = digitsOnly(value)
        if (digits.isEmpty()) return ""
        val network = CardNetworkResolver.resolve(digits)
        val groups =
            when (network) {
                CardNetwork.AMEX -> intArrayOf(4, 6, 5)
                else -> intArrayOf(4, 4, 4, 4, 3)
            }
        val sb = StringBuilder()
        var cursor = 0
        for (groupSize in groups) {
            if (cursor >= digits.length) break
            if (sb.isNotEmpty()) sb.append(' ')
            val end = (cursor + groupSize).coerceAtMost(digits.length)
            sb.append(digits, cursor, end)
            cursor = end
        }
        return sb.toString()
    }

    /**
     * Formats an expiry as `MM/YY`, inserting the slash automatically as the user types.
     * Digits beyond the 4th are dropped.
     */
    public fun formatExpiry(value: String): String {
        val digits = digitsOnly(value).take(4)
        return when {
            digits.length <= 2 -> digits
            else -> digits.substring(0, 2) + "/" + digits.substring(2)
        }
    }

    /** Longest PAN the [network] issues — input beyond this is dropped. */
    public fun maxPanLength(network: CardNetwork): Int = panLengths(network).last()

    /**
     * PAN lengths the [network] issues (ISO/IEC 7812 caps a PAN at 19 digits): Visa 16 or 19 (13 is legacy),
     * Mastercard 16, Amex 15, Diners Club 14–19, Maestro 12–19, UnionPay, JCB and Discover 16–19.
     * Unknown networks get 16–19 as well.
     */
    internal fun panLengths(network: CardNetwork): List<Int> =
        when (network) {
            CardNetwork.AMEX -> listOf(15)
            CardNetwork.VISA -> listOf(16, 19)
            CardNetwork.MASTERCARD -> listOf(16)
            CardNetwork.DINERS -> (14..19).toList()
            CardNetwork.MAESTRO -> (12..19).toList()
            CardNetwork.UNIONPAY, CardNetwork.JCB, CardNetwork.DISCOVER, CardNetwork.UNKNOWN -> (16..19).toList()
        }

    /** CVV length printed on cards of [network]: 4 on the front for Amex, 3 on the back otherwise. */
    public fun cvvLength(network: CardNetwork): Int = if (network == CardNetwork.AMEX) 4 else 3

    /** Short mask for saved cards: `•••• 1111`. Accepts cleartext or gateway labels like `411111**1111`. */
    public fun shortMask(pan: String): String {
        val digits = digitsOnly(pan)
        return if (digits.length < 4) "••••" else "•••• ${digits.takeLast(4)}"
    }

    /**
     * Produces a `•••• •••• •••• NNNN`-style mask from a cleartext PAN. Used by [CardListScreen]
     * when rendering saved bindings. For PANs shorter than 4 digits the whole thing is bulleted.
     */
    public fun maskPan(pan: String): String {
        val digits = digitsOnly(pan)
        if (digits.length < 4) return "•".repeat(digits.length)
        val last4 = digits.takeLast(4)
        return "•••• •••• •••• $last4"
    }

    /**
     * Splits a gateway binding label such as `411111**1111 12/26` into the masked PAN and the expiry.
     * The label carries both, so masking it as a whole would take the last four digits from the expiry.
     *
     * @return the masked PAN and the `MM/YY` expiry, or null if the label has no expiry.
     */
    public fun splitBindingLabel(label: String): Pair<String, String?> {
        val parts = label.trim().split(WHITESPACE)
        val expiry = parts.lastOrNull()?.takeIf { parts.size > 1 && LABEL_EXPIRY.matches(it) }
        val maskedPan = if (expiry == null) label.trim() else parts.dropLast(1).joinToString(" ")
        return maskedPan to expiry
    }

    private val WHITESPACE = Regex("\\s+")
    private val LABEL_EXPIRY = Regex("""\d{2}/\d{2}(\d{2})?""")
}
