package net.payrdr.mobile.payment.samples.android.store.invest

/**
 * The amount typed on the top-up keypad, in euros with `.` as the separator whatever the locale shows: at most six
 * whole digits and two decimals, no leading zeros.
 */
data class TopUpAmount(
    val text: String = "100",
) {
    /** Euro cents, or null while nothing is typed. */
    val minor: Long?
        get() {
            if (text.isEmpty()) return null
            val whole = text.substringBefore('.').ifEmpty { "0" }.toLong()
            val cents = text.substringAfter('.', "").padEnd(2, '0').toLong()
            return whole * CENTS + cents
        }

    val valid: Boolean get() = minor?.let { it in MIN..MAX } ?: false

    /** The keypad pressed [c]: a digit or `.`; anything that would break the format is ignored. */
    fun type(c: Char): TopUpAmount {
        val separator = text.indexOf('.')
        return when {
            c == '.' -> if (separator >= 0) this else TopUpAmount(text.ifEmpty { "0" } + ".")
            c !in '0'..'9' -> this
            separator >= 0 -> if (text.length - separator > DECIMALS) this else TopUpAmount(text + c)
            text == "0" -> TopUpAmount(c.toString())
            text.length >= WHOLE_DIGITS -> this
            else -> TopUpAmount(text + c)
        }
    }

    /** The amount without its last character. */
    fun backspace(): TopUpAmount = TopUpAmount(text.dropLast(1))

    /** Limits and presets of a top-up, in euro cents. */
    companion object {
        const val MIN = 1000L
        const val MAX = 100000L
        val PRESETS = listOf(5000L, 10000L, 25000L, 50000L)
        private const val CENTS = 100
        private const val DECIMALS = 2
        private const val WHOLE_DIGITS = 6

        /**
         * What the system keyboard left in the field, cleaned as if typed key by key: the locale's separator (`,` or
         * the Arabic `٫`) becomes `.`, Arabic-Indic and Persian digits become ASCII ones, anything else that does not
         * fit the format is dropped.
         */
        fun parse(input: String): TopUpAmount = input.fold(TopUpAmount("")) { amount, c -> amount.type(ascii(c)) }

        private fun ascii(c: Char): Char =
            when (c) {
                ',', ARABIC_SEPARATOR -> '.'
                in ARABIC_ZERO..ARABIC_ZERO + 9 -> '0' + (c - ARABIC_ZERO)
                in PERSIAN_ZERO..PERSIAN_ZERO + 9 -> '0' + (c - PERSIAN_ZERO)
                else -> c
            }

        private const val ARABIC_SEPARATOR = '\u066B'
        private const val ARABIC_ZERO = '\u0660'
        private const val PERSIAN_ZERO = '\u06F0'

        /** The amount of a preset chip. */
        fun preset(minor: Long): TopUpAmount {
            val cents = minor % CENTS
            val whole = (minor / CENTS).toString()
            return TopUpAmount(if (cents == 0L) whole else "$whole.${cents.toString().padStart(2, '0').trimEnd('0')}")
        }
    }
}

/** Where the top-up screen is in the transfer. */
sealed interface TopUpStatus {
    data object Editing : TopUpStatus

    data object Registering : TopUpStatus

    data object InSheet : TopUpStatus

    /** The bank or the gateway refused; [message] is the SDK's text, when it has one. */
    data class Declined(
        val message: String?,
    ) : TopUpStatus

    /** How the transfer ended is not known: the money is not credited and the app does not offer to pay again. */
    data object Unconfirmed : TopUpStatus

    /** Credited: [amountMinor] of order [orderId] took free cash from [freeCashBefore] to [freeCashAfter] at [at]. */
    data class Done(
        val orderId: String,
        val amountMinor: Long,
        val freeCashBefore: Long,
        val freeCashAfter: Long,
        val at: Long,
    ) : TopUpStatus
}

/** Whether "Continue" may start a transfer of [amount] now. */
fun TopUpStatus.canContinue(amount: TopUpAmount): Boolean = amount.valid && (this == TopUpStatus.Editing || this is TopUpStatus.Declined)
