import RadarpaymentsSDKPayment
import UIKit

/// Opens `checkout` for `order` over what `root`'s window shows: by its session id when there is one, by its mdOrder
/// otherwise. False when nothing opened (no window, or this checkout is already on screen); `onResult` is not called
/// then.
@MainActor
func presentCheckout(
    _ checkout: PayrdrCheckout,
    order: OpenOrder,
    from root: UIViewController?,
    onResult: @escaping (CheckoutResult) -> Void
) -> Bool {
    guard let root else { return false }
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
