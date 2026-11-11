import SwiftUI

/// The payment is being made: a turning ring, what is happening right now and the amount.
struct ProcessingStepView: View {
    let step: ProcessingStep?
    let order: OrderSummary?
    let reduceMotion: Bool

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        let colors = theme.colors
        let status = strings?.text(key: statusKey) ?? ""
        VStack(spacing: 0) {
            ProgressRing(icon: .lock)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Text(status))
            Text(strings?.text(key: "processing_title") ?? "")
                .paragraph(theme.headline, color: colors.onBackground)
                .accessibilityAddTraits(.isHeader)
                .padding(.top, 20)
            if step != nil {
                ZStack(alignment: .top) {
                    Text(status)
                        .paragraph(theme.body, color: colors.onSurfaceVariant)
                        .frame(maxWidth: .infinity)
                        .accessibilityIdentifier("processing_step")
                        .id(status)
                        .transition(statusChange)
                }
                .padding(.top, 6)
                .animation(.linear(duration: SheetMotion.statusOutDuration + SheetMotion.statusInDuration), value: status)
            }
            if let amount = order?.formattedAmount, !amount.isEmpty {
                Text(Bidi.ltr(amount))
                    .line(theme.body.tabular().weight(theme.title.weight), color: colors.onSurfaceVariant)
                    .padding(.top, 6)
            }
            Text(strings?.text(key: "processing_hint") ?? "")
                .paragraph(theme.body, color: colors.onSurfaceVariant)
                .frame(maxWidth: 280)
                .padding(.top, 12)
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("ProcessingScreen")
        .onChange(of: status) { UIAccessibility.post(notification: .announcement, argument: $0) }
        .announcesScreenChange(reduceMotion: reduceMotion)
    }

    /// The old line fades out first, then the new one fades in.
    private var statusChange: AnyTransition {
        .asymmetric(
            insertion: AnyTransition.opacity.animation(
                Animation.timingCurve(0.4, 0, 0.2, 1, duration: SheetMotion.statusInDuration).delay(SheetMotion.statusOutDuration)
            ),
            removal: AnyTransition.opacity.animation(.timingCurve(0.4, 0, 0.2, 1, duration: SheetMotion.statusOutDuration))
        )
    }

    private var statusKey: String {
        guard let step = step else { return "processing_title" }
        switch step {
        case .loadingSession: return "processing_loading_session"
        case .loadingPublicKey: return "processing_loading_public_key"
        case .tokenizingCard: return "processing_tokenizing_card"
        case .submittingPayment: return "processing_submitting_payment"
        case .finalizing: return "processing_finalizing"
        default: return "processing_title"
        }
    }
}

/// Isolates text that reads left to right (amounts, card masks, codes) inside a right-to-left line.
enum Bidi {
    static func ltr(_ text: String) -> String { "\u{2066}" + text + "\u{2069}" }
}
