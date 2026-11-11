import SwiftUI

/// Under the bank's page while the buyer confirms the payment there: what is awaited and a way out.
struct ThreeDsStepView: View {
    let order: OrderSummary?
    let reduceMotion: Bool
    let onCancel: () -> Void

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings
    @EnvironmentObject private var insets: SheetInsets

    /// The sheet's own padding above and below its content.
    private let sheetChrome: CGFloat = 20

    var body: some View {
        let colors = theme.colors
        VStack(spacing: 0) {
            VStack(spacing: 0) {
                ProgressRing(icon: .shield)
                Text(strings?.text(key: "threeds_title") ?? "")
                    .paragraph(theme.headline, color: colors.onBackground)
                    .accessibilityAddTraits(.isHeader)
                    .padding(.top, 20)
                if let amount = order?.formattedAmount {
                    Text(Bidi.ltr(amount))
                        .line(theme.body.tabular().weight(theme.title.weight), color: colors.onSurfaceVariant)
                        .padding(.top, 6)
                }
                Text(strings?.text(key: "threeds_hint") ?? "")
                    .paragraph(theme.body, color: colors.onSurfaceVariant)
                    .frame(maxWidth: 280)
                    .padding(.top, 12)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            StepButton(text: strings?.text(key: "threeds_cancel") ?? "", kind: .secondary, action: onCancel)
        }
        .padding(.horizontal, sidePadding)
        .padding(.top, 18)
        // The step container sizes a step to its content, and this one takes the whole opened sheet.
        .frame(height: max(UIScreen.main.bounds.height - insets.top - sheetChrome - insets.sheetBottom, 0))
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("ThreeDsChallengeScreen")
        .announcesScreenChange(reduceMotion: reduceMotion)
    }
}
