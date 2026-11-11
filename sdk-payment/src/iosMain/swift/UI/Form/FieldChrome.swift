import SwiftUI

/// A field on its own: its name over it, what is wrong with it under.
struct FormField<Control: View>: View {
    let label: String
    var message: String?
    var isError = false
    @ViewBuilder let control: () -> Control

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            FieldGroupHeader(title: label)
            control()
            if let message = message, !message.isEmpty {
                FieldMessage(text: message, isError: isError)
            }
        }
    }
}

/// The frame of a field on its own.
struct FieldBox<Content: View>: View {
    var focused = false
    var isError = false
    var readOnly = false
    var valid = false
    @ViewBuilder let content: () -> Content

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        let colors = theme.colors
        let border: Color = readOnly ? colors.surface : isError ? colors.error : focused ? colors.primary : colors.outline
        HStack(spacing: 4) {
            content()
            if valid && !isError {
                IconView(icon: .check, size: 18, color: colors.success)
            }
        }
        .padding(.leading, 14)
        .padding(.trailing, 10)
        .frame(maxWidth: .infinity)
        .frame(height: fieldHeight)
        .background(RoundedRectangle(cornerRadius: theme.fieldCorner).fill(readOnly ? colors.surface : colors.background))
        // `Modifier.border` rounds its width up to whole pixels, unlike the stroke a field group draws.
        .overlay(
            FrameOutline(radius: theme.fieldCorner, width: borderWidth(groupStroke)).stroke(border, lineWidth: borderWidth(groupStroke))
        )
    }
}
