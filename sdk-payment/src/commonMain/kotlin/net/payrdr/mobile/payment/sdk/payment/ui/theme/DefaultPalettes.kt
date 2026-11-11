package net.payrdr.mobile.payment.sdk.payment.ui.theme

/**
 * Default light palette — cobalt accent on white, WCAG AA contrast on all text/background pairs.
 */
public val DefaultLightColors: PaymentColors =
    PaymentColors(
        primary = PaymentColor(0xFF1F4FD8),
        onPrimary = PaymentColor(0xFFFFFFFF),
        background = PaymentColor(0xFFFFFFFF),
        onBackground = PaymentColor(0xFF0D1726),
        surface = PaymentColor(0xFFF3F5F9),
        onSurface = PaymentColor(0xFF0D1726),
        error = PaymentColor(0xFFC42F27),
        onError = PaymentColor(0xFFFFFFFF),
        outline = PaymentColor(0xFFD9DFE8),
        success = PaymentColor(0xFF0B7F57),
        onSurfaceVariant = PaymentColor(0xFF4A5870),
        placeholder = PaymentColor(0xFF6F7C91),
        outlineStrong = PaymentColor(0xFFB9C2D0),
        scrim = PaymentColor(0x6B0D1726),
    )

/**
 * Default dark palette — the accent is lifted so it keeps contrast on the navy background.
 */
public val DefaultDarkColors: PaymentColors =
    PaymentColors(
        primary = PaymentColor(0xFF7D98FF),
        onPrimary = PaymentColor(0xFF0A1020),
        background = PaymentColor(0xFF111925),
        onBackground = PaymentColor(0xFFE7ECF4),
        surface = PaymentColor(0xFF192332),
        onSurface = PaymentColor(0xFFE7ECF4),
        error = PaymentColor(0xFFFF6F66),
        onError = PaymentColor(0xFF1A0605),
        outline = PaymentColor(0xFF263246),
        success = PaymentColor(0xFF3DC48F),
        onSurfaceVariant = PaymentColor(0xFFA3AEC0),
        placeholder = PaymentColor(0xFF7D899D),
        outlineStrong = PaymentColor(0xFF3A475D),
        scrim = PaymentColor(0x8C000000),
    )
