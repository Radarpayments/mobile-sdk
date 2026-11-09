package net.payrdr.mobile.payment.sdk.payment.ios

import net.payrdr.mobile.payment.sdk.payment.SdkPayment
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import platform.Foundation.NSThread
import platform.UIKit.UIViewController

/**
 * Opens the payment sheet on iOS over any screen and closes it again; the merchant only handles the result. Use it on
 * the main thread.
 *
 * ```swift
 * let checkout = PayrdrCheckout(config: SdkPaymentConfig(baseUrl: baseUrl))
 * checkout.present(mdOrder: mdOrder, from: self) { result in
 *     switch onEnum(of: result) {
 *     case .completed(let paid): showPaid(paid.orderId)
 *     case .canceled: break
 *     case .failed(let failed): showError(failed.error)
 *     case .unknown(let unknown): checkOnServer(unknown.orderId)
 *     }
 * }
 * // or: let result = try await checkout.present(mdOrder: mdOrder, from: self)
 * ```
 * In SwiftUI, use the `payrdrCheckout(isPresented:mdOrder:config:onResult:)` view modifier instead.
 *
 * @param config what the checkout looks like and which gateway it pays through
 */
public class PayrdrCheckout(
    config: SdkPaymentConfig,
) {
    private val sdk = SdkPayment.create(config)

    /** The checkout on screen now; null when none is. */
    private var open: MadeCheckout? = null

    /** A checkout of this object is on screen. */
    public val isPresenting: Boolean get() = open != null

    /**
     * Opens the checkout for an order the merchant's server registered, over the screen [from] belongs to (or whatever
     * is presented on top of it). [onResult] comes once, after the sheet has closed.
     *
     * @return false when nothing opened: a checkout of this object is already open, or there is no screen to present
     *   on (e.g. [from] is not in a window yet); [onResult] is not called then
     */
    public fun present(
        mdOrder: String,
        from: UIViewController,
        onResult: (CheckoutResult) -> Unit,
    ): Boolean = open(CheckoutConfig.MdOrder(mdOrder), from, onResult)

    /** [present] for an API v2 session id (`ps_…`). */
    public fun presentSession(
        sessionId: String,
        from: UIViewController,
        onResult: (CheckoutResult) -> Unit,
    ): Boolean = open(CheckoutConfig.SessionId(sessionId), from, onResult)

    /**
     * Closes the open checkout. Its `onResult` still comes, with what it had reached: its result, a cancel, or unknown
     * if a payment was already sent — then check the order on your server.
     */
    public fun dismiss() {
        val shown = open ?: return
        val controller = shown.viewController
        if (controller.presentingViewController == null) {
            shown.closeWithoutResult()
        } else {
            controller.dismissViewControllerAnimated(true) { shown.closeWithoutResult() }
        }
    }

    private fun open(
        checkout: CheckoutConfig,
        from: UIViewController,
        onResult: (CheckoutResult) -> Unit,
    ): Boolean {
        check(NSThread.isMainThread) { "PayrdrCheckout must be used on the main thread" }
        // Taken away by the app without us noticing: settle it, so this object can open again.
        open?.takeIf { it.viewController.presentingViewController == null }?.closeWithoutResult()
        if (open != null) return false
        var made: MadeCheckout? = null
        made =
            SdkPaymentViewControllerFactory().make(sdk = sdk, checkout = checkout) { result ->
                val controller = made?.viewController
                val finish = {
                    open = null
                    made = null
                    onResult(result)
                }
                // The sheet has already slid away; the transparent controller under it goes without a trace.
                if (controller?.presentingViewController != null) controller.dismissViewControllerAnimated(true, finish) else finish()
            }
        val controller = checkNotNull(made).viewController
        open = made
        // No animation of UIKit's own: the checkout animates its sheet itself.
        presenterFor(from)?.presentViewController(controller, animated = false, completion = null)
        if (controller.presentingViewController == null) {
            // UIKit declined (no window, or everything is on its way out): nothing is on screen, nothing to report.
            open = null
            made = null
            return false
        }
        return true
    }

    /** The controller to present from: the top of what [from]'s window shows, skipping anything being dismissed. */
    private fun presenterFor(from: UIViewController): UIViewController? {
        var top = from.viewIfLoaded?.window?.rootViewController ?: return null
        while (true) {
            val next = top.presentedViewController
            if (next == null || next.isBeingDismissed()) return top
            top = next
        }
    }
}
