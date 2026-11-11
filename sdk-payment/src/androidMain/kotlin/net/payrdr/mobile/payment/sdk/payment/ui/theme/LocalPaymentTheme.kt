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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode

/**
 * CompositionLocal for the resolved [ThemeColors]. Inside a [PaymentThemeProvider] this is
 * always populated; outside it throws — screens must be composed under the provider.
 */
internal val LocalPaymentColors: androidx.compose.runtime.ProvidableCompositionLocal<ThemeColors> =
    staticCompositionLocalOf { error("PaymentColors not provided — wrap content in PaymentThemeProvider { }") }

/**
 * CompositionLocal for the resolved [ThemeTypography].
 */
internal val LocalPaymentTypography: androidx.compose.runtime.ProvidableCompositionLocal<ThemeTypography> =
    staticCompositionLocalOf { ThemeTypography.Default }

/**
 * CompositionLocal for the resolved [ThemeShapes].
 */
internal val LocalPaymentShapes: androidx.compose.runtime.ProvidableCompositionLocal<ThemeShapes> =
    staticCompositionLocalOf { ThemeShapes.Default }

/**
 * Resolves [theme] to concrete [PaymentColors]/[PaymentTypography]/[PaymentShapes] triple, then
 * installs them, in Compose types, as CompositionLocals and wraps [content] in a Material3 [MaterialTheme] whose
 * color scheme is derived from our palette.
 *
 * Use at the root of any SDK screen composition. `PaymentCheckout` applies this automatically.
 */
@OptIn(ExperimentalMaterial3Api::class) // LocalRippleConfiguration
@Composable
internal fun PaymentThemeProvider(
    theme: PaymentTheme,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val palette: PaymentColors =
        when (theme) {
            PaymentTheme.System -> if (systemDark) DefaultDarkColors else DefaultLightColors
            PaymentTheme.Light -> DefaultLightColors
            PaymentTheme.Dark -> DefaultDarkColors
            is PaymentTheme.Custom -> theme.colors
        }
    val custom = theme as? PaymentTheme.Custom
    val colors = remember(palette) { ThemeColors(palette) }
    val typography = remember(custom?.typography) { custom?.typography?.let(::ThemeTypography) ?: ThemeTypography.Default }
    val shapes = remember(custom?.shapes) { custom?.shapes?.let(::ThemeShapes) ?: ThemeShapes.Default }

    val materialColorScheme =
        if (palette.isDark) {
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

private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = object : Modifier.Node() {}

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 0
}

/** The palette is dark: picks the Material color scheme and the system bar icon color. */
internal fun colorsAreDark(colors: ThemeColors): Boolean = colors.palette.isDark
