package net.payrdr.mobile.payment.sdk.payment.ui.theme

/**
 * Text styles used across the checkout UI. Fonts are the system ones unless a style sets
 * [PaymentTextStyle.fontFamily]; amounts and card numbers use tabular figures so digits line up.
 *
 * - [title] — app-bar titles, sheet dialog titles
 * - [body] — row titles, body copy
 * - [label] — field labels, section headers, secondary row text
 * - [button] — button labels
 * - [input] — text-field input
 * - [headline] — processing / result headline
 * - [amount] — order amount in the payment-sheet header and on the result screen
 * - [caption] — helper / error text under fields, security footnote
 */
public data class PaymentTypography(
    val title: PaymentTextStyle,
    val body: PaymentTextStyle,
    val label: PaymentTextStyle,
    val button: PaymentTextStyle,
    val input: PaymentTextStyle,
    val headline: PaymentTextStyle =
        PaymentTextStyle(size = 21f, weight = PaymentTextStyle.SEMI_BOLD, lineHeight = 26f),
    val amount: PaymentTextStyle =
        PaymentTextStyle(size = 30f, weight = PaymentTextStyle.SEMI_BOLD, lineHeight = 34f, letterSpacing = -0.5f, tabularFigures = true),
    val caption: PaymentTextStyle = PaymentTextStyle(size = 12.5f, lineHeight = 16f),
) {
    /** The SDK's default text styles. */
    public companion object {
        /** The SDK's own text styles. */
        public val Default: PaymentTypography =
            PaymentTypography(
                title = PaymentTextStyle(size = 17f, weight = PaymentTextStyle.SEMI_BOLD, lineHeight = 22f),
                body = PaymentTextStyle(size = 15f, lineHeight = 20f),
                label = PaymentTextStyle(size = 13f, weight = PaymentTextStyle.MEDIUM, lineHeight = 18f),
                button = PaymentTextStyle(size = 16f, weight = PaymentTextStyle.SEMI_BOLD),
                input = PaymentTextStyle(size = 16f),
            )
    }
}
