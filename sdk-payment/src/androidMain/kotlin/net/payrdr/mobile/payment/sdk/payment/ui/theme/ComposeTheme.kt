package net.payrdr.mobile.payment.sdk.payment.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** [PaymentColors] in Compose colours, as the Compose checkout draws them. */
@Immutable
internal class ThemeColors(
    val palette: PaymentColors,
) {
    val primary: Color = palette.primary.toComposeColor()
    val onPrimary: Color = palette.onPrimary.toComposeColor()
    val background: Color = palette.background.toComposeColor()
    val onBackground: Color = palette.onBackground.toComposeColor()
    val surface: Color = palette.surface.toComposeColor()
    val onSurface: Color = palette.onSurface.toComposeColor()
    val error: Color = palette.error.toComposeColor()
    val onError: Color = palette.onError.toComposeColor()
    val outline: Color = palette.outline.toComposeColor()
    val success: Color = palette.success.toComposeColor()
    val onSurfaceVariant: Color = palette.onSurfaceVariant.toComposeColor()
    val placeholder: Color = palette.placeholder.toComposeColor()
    val outlineStrong: Color = palette.outlineStrong.toComposeColor()
    val scrim: Color = palette.scrim.toComposeColor()

    /** [primary] tinted onto the background — selected payment-method fill, icon plates. */
    val primarySoft: Color = lerp(background, primary, 0.11f)

    /**
     * Fill of secondary buttons: the text colour tinted onto the background, so the button stands out from the sheet in
     * any merchant palette (a [surface] close to the background would leave it all but invisible).
     */
    val secondaryFill: Color = lerp(background, onBackground, 0.09f)

    /** Success tint for the result icon plate. */
    val successSoft: Color = lerp(background, success, 0.14f)

    /** Error tint for the result icon plate and the action-code chip. */
    val errorSoft: Color = lerp(background, error, 0.12f)
}

/** [PaymentTypography] in Compose text styles. */
@Immutable
internal class ThemeTypography(
    typography: PaymentTypography,
) {
    val title: TextStyle = typography.title.toTextStyle()
    val body: TextStyle = typography.body.toTextStyle()
    val label: TextStyle = typography.label.toTextStyle()
    val button: TextStyle = typography.button.toTextStyle()
    val input: TextStyle = typography.input.toTextStyle()
    val headline: TextStyle = typography.headline.toTextStyle()
    val amount: TextStyle = typography.amount.toTextStyle()
    val caption: TextStyle = typography.caption.toTextStyle()

    companion object {
        val Default: ThemeTypography = ThemeTypography(PaymentTypography.Default)
    }
}

/** [PaymentShapes] in Compose shapes. */
@Immutable
internal class ThemeShapes(
    shapes: PaymentShapes,
) {
    val field: Shape = shapes.field.toShape()
    val button: Shape = shapes.button.toShape()
    val sheet: Shape = RoundedCornerShape(topStart = shapes.sheetRadius.dp, topEnd = shapes.sheetRadius.dp)
    val card: Shape = shapes.card.toShape()

    companion object {
        val Default: ThemeShapes = ThemeShapes(PaymentShapes.Default)
    }
}

internal fun PaymentColor.toComposeColor(): Color = Color(argb)

/** Unset line height and letter spacing stay unspecified, so the font's own apply as they did with `TextStyle`. */
internal fun PaymentTextStyle.toTextStyle(): TextStyle =
    TextStyle(
        fontSize = size.sp,
        fontWeight = FontWeight(weight),
        fontFamily = fontFamily?.composeFontFamily(),
        lineHeight = if (lineHeight > 0f) lineHeight.sp else TextUnit.Unspecified,
        letterSpacing = if (letterSpacing != 0f) letterSpacing.sp else TextUnit.Unspecified,
        fontFeatureSettings = if (tabularFigures) TABULAR else null,
    )

internal fun PaymentCorners.toShape(): Shape =
    when (this) {
        is PaymentCorners.Radius -> RoundedCornerShape(points.dp)
        PaymentCorners.Pill -> RoundedCornerShape(percent = PILL_PERCENT)
    }

internal const val TABULAR: String = "tnum"

/** [this] with digits of one width. */
internal fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR)

private const val PILL_PERCENT = 50
