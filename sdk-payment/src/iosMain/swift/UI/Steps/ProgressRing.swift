import SwiftUI

/// A quarter arc turning around an icon: the payment is on its way.
struct ProgressRing: View {
    let icon: PaymentIcon

    @Environment(\.checkoutTheme) private var theme
    @State private var turned = false

    private let size: CGFloat = 84
    private let stroke: CGFloat = 3
    private let turnDuration = 1.1

    var body: some View {
        ZStack {
            Circle()
                .stroke(theme.colors.primarySoft, lineWidth: stroke)
                .padding(stroke / 2)
            Circle()
                .trim(from: 0, to: 0.25)
                .stroke(theme.colors.primary, style: StrokeStyle(lineWidth: stroke, lineCap: .round))
                .padding(stroke / 2)
                // A circle's path starts at three o'clock; the arc starts at twelve.
                .rotationEffect(.degrees(turned ? 270 : -90))
            IconView(icon: icon, size: 30, color: theme.colors.primary)
        }
        .frame(width: size, height: size)
        .onAppear {
            withAnimation(Animation.linear(duration: turnDuration).repeatForever(autoreverses: false)) { turned = true }
        }
    }
}
