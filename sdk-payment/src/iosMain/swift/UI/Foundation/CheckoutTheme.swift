import SwiftUI
import UIKit

/// The checkout's colours, text styles and corner radii, resolved from the integrator's `PaymentTheme`.
struct CheckoutTheme {
    let colors: CheckoutColors
    let title: CheckoutTextStyle
    let body: CheckoutTextStyle
    let label: CheckoutTextStyle
    let button: CheckoutTextStyle
    let input: CheckoutTextStyle
    let headline: CheckoutTextStyle
    let amount: CheckoutTextStyle
    let caption: CheckoutTextStyle
    let fieldCorner: CGFloat
    let buttonCorner: CGFloat
    let sheetCorner: CGFloat
    let cardCorner: CGFloat

    /// Whether the palette is a dark one: decides the status bar and the keyboard appearance.
    var isDark: Bool { colors.backgroundLuminance < 0.5 }

    init(spec: ThemeSpec, systemIsDark: Bool) {
        colors = CheckoutColors(spec: spec.followsSystem && systemIsDark ? spec.dark : spec.light)
        title = CheckoutTextStyle(spec: spec.title)
        body = CheckoutTextStyle(spec: spec.body)
        label = CheckoutTextStyle(spec: spec.label)
        button = CheckoutTextStyle(spec: spec.button)
        input = CheckoutTextStyle(spec: spec.input)
        headline = CheckoutTextStyle(spec: spec.headline)
        amount = CheckoutTextStyle(spec: spec.amount)
        caption = CheckoutTextStyle(spec: spec.caption)
        fieldCorner = CGFloat(spec.fieldCorner)
        buttonCorner = CGFloat(spec.buttonCorner)
        sheetCorner = CGFloat(spec.sheetCorner)
        cardCorner = CGFloat(spec.cardCorner)
    }
}

struct CheckoutColors {
    let primary: Color
    let onPrimary: Color
    let background: Color
    let onBackground: Color
    let surface: Color
    let onSurface: Color
    let error: Color
    let onError: Color
    let outline: Color
    let success: Color
    let onSurfaceVariant: Color
    let placeholder: Color
    let outlineStrong: Color
    let scrim: Color

    /// Tints mixed from the palette, as the Android checkout mixes them.
    let primarySoft: Color
    let secondaryFill: Color
    let successSoft: Color
    let errorSoft: Color

    let backgroundLuminance: Double

    init(spec: ColorSpec) {
        let background = ARGB(spec.background)
        primary = ARGB(spec.primary).color
        onPrimary = ARGB(spec.onPrimary).color
        self.background = background.color
        onBackground = ARGB(spec.onBackground).color
        surface = ARGB(spec.surface).color
        onSurface = ARGB(spec.onSurface).color
        error = ARGB(spec.error).color
        onError = ARGB(spec.onError).color
        outline = ARGB(spec.outline).color
        success = ARGB(spec.success).color
        onSurfaceVariant = ARGB(spec.onSurfaceVariant).color
        placeholder = ARGB(spec.placeholder).color
        outlineStrong = ARGB(spec.outlineStrong).color
        scrim = ARGB(spec.scrim).color
        primarySoft = background.mixed(with: ARGB(spec.primary), 0.11).color
        secondaryFill = background.mixed(with: ARGB(spec.onBackground), 0.09).color
        successSoft = background.mixed(with: ARGB(spec.success), 0.14).color
        errorSoft = background.mixed(with: ARGB(spec.error), 0.12).color
        backgroundLuminance = 0.299 * background.red + 0.587 * background.green + 0.114 * background.blue
    }
}

/// A colour as sRGB components in 0...1, read from Kotlin's `0xAARRGGBB`.
struct ARGB {
    let alpha: Double
    let red: Double
    let green: Double
    let blue: Double

    init(_ value: Int64) {
        alpha = Double((value >> 24) & 0xFF) / 255
        red = Double((value >> 16) & 0xFF) / 255
        green = Double((value >> 8) & 0xFF) / 255
        blue = Double(value & 0xFF) / 255
    }

    private init(alpha: Double, red: Double, green: Double, blue: Double) {
        self.alpha = alpha
        self.red = red
        self.green = green
        self.blue = blue
    }

    var color: Color { Color(.sRGB, red: red, green: green, blue: blue, opacity: alpha) }

    var uiColor: UIColor { UIColor(red: red, green: green, blue: blue, alpha: alpha) }

    /// `fraction` of `other` over this colour, mixed in Oklab as Compose's `lerp` mixes colours.
    func mixed(with other: ARGB, _ fraction: Double) -> ARGB {
        func mix(_ from: Double, _ to: Double) -> Double { from + (to - from) * fraction }
        let from = oklab
        let to = other.oklab
        let rgb = ARGB.sRGB(lightness: mix(from.0, to.0), a: mix(from.1, to.1), b: mix(from.2, to.2))
        return ARGB(alpha: mix(alpha, other.alpha), red: rgb.0, green: rgb.1, blue: rgb.2)
    }

    private var oklab: (Double, Double, Double) {
        func linear(_ value: Double) -> Double {
            value <= 0.04045 ? value / 12.92 : pow((value + 0.055) / 1.055, 2.4)
        }
        let red = linear(self.red)
        let green = linear(self.green)
        let blue = linear(self.blue)
        let long = cbrt(0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue)
        let medium = cbrt(0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue)
        let short = cbrt(0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue)
        return (
            0.2104542553 * long + 0.7936177850 * medium - 0.0040720468 * short,
            1.9779984951 * long - 2.4285922050 * medium + 0.4505937099 * short,
            0.0259040371 * long + 0.7827717662 * medium - 0.8086757660 * short
        )
    }

    private static func sRGB(lightness: Double, a: Double, b: Double) -> (Double, Double, Double) {
        func encoded(_ value: Double) -> Double {
            let clamped = min(max(value, 0), 1)
            return clamped <= 0.0031308 ? clamped * 12.92 : 1.055 * pow(clamped, 1 / 2.4) - 0.055
        }
        let long = pow(lightness + 0.3963377774 * a + 0.2158037573 * b, 3)
        let medium = pow(lightness - 0.1055613458 * a - 0.0638541728 * b, 3)
        let short = pow(lightness - 0.0894841775 * a - 1.2914855480 * b, 3)
        return (
            encoded(4.0767416621 * long - 3.3077115913 * medium + 0.2309699292 * short),
            encoded(-1.2684380046 * long + 2.6097574011 * medium - 0.3413193965 * short),
            encoded(-0.0041960863 * long - 0.7034186147 * medium + 1.7076147010 * short)
        )
    }
}

struct CheckoutTextStyle {
    let size: CGFloat
    let weight: UIFont.Weight
    /// Zero when the font's own line height applies.
    let lineHeight: CGFloat
    let tracking: CGFloat
    let tabularFigures: Bool
    /// A family the app registered; nil for the system font.
    let fontFamily: String?

    init(spec: TextSpec) {
        size = CGFloat(spec.size)
        weight = CheckoutTextStyle.weight(css: Int(spec.weight))
        lineHeight = CGFloat(spec.lineHeight)
        tracking = CGFloat(spec.tracking)
        tabularFigures = spec.tabularFigures
        fontFamily = spec.fontFamily
    }

    init(
        size: CGFloat,
        weight: UIFont.Weight,
        lineHeight: CGFloat,
        tracking: CGFloat,
        tabularFigures: Bool,
        fontFamily: String? = nil
    ) {
        self.size = size
        self.weight = weight
        self.lineHeight = lineHeight
        self.tracking = tracking
        self.tabularFigures = tabularFigures
        self.fontFamily = fontFamily
    }

    func weight(_ weight: UIFont.Weight) -> CheckoutTextStyle {
        CheckoutTextStyle(
            size: size,
            weight: weight,
            lineHeight: lineHeight,
            tracking: tracking,
            tabularFigures: tabularFigures,
            fontFamily: fontFamily
        )
    }

    func tabular() -> CheckoutTextStyle {
        CheckoutTextStyle(
            size: size,
            weight: weight,
            lineHeight: lineHeight,
            tracking: tracking,
            tabularFigures: true,
            fontFamily: fontFamily
        )
    }

    /// The font at a fixed size: the Android checkout draws in fixed sizes on iOS too, and the two must match.
    var uiFont: UIFont { sized(size) }

    var font: Font { Font(uiFont) }

    /// Space to add between lines so that a line takes `lineHeight`.
    var lineSpacing: CGFloat { lineHeight > 0 ? max(lineHeight - uiFont.lineHeight, 0) : 0 }

    private static func weight(css: Int) -> UIFont.Weight {
        switch css {
        case ..<150: return .ultraLight
        case ..<250: return .thin
        case ..<350: return .light
        case ..<450: return .regular
        case ..<550: return .medium
        case ..<650: return .semibold
        case ..<750: return .bold
        case ..<850: return .heavy
        default: return .black
        }
    }
}

private struct CheckoutThemeKey: EnvironmentKey {
    // A real theme, not a trap: SwiftUI reads the old value through the key path before it writes the new one, so
    // `.environment(\.checkoutTheme, …)` itself calls the getter while nothing is set yet.
    static let defaultValue = CheckoutTheme(spec: ThemeSpec.companion.of(theme: PaymentThemeSystem.shared), systemIsDark: false)
}

extension EnvironmentValues {
    /// Set by the checkout's root.
    var checkoutTheme: CheckoutTheme {
        get { self[CheckoutThemeKey.self] }
        set { self[CheckoutThemeKey.self] = newValue }
    }
}

extension Text {
    /// Draws the text in `style`: its font, tracking and line height.
    func checkoutStyle(_ style: CheckoutTextStyle) -> some View {
        let half = max(style.lineHeight - style.uiFont.lineHeight, 0) / 2
        return font(style.font)
            .kerning(style.tracking)
            .lineSpacing(style.lineSpacing)
            .padding(.vertical, style.lineHeight > 0 ? half : 0)
    }
}
