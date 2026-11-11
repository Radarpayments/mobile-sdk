package net.payrdr.mobile.payment.sdk.payment.scanner.presentation

/**
 * The scanner's texts in [language] over the English ones, as the checkout's `PaymentStrings` reads its own: a missing
 * text reads in English, an unknown key as the key itself.
 */
internal class ScannerTexts(
    language: String,
) {
    private val english: Map<String, String> = PaymentStringTables.table(ENGLISH).orEmpty()
    private val table: Map<String, String> = PaymentStringTables.table(language) ?: english

    fun text(key: String): String = table[key] ?: english[key] ?: key

    private companion object {
        const val ENGLISH = "en"
    }
}
