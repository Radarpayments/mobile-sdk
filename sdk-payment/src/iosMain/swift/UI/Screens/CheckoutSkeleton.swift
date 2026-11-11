import SwiftUI

/// The first step while the session loads: the layout of the step that will replace it, as pulsing blocks.
struct CheckoutSkeleton: View {
    let model: CheckoutModel
    let ui: CheckoutUi

    @Environment(\.checkoutStrings) private var strings
    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        let onClose = { model.handle(intent: PaymentIntentCancel.shared) }
        Group {
            if ui.skeleton.isCardFormRoot {
                cardForm(onClose: onClose)
            } else {
                methods(onClose: onClose)
            }
        }
        .padding(.horizontal, sidePadding)
    }

    private func methods(onClose: @escaping () -> Void) -> some View {
        VStack(spacing: sheetItemSpacing) {
            StepHeader(title: strings?.text(key: "checkout_title") ?? "", onClose: onClose)
            VStack(spacing: sheetItemSpacing) {
                PaymentMethodsBody(
                    wallet: ui.skeleton.wallet,
                    order: nil,
                    savedCards: (0..<Int(ui.skeleton.savedCards)).map(Self.card)
                )
            }
            .environment(\.isPlaceholder, true)
            .skeletonPulse(visible: ui.skeletonVisible, reduceMotion: ui.reduceMotion, label: loadingLabel)
        }
    }

    private func cardForm(onClose: @escaping () -> Void) -> some View {
        VStack(spacing: 0) {
            VStack(spacing: 0) {
                StepHeader(title: strings?.text(key: "card_form_title") ?? "", onClose: onClose)
                VStack(spacing: 16) {
                    HStack(spacing: 0) {
                        PlaceholderBlock().frame(width: 96, height: 13)
                        Spacer(minLength: 0)
                        PlaceholderBlock().frame(width: 104, height: 13)
                    }
                    .frame(height: 20)
                    VStack(spacing: 0) {
                        PlaceholderBlock().frame(width: 110, height: 14).padding(.leading, 14)
                            .frame(maxWidth: .infinity, minHeight: fieldHeight, maxHeight: fieldHeight, alignment: .leading)
                        PlaceholderBlock().frame(width: 64, height: 14).padding(.leading, 14)
                            .frame(maxWidth: .infinity, minHeight: fieldHeight, maxHeight: fieldHeight, alignment: .leading)
                    }
                    .overlay(
                        RoundedRectangle(cornerRadius: theme.cardCorner, style: .circular)
                            .strokeBorder(theme.colors.outline, lineWidth: borderWidth(1.5))
                    )
                }
                .padding(.top, 6)
                .padding(.bottom, 20)
                .skeletonPulse(visible: ui.skeletonVisible, reduceMotion: ui.reduceMotion, label: loadingLabel)
            }
            PlaceholderBlock(corner: theme.buttonCorner)
                .frame(maxWidth: .infinity)
                .frame(height: checkoutButtonHeight)
                .padding(.top, 12)
                .skeletonPulse(visible: ui.skeletonVisible, reduceMotion: ui.reduceMotion, label: nil)
        }
    }

    private var loadingLabel: String {
        strings?.text(key: "a11y_loading") ?? ""
    }

    private static func card(_ index: Int) -> SavedCardInfo {
        SavedCardSpecs.shared.skeletonCard(index: Int32(index))
    }
}

private struct SkeletonPulse: ViewModifier {
    let visible: Bool
    let reduceMotion: Bool
    let label: String?

    @State private var dimmed = false

    func body(content: Content) -> some View {
        content
            .opacity(dimmed ? SheetMotion.pulseMinOpacity : 1)
            .opacity(visible ? 1 : 0)
            .animation(.timingCurve(0.4, 0, 0.2, 1, duration: SheetMotion.skeletonFadeDuration), value: visible)
            .onAppear {
                guard !reduceMotion else { return }
                withAnimation(Animation.timingCurve(0.4, 0, 0.2, 1, duration: SheetMotion.pulseDuration / 2).repeatForever(autoreverses: true)) {
                    dimmed = true
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(label ?? ""))
            .accessibilityAddTraits(label == nil ? [] : .updatesFrequently)
            .accessibilityHidden(label == nil)
    }
}

private extension View {
    /// Fades the blocks in when the skeleton shows and pulses them; VoiceOver reads them as one "Loading".
    func skeletonPulse(visible: Bool, reduceMotion: Bool, label: String?) -> some View {
        modifier(SkeletonPulse(visible: visible, reduceMotion: reduceMotion, label: label))
    }
}
