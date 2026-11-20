import SwiftUI
import UIKit

/// The colours, light and dark: the system setting picks the set.
enum QaColor {
    static let bg = pair(0xFFFFFF, 0x0E0E0E)
    static let ink = pair(0x111111, 0xF2F2F2)
    static let muted = pair(0x6B6B6B, 0x9A9A9A)
    static let line = pair(0xE5E5E5, 0x2A2A2A)
    static let fill = pair(0xF5F5F5, 0x1A1A1A)
    static let ok = pair(0x1A7F4B, 0x4CC38A)
    static let bad = pair(0xC62828, 0xFF6B6B)
    /// Text on an outcome's colour.
    static let onOutcome = pair(0xFFFFFF, 0x0E0E0E)

    private static func pair(_ light: UInt32, _ dark: UInt32) -> Color {
        Color(UIColor { $0.userInterfaceStyle == .dark ? rgb(dark) : rgb(light) })
    }

    private static func rgb(_ value: UInt32) -> UIColor {
        UIColor(
            red: CGFloat((value >> 16) & 0xFF) / 255,
            green: CGFloat((value >> 8) & 0xFF) / 255,
            blue: CGFloat(value & 0xFF) / 255,
            alpha: 1
        )
    }
}

/// The sizes, on a 4 pt grid.
enum QaSize {
    static let screen: CGFloat = 16
    static let captionTop: CGFloat = 24
    static let captionBottom: CGFloat = 8
    static let row: CGFloat = 52
    static let rowTwoLines: CGFloat = 68
    static let gap: CGFloat = 8
    static let control: CGFloat = 44
    static let field: CGFloat = 52
    static let button: CGFloat = 52
    static let chip: CGFloat = 36
    static let appBar: CGFloat = 56
    static let line: CGFloat = 1
}

/// One step of the type scale: sans for words, monospace for values, identifiers, codes and the journal.
struct QaTextStyle {
    var size: CGFloat
    var lineHeight: CGFloat
    var weight: Font.Weight = .regular
    var mono = false
    var tracking: CGFloat = 0
    var underline = false

    var font: Font { .system(size: size, weight: weight, design: mono ? .monospaced : .default) }

    var uiFont: UIFont {
        let font = UIFont.systemFont(ofSize: size, weight: weight.uiWeight)
        guard mono, let descriptor = font.fontDescriptor.withDesign(.monospaced) else { return font }
        return UIFont(descriptor: descriptor, size: size)
    }

    func with(weight: Font.Weight) -> QaTextStyle {
        var style = self
        style.weight = weight
        return style
    }
}

enum QaType {
    static let title = QaTextStyle(size: 20, lineHeight: 28, weight: .semibold)
    static let body = QaTextStyle(size: 16, lineHeight: 24)
    static let bodyStrong = body.with(weight: .semibold)
    static let action = body.with(weight: .medium)
    static let label = QaTextStyle(size: 14, lineHeight: 20, weight: .medium)
    static let link: QaTextStyle = {
        var style = label
        style.underline = true
        return style
    }()

    static let caption = QaTextStyle(size: 12, lineHeight: 16, weight: .semibold, tracking: 12 * 0.08)
    static let help = QaTextStyle(size: 13, lineHeight: 20)
    static let button = bodyStrong
    static let outcome = QaTextStyle(size: 28, lineHeight: 36, weight: .semibold)
    static let value = QaTextStyle(size: 13, lineHeight: 20, mono: true)
    static let input = QaTextStyle(size: 16, lineHeight: 24, mono: true)
    static let inputSmall = QaTextStyle(size: 14, lineHeight: 20, mono: true)
    static let journal = QaTextStyle(size: 12, lineHeight: 16, mono: true)
    static let journalHead = QaTextStyle(size: 14, lineHeight: 20, mono: true)
}

extension View {
    /// The font of [style] with its line height: the leading between lines and half of the rest above and below.
    func qaStyle(_ style: QaTextStyle) -> some View {
        let extra = max(0, style.lineHeight - style.uiFont.lineHeight)
        return font(style.font)
            .tracking(style.tracking)
            .underline(style.underline)
            .lineSpacing(extra)
            .padding(.vertical, extra / 2)
    }
}

private extension Font.Weight {
    var uiWeight: UIFont.Weight {
        switch self {
        case .medium: .medium
        case .semibold: .semibold
        case .bold: .bold
        default: .regular
        }
    }
}
