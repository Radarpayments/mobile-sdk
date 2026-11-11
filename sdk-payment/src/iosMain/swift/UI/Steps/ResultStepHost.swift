import SwiftUI

/// The result step of the checkout: words the outcome once and hands the buyer's choice to the model.
struct ResultStepHost: View {
    let model: CheckoutModel
    let ui: CheckoutUi
    let completed: PaymentStateCompleted

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings

    // The card logo swaps its colours on a darker sheet than the theme calls dark, as on Android.
    private let darkLogoLuminance = 0.4

    var body: some View {
        if let strings = strings {
            ResultStepView(
                result: ResultSpec.companion.of(
                    completed: completed,
                    ui: ui,
                    strings: strings,
                    dark: theme.colors.backgroundLuminance < darkLogoLuminance
                ),
                reduceMotion: ui.reduceMotion,
                onClose: { model.closeResult() },
                onRetry: { model.retryFromResult() }
            )
        }
    }
}
