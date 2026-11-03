package net.payrdr.mobile.payment.samples.android.core

/** Card network by the number's leading digits, with the ranges, lengths and grouping the SDK's own card form uses. */
internal enum class CardNetwork(
    val displayName: String,
) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMEX("American Express"),
    UNIONPAY("UnionPay"),
    JCB("JCB"),
    DISCOVER("Discover"),
    DINERS("Diners Club"),
    MAESTRO("Maestro"),
    UNKNOWN(""),
    ;

    /** Longest number the network issues: input beyond it is dropped. */
    val maxPanLength: Int
        get() =
            when (this) {
                AMEX -> 15
                MASTERCARD -> 16
                else -> 19
            }

    /** 4 digits on the front for Amex, 3 on the back otherwise. */
    val cvcLength: Int get() = if (this == AMEX) 4 else 3

    /** [digits] in the network's groups: "4111 1111 1111 1111", Amex 4-6-5. */
    fun format(digits: String): String {
        val groups = if (this == AMEX) listOf(4, 6, 5) else listOf(4, 4, 4, 4, 3)
        var rest = digits.filter(Char::isDigit)
        val parts = mutableListOf<String>()
        for (size in groups) {
            if (rest.isEmpty()) break
            parts += rest.take(size)
            rest = rest.drop(size)
        }
        return parts.joinToString(" ")
    }

    companion object {
        fun of(cardNumber: String): CardNetwork {
            val digits = cardNumber.filter(Char::isDigit)
            val two = digits.take(2).takeIf { it.length == 2 }?.toInt()
            val three = digits.take(3).takeIf { it.length == 3 }?.toInt()
            val four = digits.take(4).takeIf { it.length == 4 }?.toInt()
            return when {
                digits.startsWith("4") -> VISA
                two == 34 || two == 37 -> AMEX
                two in 51..55 || four in 2221..2720 -> MASTERCARD
                two == 62 -> UNIONPAY
                else -> others(two, three, four)
            }
        }

        /** Diners Club, JCB, Discover and Maestro: ranges that need up to four digits to tell apart. */
        private fun others(
            two: Int?,
            three: Int?,
            four: Int?,
        ): CardNetwork =
            when {
                two == 36 || two == 38 || two == 39 || three in 300..305 || four == 3095 -> DINERS
                four in 3528..3589 -> JCB
                four == 6011 || three in 644..649 || two == 65 -> DISCOVER
                two == 50 || two in 56..58 || four == 6304 || four == 6759 || four in 6761..6763 -> MAESTRO
                else -> UNKNOWN
            }
    }
}
