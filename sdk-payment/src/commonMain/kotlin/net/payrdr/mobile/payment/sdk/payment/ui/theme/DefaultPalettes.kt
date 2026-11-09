package net.payrdr.mobile.payment.sdk.payment.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Default light palette — cobalt accent on white, WCAG AA contrast on all text/background pairs.
 */
public val DefaultLightColors: PaymentColors =
    PaymentColors(
        primary = Color(0xFF1F4FD8),
        onPrimary = Color(0xFFFFFFFF),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF0D1726),
        surface = Color(0xFFF3F5F9),
        onSurface = Color(0xFF0D1726),
        error = Color(0xFFC42F27),
        onError = Color(0xFFFFFFFF),
        outline = Color(0xFFD9DFE8),
        success = Color(0xFF0B7F57),
        onSurfaceVariant = Color(0xFF4A5870),
        placeholder = Color(0xFF6F7C91),
        outlineStrong = Color(0xFFB9C2D0),
        scrim = Color(0x6B0D1726),
    )

/**
 * Default dark palette — the accent is lifted so it keeps contrast on the navy background.
 */
public val DefaultDarkColors: PaymentColors =
    PaymentColors(
        primary = Color(0xFF7D98FF),
        onPrimary = Color(0xFF0A1020),
        background = Color(0xFF111925),
        onBackground = Color(0xFFE7ECF4),
        surface = Color(0xFF192332),
        onSurface = Color(0xFFE7ECF4),
        error = Color(0xFFFF6F66),
        onError = Color(0xFF1A0605),
        outline = Color(0xFF263246),
        success = Color(0xFF3DC48F),
        onSurfaceVariant = Color(0xFFA3AEC0),
        placeholder = Color(0xFF7D899D),
        outlineStrong = Color(0xFF3A475D),
        scrim = Color(0x8C000000),
    )
