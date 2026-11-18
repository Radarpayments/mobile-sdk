import RadarpaymentsSDKPayment
import UIKit

/// Opens the checkout for `order` over whatever the key window shows: by its session id when there is one, by its
/// mdOrder otherwise. False when nothing opened (no window, or a checkout is already on screen); `onResult` is not
/// called then.
@MainActor
func presentCheckout(config: SdkPaymentConfig, order: OpenOrder, onResult: @escaping (CheckoutResult) -> Void) -> Bool {
    guard let root = UIApplication.shared.keyWindowRoot else { return false }
    let checkout = PayrdrCheckout(config: config)
    if let sessionId = order.sessionId {
        return checkout.presentSession(sessionId: sessionId, from: root, onResult: onResult)
    }
    return checkout.present(mdOrder: order.mdOrder ?? "", from: root, onResult: onResult)
}

extension UIApplication {
    /// The root controller of the foreground key window.
    var keyWindowRoot: UIViewController? {
        connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
    }
}
