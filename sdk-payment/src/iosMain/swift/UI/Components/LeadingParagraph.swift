import CoreText
import SwiftUI
import UIKit

/// A paragraph set from the leading edge, with its lines broken where the Android checkout breaks them: each line takes
/// every word that fits. `Text` moves a word down to keep the last line from being a single word.
struct LeadingParagraph: View {
    let text: String
    let style: CheckoutTextStyle
    let color: Color

    @State private var width: CGFloat = 0

    var body: some View {
        let font = MatchedFont.font(style.uiFont)
        let kerning = style.tracking - MatchedFont.tracking(of: font)
        let lines = width > 0 ? LeadingParagraph.lines(of: text, font: font, kerning: kerning, width: width) : [text]
        VStack(alignment: .leading, spacing: max(style.lineHeight - style.singleLineHeight, 0)) {
            ForEach(lines.indices, id: \.self) { index in
                Text(lines[index]).line(style, color: color)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(GeometryReader { proxy in
            Color.clear
                .onAppear { width = proxy.size.width }
                .onChange(of: proxy.size.width) { width = $0 }
        })
        .accessibilityElement(children: .ignore)
        .accessibility(label: Text(text))
    }

    private static func lines(of text: String, font: UIFont, kerning: CGFloat, width: CGFloat) -> [String] {
        let string = text as NSString
        let attributed = NSAttributedString(string: text, attributes: [.font: font, .kern: kerning])
        let typesetter = CTTypesetterCreateWithAttributedString(attributed)
        var lines: [String] = []
        var start = 0
        while start < string.length {
            let count = max(CTTypesetterSuggestLineBreak(typesetter, start, Double(width)), 1)
            let line = string.substring(with: NSRange(location: start, length: count))
            lines.append(line.trimmingCharacters(in: .whitespacesAndNewlines))
            start += count
        }
        return lines
    }
}
