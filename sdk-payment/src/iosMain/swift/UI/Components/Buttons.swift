import SwiftUI

let checkoutButtonHeight: CGFloat = 54
/// Between a button's icon and its text.
let iconGap: CGFloat = 10

enum ButtonKind {
    case primary
    case secondary
    case danger
}

/// The checkout's full-width button. In a skeleton it is a block of its size.
struct CheckoutButton: View {
    let text: String
    var kind: ButtonKind = .primary
    var icon: PaymentIcon?
    var enabled = true
    var loading = false
    /// How the label moves on when it changes.
    var motion: LabelMotion = .inPlace
    /// The step's main button: in the sheet it is drawn by the `primaryButtonLayer`, and stays in place from step to step.
    var primary = false
    let action: () -> Void

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.isPlaceholder) private var isPlaceholder
    @Environment(\.hasPrimaryButtonLayer) private var hasPrimaryButtonLayer

    var body: some View {
        if isPlaceholder {
            PlaceholderBlock(corner: theme.buttonCorner).frame(maxWidth: .infinity).frame(height: checkoutButtonHeight)
        } else if primary && hasPrimaryButtonLayer {
            PrimaryButtonPlace(
                face: PrimaryButtonFace(text: text, icon: icon, kind: kind, enabled: enabled, loading: loading),
                action: action
            )
        } else {
            Button(action: action) {
                ButtonFace(text: text, kind: kind, icon: icon, loading: loading, motion: motion)
            }
            .buttonStyle(PressedButton(enabled: enabled))
            .disabled(!enabled || loading)
        }
    }
}

struct ButtonFace: View {
    let text: String
    let kind: ButtonKind
    let icon: PaymentIcon?
    let loading: Bool
    let motion: LabelMotion

    @Environment(\.checkoutTheme) private var theme

    /// Where the Android checkout puts the label: centred on whole pixels, half a pixel going down.
    private var labelTop: CGFloat {
        let scale = UIScreen.main.scale
        return ((checkoutButtonHeight - theme.button.singleLineHeight) * scale / 2).rounded(.toNearestOrAwayFromZero) / scale
    }

    var body: some View {
        let colors = theme.colors
        let container: Color = kind == .primary ? colors.primary : kind == .secondary ? colors.secondaryFill : colors.error
        let content: Color = kind == .primary ? colors.onPrimary : kind == .secondary ? colors.onBackground : colors.onError
        let style = theme.button.tabular()
        CascadeLabel(
            parts: labelParts(text: text, icon: icon, loading: loading),
            style: style,
            color: content,
            lineTop: labelTop,
            motion: motion
        ) {
            if loading {
                ButtonSpinner(color: content)
            } else {
                HStack(spacing: iconGap) {
                    if let icon = icon {
                        IconView(icon: icon, size: 18, color: content)
                    }
                    Text(text).line(style, color: content).truncationMode(.tail)
                }
                .padding(.top, labelTop)
                .frame(height: checkoutButtonHeight, alignment: .top)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: checkoutButtonHeight)
        .padding(.horizontal, 16)
        .frame(maxWidth: .infinity)
        .frame(height: checkoutButtonHeight)
        .background(container)
        .clipShape(RoundedRectangle(cornerRadius: theme.buttonCorner, style: .circular))
        .contentShape(Rectangle())
    }
}

struct PressedButton: ButtonStyle {
    let enabled: Bool

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.985 : 1)
            .opacity(enabled ? 1 : 0.45)
    }
}

struct ButtonSpinner: View {
    let color: Color

    @State private var turning = false

    var body: some View {
        Circle()
            .trim(from: 0, to: 0.75)
            .stroke(color, style: StrokeStyle(lineWidth: 2, lineCap: .round))
            .frame(width: 18, height: 18)
            .frame(width: 20, height: 20)
            .rotationEffect(.degrees(turning ? 360 : 0))
            .onAppear {
                withAnimation(Animation.linear(duration: 1.1).repeatForever(autoreverses: false)) { turning = true }
            }
            .accessibilityHidden(true)
    }
}

/// A text button in the primary colour, 40 points tall, for actions beside a section title.
struct TextAction: View {
    let text: String
    var icon: PaymentIcon?
    var color: Color?
    let action: () -> Void

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.isPlaceholder) private var isPlaceholder

    var body: some View {
        let tint = color ?? theme.colors.primary
        Button(action: action) {
            HStack(spacing: 5) {
                if let icon = icon {
                    IconView(icon: icon, size: 18, color: tint).placeholderText()
                }
                Text(text).line(theme.label.weight(.semibold), color: tint).placeholderText()
            }
            .padding(.horizontal, 8)
            .frame(height: 40)
            .contentShape(RoundedRectangle(cornerRadius: 10, style: .circular))
        }
        .buttonStyle(PlainPress())
        .disabled(isPlaceholder)
    }
}

/// An icon button with a 44-point target that tints its backdrop while pressed.
struct IconAction: View {
    let icon: PaymentIcon
    let label: String
    var tint: Color?
    let action: () -> Void

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        Button(action: action) {
            IconView(icon: icon, size: 22, color: tint ?? theme.colors.onBackground)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(IconPress(pressed: theme.colors.primarySoft))
        .accessibilityLabel(Text(label))
    }
}

private struct IconPress: ButtonStyle {
    let pressed: Color

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? pressed : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .circular))
    }
}
