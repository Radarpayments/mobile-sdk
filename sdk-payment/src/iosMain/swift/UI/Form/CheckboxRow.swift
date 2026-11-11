import SwiftUI

/// A checkbox with its title and a line under it. Without `onChange` it shows its state and does not change.
struct CheckboxRow: View {
    let title: String
    var description: String?
    let checked: Bool
    var onChange: ((Bool) -> Void)?

    @Environment(\.checkoutTheme) private var theme

    /// Half the height of a capital letter, as a share of the font size: the box is centred on the title's capitals.
    private static let capHalf: CGFloat = 0.355
    private static let boxSize: CGFloat = 22

    var body: some View {
        let style = theme.body.weight(theme.label.weight)
        let scale = UIScreen.main.scale
        let capHalf = (style.size * scale * Self.capHalf).rounded() / scale
        Button(action: { onChange?(!checked) }, label: {
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                box.alignmentGuide(.firstTextBaseline) { $0.height / 2 + capHalf }
                VStack(alignment: .leading, spacing: 2) {
                    LeadingParagraph(text: title, style: style, color: theme.colors.onBackground)
                    if let description = description {
                        LeadingParagraph(text: description, style: theme.caption, color: theme.colors.onSurfaceVariant)
                    }
                }
            }
            .padding(.vertical, 4)
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
        })
        .buttonStyle(PlainPress())
        .accessibilityElement(children: .ignore)
        .accessibility(label: Text([title, description ?? ""].filter { !$0.isEmpty }.joined(separator: ", ")))
        .accessibility(addTraits: checked ? [.isButton, .isSelected] : .isButton)
    }

    private var box: some View {
        let colors = theme.colors
        return ZStack {
            RoundedRectangle(cornerRadius: 6).fill(checked ? colors.primary : Color.clear)
            RoundedRectangle(cornerRadius: 6).strokeBorder(checked ? colors.primary : colors.outlineStrong, lineWidth: 1.5)
            if checked {
                IconView(icon: .check, size: 16, color: colors.onPrimary)
            }
        }
        .frame(width: Self.boxSize, height: Self.boxSize)
        .animation(.easeOut(duration: 0.15), value: checked)
    }
}
