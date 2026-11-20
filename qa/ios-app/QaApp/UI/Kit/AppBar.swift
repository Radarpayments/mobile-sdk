import SwiftUI

/// The bar on top of every screen: Back when there is a screen to return to, the title, the actions; a line under it.
struct AppBar<Actions: View>: View {
    let title: String
    let back: (() -> Void)?
    @ViewBuilder let actions: () -> Actions

    var body: some View {
        HStack(spacing: 16) {
            if let back {
                Button(action: back) {
                    Chevron(direction: .left, size: 10, stroke: 2, color: QaColor.ink)
                        .frame(width: QaSize.control, height: QaSize.control, alignment: .leading)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Back")
                // The target stays 44 pt wide under the title while the title keeps the mockup's 16 pt after the glyph.
                .padding(.trailing, -(QaSize.control - 10))
            }
            Text(title)
                .qaStyle(QaType.title)
                .foregroundStyle(QaColor.ink)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityAddTraits(.isHeader)
            actions()
        }
        .padding(.horizontal, QaSize.screen)
        .frame(height: QaSize.appBar)
        .overlay(alignment: .bottom) { HLine() }
    }
}

/// A word in the bar that does something: Add, Copy, Clear.
struct BarAction: View {
    let text: String
    let tag: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .qaStyle(QaType.action)
                .foregroundStyle(QaColor.ink)
                .frame(minHeight: QaSize.control)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(tag)
    }
}

/// The bar's menu: three squares that open a list of actions.
struct BarMenu<Items: View>: View {
    let tag: String
    @ViewBuilder let items: () -> Items

    var body: some View {
        Menu(content: items) {
            MenuDots()
                .frame(width: QaSize.control, height: QaSize.control, alignment: .trailing)
                .contentShape(Rectangle())
        }
        .accessibilityLabel("Menu")
        .accessibilityIdentifier(tag)
    }
}

/// A 1 pt line across.
struct HLine: View {
    var leading: CGFloat = 0

    var body: some View {
        QaColor.line.frame(height: QaSize.line).padding(.leading, leading)
    }
}
