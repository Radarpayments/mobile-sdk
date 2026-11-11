package net.payrdr.mobile.payment.sdk.payment.presentation

/**
 * The texts of one language over the English ones: a key the language lacks reads in English, a key nobody has reads
 * as the key itself, so a missing text shows up on screen instead of crashing the checkout.
 */
internal class StringCatalog(
    private val table: Map<String, String>,
    private val english: Map<String, String>,
) {
    fun text(key: String): String = table[key] ?: english[key] ?: key

    /** [text] with its `%1$s`-style placeholders filled from [args]; a placeholder without an argument stays. */
    fun format(
        key: String,
        args: List<String>,
    ): String =
        Placeholder.replace(text(key)) { match ->
            args.getOrNull(match.groupValues[1].toInt() - 1) ?: match.value
        }

    companion object {
        /** The catalog of [language] out of [tables] (language to its texts); an unknown language reads in English. */
        fun of(
            language: String,
            tables: (String) -> Map<String, String>?,
        ): StringCatalog {
            val english = tables(ENGLISH).orEmpty()
            return StringCatalog(tables(language) ?: english, english)
        }

        const val ENGLISH = "en"

        // The placeholders Compose resources fill.
        private val Placeholder = Regex("""%(\d+)\$[ds]""")
    }
}
