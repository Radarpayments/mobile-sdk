package net.payrdr.mobile.payment.sdk.payment.presentation

import net.payrdr.mobile.payment.sdk.payment.ui.theme.DefaultDarkColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.DefaultLightColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentCorners
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTextStyle
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.theme.sheetRadius

/**
 * A [PaymentTheme] as plain numbers for the SwiftUI checkout: colours as ARGB, sizes in points.
 *
 * @property followsSystem the palette is the SDK's own and switches with the device's dark mode; [light] and [dark]
 * differ only then
 */
@PayrdrUiApi
@Suppress("LongParameterList")
public class ThemeSpec internal constructor(
    public val followsSystem: Boolean,
    public val light: ColorSpec,
    public val dark: ColorSpec,
    public val title: TextSpec,
    public val body: TextSpec,
    public val label: TextSpec,
    public val button: TextSpec,
    public val input: TextSpec,
    public val headline: TextSpec,
    public val amount: TextSpec,
    public val caption: TextSpec,
    public val fieldCorner: Double,
    public val buttonCorner: Double,
    public val sheetCorner: Double,
    public val cardCorner: Double,
) {
    /** Builds a spec from a theme. */
    public companion object {
        /** What [theme] looks like, resolved the way the Compose checkout resolves it. */
        public fun of(theme: PaymentTheme): ThemeSpec {
            val custom = theme as? PaymentTheme.Custom
            val typography = custom?.typography ?: PaymentTypography.Default
            val shapes = custom?.shapes ?: PaymentShapes.Default
            return ThemeSpec(
                followsSystem = theme == PaymentTheme.System,
                light = (custom?.colors ?: if (theme == PaymentTheme.Dark) DefaultDarkColors else DefaultLightColors).spec(),
                dark = (custom?.colors ?: if (theme == PaymentTheme.Light) DefaultLightColors else DefaultDarkColors).spec(),
                title = typography.title.spec(),
                body = typography.body.spec(),
                label = typography.label.spec(),
                button = typography.button.spec(),
                input = typography.input.spec(),
                headline = typography.headline.spec(),
                amount = typography.amount.spec(),
                caption = typography.caption.spec(),
                fieldCorner = shapes.field.points(),
                buttonCorner = shapes.button.points(),
                sheetCorner = shapes.sheetRadius.toDouble(),
                cardCorner = shapes.card.points(),
            )
        }
    }
}

/** The palette as `0xAARRGGBB` values. */
@PayrdrUiApi
@Suppress("LongParameterList")
public class ColorSpec internal constructor(
    public val primary: Long,
    public val onPrimary: Long,
    public val background: Long,
    public val onBackground: Long,
    public val surface: Long,
    public val onSurface: Long,
    public val error: Long,
    public val onError: Long,
    public val outline: Long,
    public val success: Long,
    public val onSurfaceVariant: Long,
    public val placeholder: Long,
    public val outlineStrong: Long,
    public val scrim: Long,
)

/**
 * A text style in points.
 *
 * @property weight the CSS weight, 1–1000
 * @property lineHeight `0.0` when the font's own line height applies
 * @property tabularFigures digits of one width
 * @property fontFamily the family name the app registered, `null` for the system font
 */
@PayrdrUiApi
public class TextSpec internal constructor(
    public val size: Double,
    public val weight: Int,
    public val lineHeight: Double,
    public val tracking: Double,
    public val tabularFigures: Boolean,
    public val fontFamily: String?,
)

private fun PaymentColors.spec(): ColorSpec =
    ColorSpec(
        primary = primary.argb,
        onPrimary = onPrimary.argb,
        background = background.argb,
        onBackground = onBackground.argb,
        surface = surface.argb,
        onSurface = onSurface.argb,
        error = error.argb,
        onError = onError.argb,
        outline = outline.argb,
        success = success.argb,
        onSurfaceVariant = onSurfaceVariant.argb,
        placeholder = placeholder.argb,
        outlineStrong = outlineStrong.argb,
        scrim = scrim.argb,
    )

private fun PaymentTextStyle.spec(): TextSpec =
    TextSpec(
        size = size.toDouble(),
        weight = weight,
        lineHeight = lineHeight.toDouble(),
        tracking = letterSpacing.toDouble(),
        tabularFigures = tabularFigures,
        fontFamily = fontFamily?.name,
    )

/** A pill's radius: far above half of any surface's height, which is where SwiftUI and PassKit clamp it. */
private fun PaymentCorners.points(): Double =
    when (this) {
        is PaymentCorners.Radius -> points.toDouble()
        PaymentCorners.Pill -> PILL_CORNER
    }

private const val PILL_CORNER = 500.0
