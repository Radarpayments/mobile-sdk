package net.payrdr.mobile.payment.sdk.qa.ui.kit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** The app's colours: ink on a plain background; [ok] and [bad] are kept for the run's outcome and field errors. */
@Immutable
internal class QaColors(
    val bg: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val fill: Color,
    val ok: Color,
    val bad: Color,
    val onOutcome: Color,
)

private val light =
    QaColors(
        bg = Color(0xFFFFFFFF),
        ink = Color(0xFF111111),
        muted = Color(0xFF6B6B6B),
        line = Color(0xFFE5E5E5),
        fill = Color(0xFFF5F5F5),
        ok = Color(0xFF1A7F4B),
        bad = Color(0xFFC62828),
        onOutcome = Color(0xFFFFFFFF),
    )

private val dark =
    QaColors(
        bg = Color(0xFF0E0E0E),
        ink = Color(0xFFF2F2F2),
        muted = Color(0xFF9A9A9A),
        line = Color(0xFF2A2A2A),
        fill = Color(0xFF1A1A1A),
        ok = Color(0xFF4CC38A),
        bad = Color(0xFFFF6B6B),
        onOutcome = Color(0xFF0E0E0E),
    )

private val LocalQaColors = staticCompositionLocalOf { light }

/** The sizes of the 4 dp grid the screens are laid out on. */
internal object QaSize {
    val screen = 16.dp
    val captionTop = 24.dp
    val captionBottom = 8.dp
    val row = 52.dp
    val rowTwoLines = 68.dp
    val gap = 8.dp
    val control = 44.dp
    val field = 52.dp
    val button = 52.dp
    val chip = 36.dp
    val appBar = 56.dp
    val line = 1.dp
}

/** The type scale: sans for words, monospace for values, identifiers, codes and the journal. */
internal object QaType {
    private val sans = FontFamily.Default
    private val mono = FontFamily.Monospace

    val title = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp)
    val body = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val bodyStrong = body.copy(fontWeight = FontWeight.SemiBold)
    val action = body.copy(fontWeight = FontWeight.Medium)
    val label = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
    val link = label.copy(textDecoration = TextDecoration.Underline)
    val caption =
        TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.08.em)
    val help = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp)
    val button = bodyStrong
    val outcome = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp)
    val value = TextStyle(fontFamily = mono, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp)
    val input = TextStyle(fontFamily = mono, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val inputSmall = input.copy(fontSize = 14.sp, lineHeight = 20.sp)
    val journal = TextStyle(fontFamily = mono, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    val journalHead = TextStyle(fontFamily = mono, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
}

/** The current colours; [QaTheme] picks the light or the dark set from the system setting. */
internal val qaColors: QaColors
    @Composable get() = LocalQaColors.current

/**
 * Provides the colours to the screens. Material's scheme gets the same colours because the popups (menus, the dialog)
 * and the text cursor read it.
 */
@Composable
internal fun QaTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) dark else light
    val scheme =
        if (colors === dark) {
            darkColorScheme(
                primary = colors.ink,
                onPrimary = colors.bg,
                background = colors.bg,
                onBackground = colors.ink,
                surface = colors.bg,
                onSurface = colors.ink,
                surfaceContainer = colors.bg,
                onSurfaceVariant = colors.muted,
                outline = colors.line,
                error = colors.bad,
            )
        } else {
            lightColorScheme(
                primary = colors.ink,
                onPrimary = colors.bg,
                background = colors.bg,
                onBackground = colors.ink,
                surface = colors.bg,
                onSurface = colors.ink,
                surfaceContainer = colors.bg,
                onSurfaceVariant = colors.muted,
                outline = colors.line,
                error = colors.bad,
            )
        }
    CompositionLocalProvider(LocalQaColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
