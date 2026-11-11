import SwiftUI

/// The full-width button under a waiting or result screen.
struct StepButton: View {
    enum Kind {
        case primary
        case secondary
    }

    let text: String
    var kind: Kind = .primary
    let action: () -> Void

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        let colors = theme.colors
        Button(action: action) {
            Text(text)
                .line(theme.button.tabular(), color: kind == .primary ? colors.onPrimary : colors.onBackground)
                .truncationMode(.tail)
                .padding(.horizontal, 16)
                .frame(maxWidth: .infinity, minHeight: 54, maxHeight: 54)
                .background(kind == .primary ? colors.primary : colors.secondaryFill)
                .clipShape(RoundedRectangle(cornerRadius: theme.buttonCorner, style: .circular))
                .contentShape(Rectangle())
        }
        .buttonStyle(PressedScale())
    }
}

private struct PressedScale: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.scaleEffect(configuration.isPressed ? 0.985 : 1)
    }
}
