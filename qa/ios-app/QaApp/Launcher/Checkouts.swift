import RadarpaymentsSDKPayment
import UIKit

/// The app's checkout: one `PayrdrCheckout` kept while its configuration and profile stay the same, so that the SDK's
/// own guard refuses a second sheet while one is on screen.
@MainActor
final class Checkouts {
    private let journal: Journal
    // One provider for the app's lifetime: the camera session it owns is not cheap to set up.
    private let scanner: MlKitVisionCardScannerProvider
    private var checkout: PayrdrCheckout?
    private var key: String?

    init(journal: Journal) {
        self.journal = journal
        scanner = MlKitVisionCardScannerProvider(logTransport: journal)
    }

    func open(
        _ order: OpenOrder,
        _ config: QaConfig,
        _ profile: StandProfile,
        onResult: @escaping (CheckoutResult) -> Void
    ) -> Bool {
        let checkout = checkoutFor(config, profile)
        return presentCheckout(checkout, order: order, from: UIApplication.shared.keyWindowRoot) { result in
            // The SDK calls back on the main thread, through a closure Kotlin does not mark as main-actor.
            MainActor.assumeIsolated { onResult(result) }
        }
    }

    // SslPinningConfig has no equality, so the cache is keyed on what the SDK config is built from, written out and
    // compared by scalars: Swift's `==` would take canonically equivalent texts for the same.
    private func checkoutFor(_ config: QaConfig, _ profile: StandProfile) -> PayrdrCheckout {
        let wanted = QaConfigJson.encode(config) + "\n" + StandProfileJson.encode(profile)
        if let checkout, checkout.isPresenting || key.map({ sameText($0, wanted) }) == true {
            return checkout
        }
        let made = PayrdrCheckout(config: sdkPaymentConfig(config: config, profile: profile, journal: journal, scanner: scanner))
        checkout = made
        key = wanted
        return made
    }
}
