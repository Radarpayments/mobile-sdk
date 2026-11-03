import Combine
import Foundation
import sdk_payment_scanner

/// The SDK configuration of the developer settings' gateway. Read on every payment, so a gateway changed there applies
/// to the next one.
final class SampleStore: ObservableObject {
    func config(sslPinning: SslPinningConfig? = nil) -> SdkPaymentConfig {
        SdkPaymentConfig(
            baseUrl: UserDefaults.standard.string(forKey: AppPrefsKeys.baseUrl) ?? AppPrefsDefaults.baseUrl,
            sslPinning: sslPinning,
            cardScannerProvider: MlKitVisionCardScannerProvider(logTransport: nil)
        )
    }
}
