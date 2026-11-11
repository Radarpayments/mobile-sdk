import SwiftUI

/// Where the hint's bubble hangs from: the bounds of its button in the step that draws the bubble.
final class CvcHintAnchor {
    static let space = "cvc-hint"
    var frame = CGRect.zero
}

private struct CvcHintAnchorKey: EnvironmentKey {
    static let defaultValue = CvcHintAnchor()
}

extension EnvironmentValues {
    var cvcHintAnchor: CvcHintAnchor {
        get { self[CvcHintAnchorKey.self] }
        set { self[CvcHintAnchorKey.self] = newValue }
    }
}

/// The info mark in the security code's cell: it shows where the code is on the card, the keyboard stays up.
struct CvcHintButton: View {
    @Binding var shown: Bool

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings
    @Environment(\.cvcHintAnchor) private var anchor

    var body: some View {
        GeometryReader { proxy in
            Button(action: {
                anchor.frame = proxy.frame(in: .named(CvcHintAnchor.space))
                shown.toggle()
            }, label: {
                IconView(icon: .info, size: 20, color: shown ? theme.colors.primary : theme.colors.onSurfaceVariant)
                    .frame(width: Self.target, height: Self.target)
                    .contentShape(Rectangle())
            })
            .buttonStyle(PlainPress())
            .accessibility(label: Text(strings?.text(key: "cvv_hint_open") ?? ""))
        }
        .frame(width: Self.target, height: Self.target)
    }

    private static let target: CGFloat = 32
}

/// The hint's bubble under its button, its arrow at the button's middle.
struct CvcHintBubble: View {
    let text: String

    @Environment(\.checkoutTheme) private var theme

    /// From the bubble's trailing edge to the middle of the arrow, which stands at the middle of the button.
    static let arrowInset: CGFloat = 22
    static let anchorGap: CGFloat = 8
    private static let arrowWidth: CGFloat = 12
    private static let arrowHeight: CGFloat = 6

    var body: some View {
        VStack(alignment: .trailing, spacing: 0) {
            Arrow()
                .fill(theme.colors.onBackground)
                .frame(width: Self.arrowWidth, height: Self.arrowHeight)
                .padding(.trailing, Self.arrowInset - Self.arrowWidth / 2)
            Text(text)
                .line(theme.caption, color: theme.colors.background)
                .fixedSize()
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(
                    ZStack {
                        // Fitted to the shadow Skia draws for an elevation of 8dp: narrower than the bubble, cast downwards.
                        RoundedRectangle(cornerRadius: 10)
                            .fill(Color.black.opacity(0.25))
                            .padding(.horizontal, 2.5)
                            .offset(y: 8)
                            .blur(radius: 7.6)
                        RoundedRectangle(cornerRadius: 10).fill(theme.colors.onBackground)
                    }
                )
        }
    }

    private struct Arrow: Shape {
        func path(in rect: CGRect) -> Path {
            var path = Path()
            path.move(to: CGPoint(x: rect.midX, y: rect.minY))
            path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
            path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
            path.closeSubpath()
            return path
        }
    }
}
