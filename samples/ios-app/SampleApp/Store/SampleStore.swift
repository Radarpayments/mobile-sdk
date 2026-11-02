import Combine
import Foundation
import SwiftUI
import sdk_payment_scanner

/// Holds the latest `SdkPayment` instance, rebuilt on config-affecting pref changes.
/// Views observe this and re-render when `sdk` changes identity.
final class SampleStore: ObservableObject {
    @Published private(set) var sdk: SdkPayment

    @AppStorage(AppPrefsKeys.baseUrl) var baseUrl: String = AppPrefsDefaults.baseUrl {
        didSet { rebuild() }
    }

    init() {
        let cfg = SdkPaymentConfig(
            baseUrl: AppPrefsDefaults.baseUrl,
            cardScannerProvider: MlKitVisionCardScannerProvider(logger: nil)
        )
        let s = SdkPayment.companion.create(config: cfg)
        self.sdk = s
    }

    /// Rebuilds `sdk` using the current baseUrl and the optional overrides passed in.
    func useConfig(theme: (any PaymentTheme)? = nil, locale: String? = nil,
                   applePay: ApplePayConfig? = nil, sslPinning: SslPinningConfig? = nil) {
        let cfg = SdkPaymentConfig(
            baseUrl: baseUrl,
            sslPinning: sslPinning,
            locale: locale,
            theme: theme ?? PaymentThemeSystem.shared,
            applePay: applePay,
            cardScannerProvider: MlKitVisionCardScannerProvider(logger: nil)
        )
        let s = SdkPayment.companion.create(config: cfg)
        self.sdk = s
    }

    private func rebuild() { useConfig() }
}
