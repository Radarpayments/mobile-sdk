import Flutter
import PayrdrSdkBridge
import UIKit

/// Passes the Dart side's JSON to the SDK's bridge and its answers back; all mapping lives in sdk-bridge. The sheet opens
/// over whatever the app's key window shows.
public final class PayrdrSdkPlugin: NSObject, FlutterPlugin {
    private let checkout = PayrdrBridgeCheckout()

    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "net.payrdr.sdk", binaryMessenger: registrar.messenger())
        registrar.addMethodCallDelegate(PayrdrSdkPlugin(), channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let request = call.arguments as? String else {
            result(FlutterError(code: "invalid_request", message: "Expected a JSON string", details: nil))
            return
        }
        switch call.method {
        case "present":
            checkout.present(requestJson: request, from: nil) { result($0) }
        case "coreToken":
            result(PayrdrBridge.shared.coreToken(requestJson: request))
        default:
            result(FlutterMethodNotImplemented)
        }
    }
}
