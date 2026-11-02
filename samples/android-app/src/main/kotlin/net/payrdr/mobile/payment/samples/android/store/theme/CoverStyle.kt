package net.payrdr.mobile.payment.samples.android.store.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.store.invest.InvestColors
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.sdk.payment.ui.theme.DefaultDarkColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.DefaultLightColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme

/**
 * The brand of a demo business: its colours, corner radius and fonts. The store screens use all of it;
 * the SDK sheet takes the colours and corners through [paymentTheme].
 *
 * @property fieldSurface background of input fields and cards inside the SDK sheet.
 * @property pillButtons buttons are fully rounded (the coffee shop) instead of using [radius].
 */
data class CoverStyle(
    val id: CoverId,
    val dark: Boolean,
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val surface: Color,
    val fieldSurface: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val accent: Color,
    val radius: Dp,
    val pillButtons: Boolean,
    val headingFont: FontFamily,
    val bodyFont: FontFamily,
    val accentFont: FontFamily?,
)

/** Brand of [id], from the design tokens picked with the ui-ux-pro-max design system. */
fun coverStyle(id: CoverId): CoverStyle =
    when (id) {
        CoverId.FURNITURE ->
            CoverStyle(
                id = id,
                dark = false,
                primary = Color(0xFF1C1917),
                onPrimary = Color(0xFFFFFFFF),
                background = Color(0xFFFAFAF9),
                surface = Color(0xFFFFFFFF),
                fieldSurface = Color(0xFFF5F5F4),
                text = Color(0xFF0C0A09),
                muted = Color(0xFF57534E),
                border = Color(0xFFD6D3D1),
                accent = Color(0xFFA16207),
                radius = 12.dp,
                pillButtons = false,
                headingFont = StoreFonts.cormorant,
                bodyFont = StoreFonts.montserrat,
                accentFont = null,
            )
        CoverId.COFFEE ->
            CoverStyle(
                id = id,
                dark = true,
                primary = Color(0xFFF3D9B1),
                onPrimary = Color(0xFF17110E),
                background = Color(0xFF17110E),
                surface = Color(0xFF261C17),
                fieldSurface = Color(0xFF2E231E),
                text = Color(0xFFFEF3C7),
                muted = Color(0xFFD6C3A5),
                border = Color(0xFF5B4A40),
                accent = Color(0xFFF3D9B1),
                radius = 18.dp,
                pillButtons = true,
                headingFont = StoreFonts.nunito,
                bodyFont = StoreFonts.nunito,
                accentFont = null,
            )
        CoverId.TICKETS ->
            CoverStyle(
                id = id,
                dark = false,
                primary = Color(0xFFEA580C),
                onPrimary = Color(0xFF000000),
                background = Color(0xFFFFFFFF),
                surface = Color(0xFFFFF7ED),
                fieldSurface = Color(0xFFF8FAFC),
                text = Color(0xFF0F172A),
                muted = Color(0xFF64748B),
                border = Color(0xFFE2E8F0),
                accent = Color(0xFFEA580C),
                radius = 14.dp,
                pillButtons = false,
                headingFont = StoreFonts.inter,
                bodyFont = StoreFonts.inter,
                accentFont = StoreFonts.playfairItalic,
            )
        CoverId.INVEST ->
            CoverStyle(
                id = id,
                dark = true,
                primary = InvestColors.accent,
                onPrimary = InvestColors.onAccent,
                background = InvestColors.surface,
                surface = InvestColors.surface,
                fieldSurface = InvestColors.surface2,
                text = InvestColors.text,
                muted = InvestColors.muted,
                border = InvestColors.fieldBorder,
                accent = InvestColors.accent,
                radius = 12.dp,
                pillButtons = false,
                headingFont = StoreFonts.plex,
                bodyFont = StoreFonts.plex,
                accentFont = null,
            )
    }

/** The SDK sheet in this brand: colours and corners; typography stays the SDK's own. */
fun CoverStyle.paymentTheme(): PaymentTheme.Custom {
    val stock = if (dark) DefaultDarkColors else DefaultLightColors
    val corners = RoundedCornerShape(radius)
    return PaymentTheme.Custom(
        colors =
            PaymentColors(
                primary = primary,
                onPrimary = onPrimary,
                background = background,
                onBackground = text,
                surface = fieldSurface,
                onSurface = text,
                error = stock.error,
                onError = stock.onError,
                outline = border,
                success = stock.success,
                onSurfaceVariant = muted,
            ),
        shapes =
            PaymentShapes(
                field = corners,
                button = if (pillButtons) RoundedCornerShape(percent = 50) else corners,
                sheet = PaymentShapes.Default.sheet,
                card = corners,
            ),
    )
}

/** Material theme and system bar icons of cover [id] for the store screens. */
@Composable
fun CoverTheme(
    id: CoverId,
    content: @Composable () -> Unit,
) {
    val style = coverStyle(id)
    val scheme =
        if (style.dark) {
            darkColorScheme(
                primary = style.primary,
                onPrimary = style.onPrimary,
                background = style.background,
                onBackground = style.text,
                surface = style.background,
                onSurface = style.text,
                surfaceVariant = style.surface,
                onSurfaceVariant = style.muted,
                outline = style.border,
            )
        } else {
            lightColorScheme(
                primary = style.primary,
                onPrimary = style.onPrimary,
                background = style.background,
                onBackground = style.text,
                surface = style.background,
                onSurface = style.text,
                surfaceVariant = style.surface,
                onSurfaceVariant = style.muted,
                outline = style.border,
            )
        }
    SystemBarIcons(lightBackground = !style.dark)
    MaterialTheme(colorScheme = scheme, typography = typographyOf(style.bodyFont), content = content)
}

/** Dark status and navigation bar icons on a light screen, light icons on a dark one. */
@Composable
fun SystemBarIcons(lightBackground: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBackground
            isAppearanceLightNavigationBars = lightBackground
        }
    }
}

private fun typographyOf(body: FontFamily): Typography {
    val base = Typography()

    fun TextStyle.withBody() = copy(fontFamily = body)
    return base.copy(
        bodyLarge = base.bodyLarge.withBody(),
        bodyMedium = base.bodyMedium.withBody(),
        bodySmall = base.bodySmall.withBody(),
        labelLarge = base.labelLarge.withBody(),
        labelMedium = base.labelMedium.withBody(),
        labelSmall = base.labelSmall.withBody(),
        titleLarge = base.titleLarge.withBody(),
        titleMedium = base.titleMedium.withBody(),
        titleSmall = base.titleSmall.withBody(),
    )
}

/** Variable Google Fonts (OFL); each weight is a variation of one file. */
@OptIn(ExperimentalTextApi::class)
internal object StoreFonts {
    val cormorant = variable(R.font.cormorant, 500, 600, 700)
    val montserrat = variable(R.font.montserrat, 400, 500, 600, 700)
    val nunito = variable(R.font.nunito, 400, 600, 700, 800)
    val inter = variable(R.font.inter, 400, 500, 600, 700, 800)
    val playfairItalic = variable(R.font.playfair_display_italic, 600, style = FontStyle.Italic)
    val plex = variable(R.font.ibm_plex_sans, 400, 500, 600, 700)

    private fun variable(
        res: Int,
        vararg weights: Int,
        style: FontStyle = FontStyle.Normal,
    ) = FontFamily(
        weights.map { w ->
            Font(res, FontWeight(w), style, variationSettings = FontVariation.Settings(FontVariation.weight(w)))
        },
    )
}
