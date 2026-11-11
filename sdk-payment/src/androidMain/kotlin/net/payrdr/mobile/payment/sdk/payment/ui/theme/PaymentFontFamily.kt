package net.payrdr.mobile.payment.sdk.payment.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * A font family the app ships, as Compose loads it: `PaymentFontFamily(FontFamily(Font(R.font.inter)))`.
 *
 * @property fontFamily the Compose family the checkout draws in
 */
public actual class PaymentFontFamily(
    public val fontFamily: FontFamily,
) {
    override fun equals(other: Any?): Boolean = other is PaymentFontFamily && other.fontFamily == fontFamily

    override fun hashCode(): Int = fontFamily.hashCode()

    override fun toString(): String = "PaymentFontFamily($fontFamily)"
}

/** The family Compose draws [this] in; null for the system font. */
internal fun PaymentFontFamily.composeFontFamily(): FontFamily? = fontFamily
