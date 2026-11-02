package net.payrdr.mobile.payment.sdk.payment.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Text styles used across the checkout UI. Fonts stay system (SF Pro on iOS, Roboto on Android);
 * amounts and card numbers use tabular figures so digits line up.
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
@Immutable
public data class PaymentTypography(
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val button: TextStyle,
    val input: TextStyle,
    val headline: TextStyle = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    val amount: TextStyle =
        TextStyle(
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 34.sp,
            letterSpacing = (-0.5).sp,
            fontFeatureSettings = TABULAR,
        ),
    val caption: TextStyle = TextStyle(fontSize = 12.5.sp, lineHeight = 16.sp),
) {
    public companion object {
        /** Default typography used when the merchant does not provide overrides. */
        public val Default: PaymentTypography =
            PaymentTypography(
                title = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
                body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
                label = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp),
                button = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                input = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
            )
    }
}

/** OpenType feature string for tabular (fixed-width) figures. */
internal const val TABULAR: String = "tnum"

/** Same style with tabular figures — card numbers, masks, amounts, codes. */
internal fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR)
