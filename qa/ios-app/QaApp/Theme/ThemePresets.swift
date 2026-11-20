import RadarpaymentsSDKPayment

private let largeTextScale: Float = 1.5

// A family name iOS ships, so no font file is bundled; README "Theme presets" names it for iOS.
private let serifFamily = "Georgia"

// The four optional tokens repeat the SDK's own defaults: Swift sees the Kotlin initializer without its defaults.
private let baseColors = paletteWithDefaults(
    primary: 0xFF1F_5FFF,
    onPrimary: 0xFFFF_FFFF,
    background: 0xFFFF_FFFF,
    onBackground: 0xFF14_161A,
    surface: 0xFFF3_F4F6,
    onSurface: 0xFF14_161A,
    error: 0xFFD9_2D20,
    onError: 0xFFFF_FFFF,
    outline: 0xFFD0_D5DD,
    success: 0xFF12_B76A
)

private let contrastColors = palette([
    0xFF00_0000, 0xFFFF_FFFF, 0xFFFF_FFFF, 0xFF00_0000, 0xFFFF_FFFF, 0xFF00_0000, 0xFFB0_0020,
    0xFFFF_FFFF, 0xFF00_0000, 0xFF00_6400, 0xFF00_0000, 0xFF59_5959, 0xFF00_0000, 0xCC00_0000,
])

// Fourteen distinct colours so a token can be told from the screen.
private let debugColors = palette([
    0xFFFF_00FF, 0xFF00_FF00, 0xFFFF_FF00, 0xFF00_00FF, 0xFF00_FFFF, 0xFFFF_0000, 0xFFFF_8000,
    0xFF80_0080, 0xFF00_8000, 0xFF00_80FF, 0xFF80_4000, 0xFF80_8080, 0xFF00_0000, 0x80FF_0080,
])

/// The `debug` palette by token name, in the order of `PaymentColors`' initializer, for the legend in the Theme group.
let debugLegend: [(token: String, color: PaymentColor)] = [
    ("primary", debugColors.primary),
    ("onPrimary", debugColors.onPrimary),
    ("background", debugColors.background),
    ("onBackground", debugColors.onBackground),
    ("surface", debugColors.surface),
    ("onSurface", debugColors.onSurface),
    ("error", debugColors.error),
    ("onError", debugColors.onError),
    ("outline", debugColors.outline),
    ("success", debugColors.success),
    ("onSurfaceVariant", debugColors.onSurfaceVariant),
    ("placeholder", debugColors.placeholder),
    ("outlineStrong", debugColors.outlineStrong),
    ("scrim", debugColors.scrim),
]

extension ThemePreset {
    /// The SDK theme QA picked: the SDK's own three themes, or a custom theme that changes one aspect.
    func paymentTheme() -> any PaymentTheme {
        switch self {
        case .system: PaymentThemeSystem.shared
        case .light: PaymentThemeLight.shared
        case .dark: PaymentThemeDark.shared
        case .contrast: custom(colors: contrastColors)
        case .rounded: custom(colors: baseColors, shapes: corners(PaymentCornersPill.shared))
        case .square: custom(colors: baseColors, shapes: corners(PaymentCornersRadius(points: 0)))
        case .largeText: custom(colors: baseColors, typography: largeTypography())
        case .debug: custom(colors: debugColors)
        }
    }
}

private func custom(
    colors: PaymentColors,
    typography: PaymentTypography = PaymentTypography.companion.Default,
    shapes: PaymentShapes = PaymentShapes.companion.Default
) -> PaymentThemeCustom {
    PaymentThemeCustom(colors: colors, typography: typography, shapes: shapes)
}

private func corners(_ corners: any PaymentCorners) -> PaymentShapes {
    PaymentShapes(field: corners, button: corners, sheet: corners, card: corners)
}

private func largeTypography() -> PaymentTypography {
    let serif = PaymentFontFamily(name: serifFamily)
    func large(_ style: PaymentTextStyle) -> PaymentTextStyle {
        style.doCopy(
            size: style.size * largeTextScale,
            weight: style.weight,
            lineHeight: style.lineHeight * largeTextScale,
            letterSpacing: style.letterSpacing,
            tabularFigures: style.tabularFigures,
            fontFamily: serif
        )
    }
    let base = PaymentTypography.companion.Default
    return PaymentTypography(
        title: large(base.title),
        body: large(base.body),
        label: large(base.label),
        button: large(base.button),
        input: large(base.input),
        headline: large(base.headline),
        amount: large(base.amount),
        caption: large(base.caption)
    )
}

private func paletteWithDefaults(
    primary: Int64,
    onPrimary: Int64,
    background: Int64,
    onBackground: Int64,
    surface: Int64,
    onSurface: Int64,
    error: Int64,
    onError: Int64,
    outline: Int64,
    success: Int64
) -> PaymentColors {
    let onBackground = PaymentColor(argb: onBackground)
    let outline = PaymentColor(argb: outline)
    return PaymentColors(
        primary: PaymentColor(argb: primary),
        onPrimary: PaymentColor(argb: onPrimary),
        background: PaymentColor(argb: background),
        onBackground: onBackground,
        surface: PaymentColor(argb: surface),
        onSurface: PaymentColor(argb: onSurface),
        error: PaymentColor(argb: error),
        onError: PaymentColor(argb: onError),
        outline: outline,
        success: PaymentColor(argb: success),
        onSurfaceVariant: onBackground.withAlpha(alpha: 0.7),
        placeholder: onBackground.withAlpha(alpha: 0.5),
        outlineStrong: outline,
        scrim: PaymentColor.companion.Black.withAlpha(alpha: 0.45)
    )
}

/// A palette of all fourteen tokens in the order of `PaymentColors`' initializer.
private func palette(_ argb: [Int64]) -> PaymentColors {
    precondition(argb.count == 14, "a palette has fourteen tokens")
    let color = argb.map { PaymentColor(argb: $0) }
    return PaymentColors(
        primary: color[0],
        onPrimary: color[1],
        background: color[2],
        onBackground: color[3],
        surface: color[4],
        onSurface: color[5],
        error: color[6],
        onError: color[7],
        outline: color[8],
        success: color[9],
        onSurfaceVariant: color[10],
        placeholder: color[11],
        outlineStrong: color[12],
        scrim: color[13]
    )
}
