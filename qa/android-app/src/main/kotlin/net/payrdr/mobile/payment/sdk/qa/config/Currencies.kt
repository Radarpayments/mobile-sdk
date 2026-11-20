package net.payrdr.mobile.payment.sdk.qa.config

internal object Currencies {
    private class Currency(
        val numeric: String,
        val minorDigits: Int,
    )

    // The minor digits are ISO 4217's exponents. The form shows the amount in major units with them; the document keeps
    // minor units, so a currency added here with a wrong exponent shifts what the tester types by powers of ten.
    private val byAlpha =
        linkedMapOf(
            "EUR" to Currency("978", 2),
            "USD" to Currency("840", 2),
            "GBP" to Currency("826", 2),
            "CHF" to Currency("756", 2),
            "RUB" to Currency("643", 2),
            "KZT" to Currency("398", 2),
            "BGN" to Currency("975", 2),
            "RON" to Currency("946", 2),
            "PLN" to Currency("985", 2),
            "CZK" to Currency("203", 2),
            "TRY" to Currency("949", 2),
            "AED" to Currency("784", 2),
            "UAH" to Currency("980", 2),
            "BYN" to Currency("933", 2),
            "GEL" to Currency("981", 2),
            "AMD" to Currency("051", 2),
            "AZN" to Currency("944", 2),
            "UZS" to Currency("860", 2),
        )

    val codes: List<String> = byAlpha.keys.toList()

    fun numeric(alpha: String): String? = byAlpha[alpha]?.numeric

    /** The digits after the decimal point; an unknown code gets two, which the document's own check then rejects. */
    fun minorDigits(alpha: String): Int = byAlpha[alpha]?.minorDigits ?: DEFAULT_MINOR_DIGITS

    private const val DEFAULT_MINOR_DIGITS = 2
}
