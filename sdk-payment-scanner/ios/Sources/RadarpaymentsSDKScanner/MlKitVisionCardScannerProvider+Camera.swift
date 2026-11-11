import Foundation
#if canImport(RadarpaymentsSDKPayment)
import RadarpaymentsSDKPayment
#else
import PayrdrSdkBridge
#endif

public extension MlKitVisionCardScannerProvider {
    /// The card scanner of the checkout, reading the card with the device's camera and Vision. Pass it to
    /// `SdkPaymentConfig.cardScannerProvider`; the app needs `NSCameraUsageDescription` in its Info.plist.
    ///
    /// - Parameter logTransport: an optional sink for scanner events, usually the one of the SDK config.
    convenience init(logTransport: LogTransport? = nil) {
        self.init(camera: VisionScannerCamera(), logTransport: logTransport)
    }
}
