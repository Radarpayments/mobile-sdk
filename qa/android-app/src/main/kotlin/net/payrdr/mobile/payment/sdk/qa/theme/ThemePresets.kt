package net.payrdr.mobile.payment.sdk.qa.theme

import androidx.compose.ui.text.font.FontFamily
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentColor
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentCorners
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentFontFamily
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTextStyle
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTypography
import net.payrdr.mobile.payment.sdk.qa.config.ThemePreset

private const val LARGE_TEXT_SCALE = 1.5f

private val BASE_COLORS =
    PaymentColors(
        primary = PaymentColor(0xFF1F5FFF),
        onPrimary = PaymentColor(0xFFFFFFFF),
        background = PaymentColor(0xFFFFFFFF),
        onBackground = PaymentColor(0xFF14161A),
        surface = PaymentColor(0xFFF3F4F6),
        onSurface = PaymentColor(0xFF14161A),
        error = PaymentColor(0xFFD92D20),
        onError = PaymentColor(0xFFFFFFFF),
        outline = PaymentColor(0xFFD0D5DD),
        success = PaymentColor(0xFF12B76A),
    )

private val CONTRAST_COLORS =
    PaymentColors(
        primary = PaymentColor(0xFF000000),
        onPrimary = PaymentColor(0xFFFFFFFF),
        background = PaymentColor(0xFFFFFFFF),
        onBackground = PaymentColor(0xFF000000),
        surface = PaymentColor(0xFFFFFFFF),
        onSurface = PaymentColor(0xFF000000),
        error = PaymentColor(0xFFB00020),
        onError = PaymentColor(0xFFFFFFFF),
        outline = PaymentColor(0xFF000000),
        success = PaymentColor(0xFF006400),
        onSurfaceVariant = PaymentColor(0xFF000000),
        placeholder = PaymentColor(0xFF595959),
        outlineStrong = PaymentColor(0xFF000000),
        scrim = PaymentColor(0xCC000000),
    )

// Every token gets a colour no other token has, so a wrong token on screen can be named from the legend.
private val DEBUG_COLORS =
    PaymentColors(
        primary = PaymentColor(0xFFFF00FF),
        onPrimary = PaymentColor(0xFF00FF00),
        background = PaymentColor(0xFFFFFF00),
        onBackground = PaymentColor(0xFF0000FF),
        surface = PaymentColor(0xFF00FFFF),
        onSurface = PaymentColor(0xFFFF0000),
        error = PaymentColor(0xFFFF8000),
        onError = PaymentColor(0xFF800080),
        outline = PaymentColor(0xFF008000),
        success = PaymentColor(0xFF0080FF),
        onSurfaceVariant = PaymentColor(0xFF804000),
        placeholder = PaymentColor(0xFF808080),
        outlineStrong = PaymentColor(0xFF000000),
        scrim = PaymentColor(0x80FF0080),
    )

/** The tokens of the debug preset in [PaymentColors] order, named as in the SDK, for the legend on screen. */
internal val DEBUG_LEGEND: List<Pair<String, PaymentColor>> =
    with(DEBUG_COLORS) {
        listOf(
            "primary" to primary,
            "onPrimary" to onPrimary,
            "background" to background,
            "onBackground" to onBackground,
            "surface" to surface,
            "onSurface" to onSurface,
            "error" to error,
            "onError" to onError,
            "outline" to outline,
            "success" to success,
            "onSurfaceVariant" to onSurfaceVariant,
            "placeholder" to placeholder,
            "outlineStrong" to outlineStrong,
            "scrim" to scrim,
        )
    }

/** The SDK theme QA picked: the SDK's own three themes, or a custom theme that changes one aspect. */
internal fun ThemePreset.paymentTheme(): PaymentTheme =
    when (this) {
        ThemePreset.SYSTEM -> PaymentTheme.System
        ThemePreset.LIGHT -> PaymentTheme.Light
        ThemePreset.DARK -> PaymentTheme.Dark
        ThemePreset.CONTRAST -> PaymentTheme.Custom(colors = CONTRAST_COLORS)
        ThemePreset.ROUNDED -> PaymentTheme.Custom(colors = BASE_COLORS, shapes = corners(PaymentCorners.Pill))
        ThemePreset.SQUARE -> PaymentTheme.Custom(colors = BASE_COLORS, shapes = corners(PaymentCorners.Radius(0f)))
        ThemePreset.LARGE_TEXT -> PaymentTheme.Custom(colors = BASE_COLORS, typography = largeTypography())
        ThemePreset.DEBUG -> PaymentTheme.Custom(colors = DEBUG_COLORS)
    }

private fun corners(corners: PaymentCorners) = PaymentShapes(field = corners, button = corners, sheet = corners, card = corners)

private fun largeTypography(): PaymentTypography {
    val serif = PaymentFontFamily(FontFamily.Serif)

    fun PaymentTextStyle.large() = copy(size = size * LARGE_TEXT_SCALE, lineHeight = lineHeight * LARGE_TEXT_SCALE, fontFamily = serif)
    return with(PaymentTypography.Default) {
        PaymentTypography(
            title = title.large(),
            body = body.large(),
            label = label.large(),
            button = button.large(),
            input = input.large(),
            headline = headline.large(),
            amount = amount.large(),
            caption = caption.large(),
        )
    }
}
