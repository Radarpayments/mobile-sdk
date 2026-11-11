import SwiftUI

let stepHeaderHeight: CGFloat = 44
private let roundActionTarget: CGFloat = 44
private let roundActionSize: CGFloat = 32
private let roundActionInset = (roundActionTarget - roundActionSize) / 2

/// A step's header: the centred title between a back and a close button; a nil action leaves its button out.
struct StepHeader: View {
    let title: String
    var onBack: (() -> Void)?
    var onClose: (() -> Void)?

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        ZStack {
            Text(title)
                .line(theme.title, color: theme.colors.onBackground)
                .truncationMode(.tail)
                .padding(.horizontal, roundActionTarget + 8)
                .accessibilityAddTraits(.isHeader)
            HStack(spacing: 0) {
                if let onBack = onBack {
                    RoundIconAction(icon: .back, label: strings?.text(key: "action_back") ?? "", action: onBack)
                        .padding(.leading, -roundActionInset)
                }
                Spacer(minLength: 0)
                if let onClose = onClose {
                    RoundIconAction(icon: .close, label: strings?.text(key: "action_close") ?? "", action: onClose)
                        .padding(.trailing, -roundActionInset)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: stepHeaderHeight)
        .announcesScreenChangeWhenSettled()
    }
}

/// A round icon button of the header: a 32-point disc in a 44-point touch target.
struct RoundIconAction: View {
    let icon: PaymentIcon
    let label: String
    let action: () -> Void

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle().fill(theme.colors.surface).frame(width: roundActionSize, height: roundActionSize)
                IconView(icon: icon, size: 18, color: theme.colors.onBackground)
            }
            .frame(width: roundActionTarget, height: roundActionTarget)
            .contentShape(Circle())
        }
        .buttonStyle(PlainPress())
        .accessibilityLabel(Text(label))
    }
}

/// A hairline with a caption in its middle.
struct LabeledDivider: View {
    let text: String

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        HStack(spacing: 12) {
            line
            Text(text).line(theme.caption, color: theme.colors.onSurfaceVariant).placeholderText()
            line
        }
    }

    private var line: some View {
        Rectangle().fill(theme.colors.outline).frame(height: 1).frame(maxWidth: .infinity)
    }
}

/// A button that does not dim or highlight when pressed: the checkout has no press indication.
struct PlainPress: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
    }
}
