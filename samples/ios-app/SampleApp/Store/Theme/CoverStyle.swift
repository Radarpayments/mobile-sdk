import SwiftUI
import sdk_payment_scanner

/// The brand of a demo business: its colours, corner radius and fonts. The store screens use all of it;
/// the SDK sheet takes the colours and corners through `paymentTheme()`.
struct CoverStyle {
    let id: CoverId
    let dark: Bool
    let primaryArgb: UInt32
    let onPrimaryArgb: UInt32
    let backgroundArgb: UInt32
    let surfaceArgb: UInt32
    /// Background of fields inside the SDK sheet.
    let fieldSurfaceArgb: UInt32
    let textArgb: UInt32
    let mutedArgb: UInt32
    let borderArgb: UInt32
    let accentArgb: UInt32
    let radius: CGFloat
    /// Fully rounded buttons (the coffee shop).
    let pillButtons: Bool
    let headingFont: String
    let bodyFont: String
    let accentFont: String?

    var primary: Color { Color(argb: primaryArgb) }
    var onPrimary: Color { Color(argb: onPrimaryArgb) }
    var background: Color { Color(argb: backgroundArgb) }
    var surface: Color { Color(argb: surfaceArgb) }
    var text: Color { Color(argb: textArgb) }
    var muted: Color { Color(argb: mutedArgb) }
    var border: Color { Color(argb: borderArgb) }
    var accent: Color { Color(argb: accentArgb) }

    /// A heading in the brand font.
    func heading(_ size: CGFloat, _ weight: Font.Weight = .bold) -> Font { .custom(headingFont, size: size).weight(weight) }
    /// Body text in the brand font.
    func body(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font { .custom(bodyFont, size: size).weight(weight) }
    var buttonShape: AnyShape { pillButtons ? AnyShape(Capsule()) : AnyShape(RoundedRectangle(cornerRadius: radius)) }
}

/// Brand of `id`, from the design tokens picked with the ui-ux-pro-max design system (the same as Android).
func coverStyle(_ id: CoverId) -> CoverStyle {
    switch id {
    case .furniture:
        return CoverStyle(id: id, dark: false, primaryArgb: 0xFF1C_1917, onPrimaryArgb: 0xFFFF_FFFF, backgroundArgb: 0xFFFA_FAF9,
                          surfaceArgb: 0xFFFF_FFFF, fieldSurfaceArgb: 0xFFF5_F5F4, textArgb: 0xFF0C_0A09, mutedArgb: 0xFF57_534E,
                          borderArgb: 0xFFD6_D3D1, accentArgb: 0xFFA1_6207, radius: 12, pillButtons: false,
                          headingFont: "Cormorant", bodyFont: "Montserrat", accentFont: nil)
    case .coffee:
        return CoverStyle(id: id, dark: true, primaryArgb: 0xFFF3_D9B1, onPrimaryArgb: 0xFF17_110E, backgroundArgb: 0xFF17_110E,
                          surfaceArgb: 0xFF26_1C17, fieldSurfaceArgb: 0xFF2E_231E, textArgb: 0xFFFE_F3C7, mutedArgb: 0xFFD6_C3A5,
                          borderArgb: 0xFF5B_4A40, accentArgb: 0xFFF3_D9B1, radius: 18, pillButtons: true,
                          headingFont: "Nunito", bodyFont: "Nunito", accentFont: nil)
    case .tickets:
        return CoverStyle(id: id, dark: false, primaryArgb: 0xFFEA_580C, onPrimaryArgb: 0xFF00_0000, backgroundArgb: 0xFFFF_FFFF,
                          surfaceArgb: 0xFFFF_F7ED, fieldSurfaceArgb: 0xFFF8_FAFC, textArgb: 0xFF0F_172A, mutedArgb: 0xFF64_748B,
                          borderArgb: 0xFFE2_E8F0, accentArgb: 0xFFEA_580C, radius: 14, pillButtons: false,
                          headingFont: "Inter", bodyFont: "Inter", accentFont: "Playfair Display")
    case .invest:
        return CoverStyle(id: id, dark: true, primaryArgb: InvestColors.accent, onPrimaryArgb: InvestColors.onAccent,
                          backgroundArgb: InvestColors.surface, surfaceArgb: InvestColors.surface, fieldSurfaceArgb: InvestColors.surface2,
                          textArgb: InvestColors.text, mutedArgb: InvestColors.muted, borderArgb: InvestColors.fieldBorder,
                          accentArgb: InvestColors.accent, radius: 12, pillButtons: false,
                          headingFont: "IBM Plex Sans", bodyFont: "IBM Plex Sans", accentFont: nil)
    }
}

extension PaymentColor {
    /// A colour from a 0xAARRGGBB value.
    convenience init(_ argb: UInt32) { self.init(argb: Int64(argb)) }
}

extension CoverStyle {
    /// The SDK sheet in this brand: colours and corners; typography stays the SDK's own.
    func paymentTheme() -> PaymentThemeCustom {
        let stock = dark ? DefaultPalettesKt.DefaultDarkColors : DefaultPalettesKt.DefaultLightColors
        let colors = PaymentColors(
            primary: PaymentColor(primaryArgb),
            onPrimary: PaymentColor(onPrimaryArgb),
            background: PaymentColor(backgroundArgb),
            onBackground: PaymentColor(textArgb),
            surface: PaymentColor(fieldSurfaceArgb),
            onSurface: PaymentColor(textArgb),
            error: stock.error,
            onError: stock.onError,
            outline: PaymentColor(borderArgb),
            success: stock.success,
            onSurfaceVariant: PaymentColor(mutedArgb),
            // The cover's text at half strength, as the SDK derives it by default; the stock one is blue-grey.
            placeholder: PaymentColor(textArgb).withAlpha(alpha: 0.5),
            outlineStrong: PaymentColor(borderArgb),
            scrim: stock.scrim
        )
        let corners = PaymentCornersRadius(points: Float(radius))
        let shapes = PaymentShapes(
            field: corners,
            button: pillButtons ? PaymentCornersPill.shared : corners,
            sheet: PaymentShapes.companion.Default.sheet,
            card: corners
        )
        return PaymentThemeCustom(colors: colors, typography: PaymentTypography.companion.Default, shapes: shapes)
    }
}

extension Color {
    /// A colour from a 0xAARRGGBB value.
    init(argb: UInt32) {
        self.init(
            .sRGB,
            red: Double((argb >> 16) & 0xFF) / 255,
            green: Double((argb >> 8) & 0xFF) / 255,
            blue: Double(argb & 0xFF) / 255,
            opacity: Double((argb >> 24) & 0xFF) / 255
        )
    }
}
