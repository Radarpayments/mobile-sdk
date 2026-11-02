package net.payrdr.mobile.payment.sdk.payment.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode

/**
 * CompositionLocal for the resolved [PaymentColors]. Inside a [PaymentThemeProvider] this is
 * always populated; outside it throws — screens must be composed under the provider.
 */
public val LocalPaymentColors: androidx.compose.runtime.ProvidableCompositionLocal<PaymentColors> =
    staticCompositionLocalOf { error("PaymentColors not provided — wrap content in PaymentThemeProvider { }") }

/**
 * CompositionLocal for the resolved [PaymentTypography].
 */
public val LocalPaymentTypography: androidx.compose.runtime.ProvidableCompositionLocal<PaymentTypography> =
    staticCompositionLocalOf { PaymentTypography.Default }

/**
 * CompositionLocal for the resolved [PaymentShapes].
 */
public val LocalPaymentShapes: androidx.compose.runtime.ProvidableCompositionLocal<PaymentShapes> =
    staticCompositionLocalOf { PaymentShapes.Default }

/**
 * Resolves [theme] to concrete [PaymentColors]/[PaymentTypography]/[PaymentShapes] triple, then
 * installs them as CompositionLocals and wraps [content] in a Material3 [MaterialTheme] whose
 * color scheme is derived from our palette.
 *
 * Use at the root of any SDK screen composition. `PaymentCheckout` applies this automatically.
 */
@OptIn(ExperimentalMaterial3Api::class) // LocalRippleConfiguration
@Composable
public fun PaymentThemeProvider(
    theme: PaymentTheme,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val colors: PaymentColors =
        when (theme) {
            PaymentTheme.System -> if (systemDark) DefaultDarkColors else DefaultLightColors
            PaymentTheme.Light -> DefaultLightColors
            PaymentTheme.Dark -> DefaultDarkColors
            is PaymentTheme.Custom -> theme.colors
        }
    val typography: PaymentTypography =
        when (theme) {
            is PaymentTheme.Custom -> theme.typography
            else -> PaymentTypography.Default
        }
    val shapes: PaymentShapes =
        when (theme) {
            is PaymentTheme.Custom -> theme.shapes
            else -> PaymentShapes.Default
        }

    val materialColorScheme =
        if (colorsAreDark(colors)) {
            darkColorScheme(
                primary = colors.primary,
                onPrimary = colors.onPrimary,
                background = colors.background,
                onBackground = colors.onBackground,
                surface = colors.surface,
                onSurface = colors.onSurface,
                error = colors.error,
                onError = colors.onError,
                outline = colors.outline,
            )
        } else {
            lightColorScheme(
                primary = colors.primary,
                onPrimary = colors.onPrimary,
                background = colors.background,
                onBackground = colors.onBackground,
                surface = colors.surface,
                onSurface = colors.onSurface,
                error = colors.error,
                onError = colors.onError,
                outline = colors.outline,
            )
        }

    CompositionLocalProvider(
        LocalPaymentColors provides colors,
        LocalPaymentTypography provides typography,
        LocalPaymentShapes provides shapes,
    ) {
        MaterialTheme(colorScheme = materialColorScheme) {
            // No Material ripple anywhere in the SDK: neither on Material components nor on plain clickables.
            CompositionLocalProvider(
                LocalRippleConfiguration provides null,
                LocalIndication provides NoIndication,
                content = content,
            )
        }
    }
}

/** Press indication that draws nothing. */
private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = object : Modifier.Node() {}

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 0
}

/**
 * Heuristic: treat a palette as dark if its background luminance is below 0.5.
 * Picks the Material color scheme and the system bar icon color.
 */
internal fun colorsAreDark(colors: PaymentColors): Boolean {
    val c = colors.background
    val luminance = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
    return luminance < 0.5f
}
