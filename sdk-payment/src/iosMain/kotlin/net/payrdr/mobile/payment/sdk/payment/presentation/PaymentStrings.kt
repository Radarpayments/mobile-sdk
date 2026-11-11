package net.payrdr.mobile.payment.sdk.payment.presentation

/**
 * The checkout's texts in [language] for the SwiftUI screens: the Compose string resources, compiled into tables by
 * the `generatePaymentStrings` task. A language the SDK is not translated into reads in English.
 */
@PayrdrUiApi
public class PaymentStrings(
    public val language: String,
) {
    private val catalog = StringCatalog.of(language, PaymentStringTables::table)

    /** The text of the string resource [key], e.g. `"card_form_pay"`. */
    public fun text(key: String): String = catalog.text(key)

    /** [text] with its `%1$s`-style placeholders filled from [args]. */
    public fun format(
        key: String,
        vararg args: String,
    ): String = catalog.format(key, args.asList())
}
