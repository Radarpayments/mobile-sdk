import SwiftUI

/// The badge of the result: it pops in, the check or the cross draws itself, and a failure shakes its head.
struct ResultMark: View {
    let success: Bool
    let reduceMotion: Bool
    /// The mark starts to draw: the moment for the haptic.
    let onMarkStart: () -> Void

    @Environment(\.checkoutTheme) private var theme
    @State private var scale: CGFloat
    @State private var drawn: CGFloat
    @State private var shake: CGFloat = 0

    private let badge: CGFloat = 76
    private let icon: CGFloat = 38

    init(success: Bool, reduceMotion: Bool, onMarkStart: @escaping () -> Void) {
        self.success = success
        self.reduceMotion = reduceMotion
        self.onMarkStart = onMarkStart
        _scale = State(initialValue: reduceMotion ? 1 : 0.5)
        _drawn = State(initialValue: reduceMotion ? 1 : 0)
    }

    var body: some View {
        let colors = theme.colors
        let mark: PaymentIcon = success ? .check : .cross
        return DrawnIconShape(icon: mark, drawn: drawn)
            .stroke(
                success ? colors.success : colors.error,
                style: StrokeStyle(lineWidth: mark.stroke * icon / DrawnIconShape.grid, lineCap: .round, lineJoin: .round)
            )
            .frame(width: icon, height: icon)
            .frame(width: badge, height: badge)
            .background(Circle().fill(success ? colors.successSoft : colors.errorSoft))
            .scaleEffect(scale)
            .modifier(Shake(progress: shake))
            .accessibilityHidden(true)
            .accessibilityIdentifier(success ? "SuccessIcon" : "FailureIcon")
            .onAppear(perform: play)
    }

    private func play() {
        guard !reduceMotion else {
            onMarkStart()
            return
        }
        // Compose's spring(dampingRatio, stiffness) with a mass of 1: damping = 2 · ratio · √stiffness.
        let damping = 2 * SheetMotion.badgeDamping * SheetMotion.badgeStiffness.squareRoot()
        withAnimation(Animation.interpolatingSpring(stiffness: SheetMotion.badgeStiffness, damping: damping).delay(SheetMotion.badgeDelay)) {
            scale = 1
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + SheetMotion.markDelay) {
            onMarkStart()
            withAnimation(SheetMotion.mark(SheetMotion.markDuration)) { drawn = 1 }
        }
        if !success {
            withAnimation(Animation.linear(duration: SheetMotion.shakeDuration).delay(SheetMotion.shakeDelay)) { shake = 1 }
        }
    }
}

/// Sideways through `SheetMotion.shakeOffsets`, evenly spaced over the shake.
private struct Shake: GeometryEffect {
    var progress: CGFloat

    var animatableData: CGFloat {
        get { progress }
        set { progress = newValue }
    }

    func effectValue(size: CGSize) -> ProjectionTransform {
        let offsets = SheetMotion.shakeOffsets
        let position = min(max(progress, 0), 1) * CGFloat(offsets.count - 1)
        let index = min(Int(position), offsets.count - 2)
        let share = position - CGFloat(index)
        let x = CGFloat(offsets[index]) + (CGFloat(offsets[index + 1]) - CGFloat(offsets[index])) * share
        return ProjectionTransform(CGAffineTransform(translationX: x, y: 0))
    }
}
