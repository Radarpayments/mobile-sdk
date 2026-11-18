package net.payrdr.mobile.payment.sdk.qa.config

internal object Currencies {
    private val numericByAlpha =
        linkedMapOf(
            "EUR" to "978",
            "USD" to "840",
            "GBP" to "826",
            "CHF" to "756",
            "RUB" to "643",
            "KZT" to "398",
            "BGN" to "975",
            "RON" to "946",
            "PLN" to "985",
            "CZK" to "203",
            "TRY" to "949",
            "AED" to "784",
            "UAH" to "980",
            "BYN" to "933",
            "GEL" to "981",
            "AMD" to "051",
            "AZN" to "944",
            "UZS" to "860",
        )

    val codes: List<String> = numericByAlpha.keys.toList()

    fun numeric(alpha: String): String? = numericByAlpha[alpha]
}
