import AVFoundation
import UIKit
#if canImport(RadarpaymentsSDKPayment)
import RadarpaymentsSDKPayment
#else
import PayrdrSdkBridge
#endif

/// The camera the scanner of the payment framework runs on: AVFoundation frames read by Vision.
public final class VisionScannerCamera: NSObject, ScannerCamera {
    public func access() -> ScannerCameraAccess {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: return .granted
        case .notDetermined: return .notAsked
        default: return .denied
        }
    }

    public func requestAccess(onAnswer: @escaping (KotlinBoolean) -> Void) {
        AVCaptureDevice.requestAccess(for: .video) { granted in
            DispatchQueue.main.async { onAnswer(KotlinBoolean(bool: granted)) }
        }
    }

    public func open(onText: @escaping ([ScannerTextLine]) -> Void, onError: @escaping () -> Void) -> ScannerCameraFeed {
        VisionCameraFeed(onText: onText, onError: onError)
    }
}
