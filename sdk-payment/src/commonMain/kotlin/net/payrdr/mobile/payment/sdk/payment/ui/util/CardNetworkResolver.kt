package net.payrdr.mobile.payment.sdk.payment.ui.util

/**
 * Resolves a partial or complete card number to its [CardNetwork] by looking at the leading digits.
 * Ranges follow ISO/IEC 7812-1 BIN assignments and publicly documented processor ranges.
 */
public object CardNetworkResolver {
    public fun resolve(cardNumber: String): CardNetwork {
        val digits = cardNumber.filter { it.isDigit() }
        if (digits.isEmpty()) return CardNetwork.UNKNOWN

        if (digits[0] == '4') return CardNetwork.VISA

        if (digits.length >= 2) {
            val twoPrefix = digits.take(2)
            if (twoPrefix == "34" || twoPrefix == "37") return CardNetwork.AMEX
        }

        if (digits.length >= 2) {
            val twoInt = digits.take(2).toIntOrNull()
            if (twoInt != null && twoInt in 51..55) return CardNetwork.MASTERCARD
        }
        if (digits.length >= 4) {
            val fourInt = digits.take(4).toIntOrNull()
            if (fourInt != null && fourInt in 2221..2720) return CardNetwork.MASTERCARD
        }

        if (digits.length >= 2) {
            val twoPrefix = digits.take(2)
            if (twoPrefix == "62") return CardNetwork.UNIONPAY
        }

        return resolveOthers(digits)
    }

    /** JCB, Diners Club, Discover and Maestro: ranges that need up to four digits to tell apart. */
    private fun resolveOthers(digits: String): CardNetwork {
        val two = digits.take(2).toIntOrNull().takeIf { digits.length >= 2 }
        val three = digits.take(3).toIntOrNull().takeIf { digits.length >= 3 }
        val four = digits.take(4).toIntOrNull().takeIf { digits.length >= 4 }
        return when {
            two == 36 || two == 38 || two == 39 || three in 300..305 || four == 3095 -> CardNetwork.DINERS
            four in 3528..3589 -> CardNetwork.JCB
            four == 6011 || three in 644..649 || two == 65 -> CardNetwork.DISCOVER
            two == 50 || two in 56..58 || four == 6304 || four == 6759 || four in 6761..6763 -> CardNetwork.MAESTRO
            else -> CardNetwork.UNKNOWN
        }
    }

    /**
     * Network of a saved binding: from the gateway's `paymentSystem` name first (`VISA`,
     * `MASTERCARD`, …), falling back to the BIN inside [maskedPan].
     */
    public fun resolveSaved(
        paymentSystem: String,
        maskedPan: String,
    ): CardNetwork {
        val name = paymentSystem.uppercase()
        return when {
            "VISA" in name -> CardNetwork.VISA
            "MAESTRO" in name -> CardNetwork.MAESTRO
            "MASTER" in name -> CardNetwork.MASTERCARD
            "AMEX" in name || "AMERICAN" in name -> CardNetwork.AMEX
            "UNION" in name || "CUP" in name -> CardNetwork.UNIONPAY
            "JCB" in name -> CardNetwork.JCB
            "DISCOVER" in name -> CardNetwork.DISCOVER
            "DINERS" in name -> CardNetwork.DINERS
            else -> resolve(maskedPan.takeWhile { it.isDigit() })
        }
    }
}
