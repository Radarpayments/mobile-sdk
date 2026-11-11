import SwiftUI
import UIKit

/// How the payment ended: the mark, the amount, the receipt of a success or the reason of a failure, and the way on.
struct ResultStepView: View {
    let result: ResultSpec
    let reduceMotion: Bool
    let onClose: () -> Void
    let onRetry: () -> Void

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        let cascade = result.success && !reduceMotion
        VStack(spacing: 20) {
            VStack(spacing: 6) {
                ResultMark(success: result.success, reduceMotion: reduceMotion, onMarkStart: haptic)
                    // A 4-point spacer of its own in the spaced column: its spacing comes on top.
                    .padding(.bottom, 10)
                Text(result.title)
                    .paragraph(theme.headline, color: theme.colors.onBackground)
                    .accessibilityAddTraits(.isHeader)
                    .cascadeIn(0, enabled: cascade)
                if let amount = result.amount {
                    Text(amount)
                        // A little under the style's size, so that the amount does not outweigh the headline.
                        .line(theme.amount, color: theme.colors.onBackground, size: theme.amount.size * 0.93)
                        .cascadeIn(1, enabled: cascade)
                }
                if result.success {
                    if !result.lines.isEmpty {
                        ReceiptView(lines: result.lines, cascade: cascade)
                            .padding(.top, 16)
                    }
                } else {
                    failure
                }
            }
            .frame(maxWidth: .infinity)
            buttons
        }
        .padding(.horizontal, sidePadding)
        .padding(.top, 18)
        .accessibilityElement(children: .contain)
        // The main button is drawn by the button layer outside the step, and VoiceOver read it before the title.
        .accessibilitySortPriority(1)
        .accessibilityIdentifier("ResultScreen")
        .onAppear {
            // The result interrupts whatever VoiceOver is reading, as an assertive live region does.
            UIAccessibility.post(notification: .announcement, argument: result.title)
        }
        .announcesScreenChange(reduceMotion: reduceMotion)
    }

    @ViewBuilder private var failure: some View {
        if let message = result.message {
            Text(message)
                .paragraph(theme.body, color: theme.colors.onSurfaceVariant)
                .frame(maxWidth: 300)
                .padding(.top, 4)
        }
        if let chip = result.chip {
            Text(chip)
                .paragraph(theme.caption.tabular(), color: theme.colors.error)
                .padding(.horizontal, 10)
                .padding(.vertical, 3)
                .background(Capsule().fill(theme.colors.errorSoft))
                .padding(.top, 6)
        }
    }

    private var buttons: some View {
        VStack(spacing: 8) {
            if result.success {
                CheckoutButton(text: strings?.text(key: "result_done") ?? "", primary: true, action: onClose)
            } else {
                if result.canRetry {
                    CheckoutButton(text: strings?.text(key: "result_try_again") ?? "", primary: true, action: onRetry)
                }
                StepButton(
                    text: strings?.text(key: "result_close") ?? "",
                    kind: result.canRetry ? .secondary : .primary,
                    action: onClose
                )
            }
        }
    }

    private func haptic() {
        UINotificationFeedbackGenerator().notificationOccurred(result.success ? .success : .error)
    }
}
