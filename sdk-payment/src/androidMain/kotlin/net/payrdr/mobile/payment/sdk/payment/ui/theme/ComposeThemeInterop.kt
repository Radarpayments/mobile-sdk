package net.payrdr.mobile.payment.sdk.payment.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified

/** [this] as a theme colour, converted to sRGB. */
public fun Color.toPaymentColor(): PaymentColor = PaymentColor(toArgb().toLong() and ARGB_MASK)

/** [this] as a Compose colour. */
public fun PaymentColor.toColor(): Color = Color(argb)

/**
 * [this] as a theme text style: size, weight, line height, letter spacing, font family and tabular figures (the `tnum`
 * feature). Units other than sp, colour, decoration and other font features are dropped; an unset size is 14 sp.
 */
public fun TextStyle.toPaymentTextStyle(): PaymentTextStyle =
    PaymentTextStyle(
        size = fontSize.sp(DEFAULT_FONT_SIZE),
        weight = fontWeight?.weight ?: FontWeight.Normal.weight,
        lineHeight = lineHeight.sp(0f),
        letterSpacing = letterSpacing.sp(0f),
        tabularFigures = fontFeatureSettings?.contains(TABULAR) == true,
        fontFamily = fontFamily?.let(::PaymentFontFamily),
    )

/** A palette from Compose colours, with the same defaults as the [PaymentColors] constructor. */
@Suppress("FunctionNaming", "LongParameterList")
public fun PaymentColors(
    primary: Color,
    onPrimary: Color,
    background: Color,
    onBackground: Color,
    surface: Color,
    onSurface: Color,
    error: Color,
    onError: Color,
    outline: Color,
    success: Color,
    onSurfaceVariant: Color = Color.Unspecified,
    placeholder: Color = Color.Unspecified,
    outlineStrong: Color = Color.Unspecified,
    scrim: Color = Color.Unspecified,
): PaymentColors {
    val stock =
        PaymentColors(
            primary = primary.toPaymentColor(),
            onPrimary = onPrimary.toPaymentColor(),
            background = background.toPaymentColor(),
            onBackground = onBackground.toPaymentColor(),
            surface = surface.toPaymentColor(),
            onSurface = onSurface.toPaymentColor(),
            error = error.toPaymentColor(),
            onError = onError.toPaymentColor(),
            outline = outline.toPaymentColor(),
            success = success.toPaymentColor(),
        )
    return stock.copy(
        onSurfaceVariant = onSurfaceVariant.orDefault(stock.onSurfaceVariant),
        placeholder = placeholder.orDefault(stock.placeholder),
        outlineStrong = outlineStrong.orDefault(stock.outlineStrong),
        scrim = scrim.orDefault(stock.scrim),
    )
}

/** Text styles from Compose ones (see [toPaymentTextStyle]); the styles left out are the SDK's own. */
@Suppress("FunctionNaming", "LongParameterList")
public fun PaymentTypography(
    title: TextStyle,
    body: TextStyle,
    label: TextStyle,
    button: TextStyle,
    input: TextStyle,
    headline: TextStyle? = null,
    amount: TextStyle? = null,
    caption: TextStyle? = null,
): PaymentTypography {
    val stock = PaymentTypography.Default
    return PaymentTypography(
        title = title.toPaymentTextStyle(),
        body = body.toPaymentTextStyle(),
        label = label.toPaymentTextStyle(),
        button = button.toPaymentTextStyle(),
        input = input.toPaymentTextStyle(),
        headline = headline?.toPaymentTextStyle() ?: stock.headline,
        amount = amount?.toPaymentTextStyle() ?: stock.amount,
        caption = caption?.toPaymentTextStyle() ?: stock.caption,
    )
}

private fun Color.orDefault(default: PaymentColor): PaymentColor = if (this == Color.Unspecified) default else toPaymentColor()

private fun TextUnit.sp(unspecified: Float): Float = if (isSpecified && isSp) value else unspecified

private const val ARGB_MASK = 0xFFFFFFFFL
private const val DEFAULT_FONT_SIZE = 14f
