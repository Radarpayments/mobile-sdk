import CoreText
import SwiftUI
import UIKit

/// The system font as the Android checkout's text engine sets it on iOS: without the font's size-specific tracking
/// and at one optical size, so that a line comes out as wide as it does there.
enum MatchedFont {
    /// Above this size UIKit moves to the display shapes; that engine keeps the text shapes at every size.
    private static let textOpticalSize: CGFloat = 17
    private static let opticalSizeAxis = 0x6F70_737A as CFNumber
    private static var trackings: [UIFont: CGFloat] = [:]

    static func font(_ base: UIFont) -> UIFont {
        guard base.pointSize > textOpticalSize else { return base }
        let descriptor = CTFontDescriptorCreateCopyWithVariation(
            base.fontDescriptor as CTFontDescriptor,
            opticalSizeAxis,
            textOpticalSize
        )
        return CTFontCreateWithFontDescriptor(descriptor, base.pointSize, nil) as UIFont
    }

    /// What the font's tracking table adds to every glyph at this size: a laid-out glyph's advance less the outline's.
    static func tracking(of font: UIFont) -> CGFloat {
        if let known = trackings[font] { return known }
        let probe = "n"
        var glyph = CGGlyph(0)
        var advance = Int32(0)
        let graphics = CTFontCopyGraphicsFont(font as CTFont, nil)
        guard CTFontGetGlyphsForCharacters(font as CTFont, Array(probe.utf16), &glyph, 1),
              graphics.getGlyphAdvances(glyphs: &glyph, count: 1, advances: &advance),
              graphics.unitsPerEm > 0
        else { return 0 }
        let line = CTLineCreateWithAttributedString(NSAttributedString(string: probe, attributes: [.font: font]))
        let laidOut = CGFloat(CTLineGetTypographicBounds(line, nil, nil, nil))
        let tracking = laidOut - CGFloat(advance) * font.pointSize / CGFloat(graphics.unitsPerEm)
        trackings[font] = tracking
        return tracking
    }
}

extension CheckoutTextStyle {
    /// The height the Android checkout's text engine gives one line: the font's own, on whole pixels.
    var singleLineHeight: CGFloat {
        let scale = UIScreen.main.scale
        return (uiFont.lineHeight * scale).rounded() / scale
    }

    func sized(_ size: CGFloat) -> UIFont {
        let system = registeredFont(size: size, weight: weight, tabular: tabularFigures)
            ?? (tabularFigures
                ? UIFont.monospacedDigitSystemFont(ofSize: size, weight: weight)
                : UIFont.systemFont(ofSize: size, weight: weight))
        // Skia falls back to Geeza Pro for the scripts the system font lacks; UIKit would pick SF Arabic.
        let fallback = UIFontDescriptor(fontAttributes: [.family: "Geeza Pro"])
        return UIFont(descriptor: system.fontDescriptor.addingAttributes([.cascadeList: [fallback]]), size: size)
    }

    /// The app's own family at the nearest weight it has; nil for the system font, or when iOS does not know the family
    /// (UIKit would otherwise substitute a font of its own choosing).
    func registeredFont(size: CGFloat, weight: UIFont.Weight, tabular: Bool) -> UIFont? {
        guard let fontFamily, !UIFont.fontNames(forFamilyName: fontFamily).isEmpty else { return nil }
        var attributes: [UIFontDescriptor.AttributeName: Any] = [
            .family: fontFamily,
            .traits: [UIFontDescriptor.TraitKey.weight: weight],
        ]
        if tabular {
            attributes[.featureSettings] = [[
                kCTFontFeatureTypeIdentifierKey as String: kNumberSpacingType,
                kCTFontFeatureSelectorIdentifierKey as String: kMonospacedNumbersSelector,
            ]]
        }
        return UIFont(descriptor: UIFontDescriptor(fontAttributes: attributes), size: size)
    }
}

extension Text {
    /// One line in `style`, as tall as the Android checkout draws it: the font's own height on whole pixels. The
    /// style's line height only spaces the lines of a paragraph apart.
    func line(_ style: CheckoutTextStyle, color: Color, size: CGFloat? = nil) -> some View {
        let base = style.sized(size ?? style.size)
        let font = MatchedFont.font(base)
        let scale = UIScreen.main.scale
        return self.font(Font(font))
            .kerning(style.tracking - MatchedFont.tracking(of: font))
            .foregroundColor(color)
            .lineLimit(1)
            // The line is as tall as the font is at its own optical size, whichever shapes fill it.
            .frame(height: (base.lineHeight * scale).rounded() / scale)
    }

    /// A centred paragraph in `style` that may wrap.
    func paragraph(_ style: CheckoutTextStyle, color: Color) -> some View {
        let base = style.uiFont
        let font = MatchedFont.font(base)
        let scale = UIScreen.main.scale
        // A line is as tall as the font is at its own optical size; the other shapes' line is a pixel taller.
        let excess = ((font.lineHeight * scale).rounded(.up) - (base.lineHeight * scale).rounded()) / scale
        return self.font(Font(font))
            .kerning(style.tracking - MatchedFont.tracking(of: font))
            .lineSpacing(style.lineHeight > 0 ? max(style.lineHeight - font.lineHeight, 0) : 0)
            .foregroundColor(color)
            .multilineTextAlignment(.center)
            .fixedSize(horizontal: false, vertical: true)
            .padding(.bottom, -max(excess, 0))
    }
}
