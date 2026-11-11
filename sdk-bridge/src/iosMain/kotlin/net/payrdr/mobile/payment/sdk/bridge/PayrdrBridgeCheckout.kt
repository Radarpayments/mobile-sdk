package net.payrdr.mobile.payment.sdk.bridge

import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.ios.PayrdrCheckout
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.withOrderId
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.ScannerCamera
import net.payrdr.mobile.payment.sdk.payment.scanner.provider.MlKitVisionCardScannerProvider
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/**
 * Opens the checkout for a Flutter or React Native plugin over whatever the app shows, and closes it again. Use it on
 * the main thread; one object per plugin instance, one checkout at a time.
 *
 * @param scannerCamera the camera of the card scanner, which the plugin's Swift sources hold (the framework has no
 *   camera code); without it a request with `cardScanner` on opens the checkout without the scan button.
 */
public class PayrdrBridgeCheckout(
    private val scannerCamera: ScannerCamera? = null,
) {
    /** The checkout on screen now; null when none is. */
    private var open: PayrdrCheckout? = null

    /**
     * Opens the checkout of [requestJson] (`{"config": {...}, "mdOrder": "..."}`, see the module README) over [from], or
     * over the key window's screen when it is null. [onResult] gets the result JSON once: after the sheet closes, or at
     * once when it could not open.
     */
    public fun present(
        requestJson: String,
        from: UIViewController?,
        onResult: (String) -> Unit,
    ) {
        if (open?.isPresenting == true) return onResult(CheckoutResultJson.failure(NotOpened.BUSY, "A checkout is already open"))
        val request =
            try {
                CheckoutRequest.parse(requestJson) { scannerCamera?.let { MlKitVisionCardScannerProvider(it) } }
            } catch (e: InvalidBridgeRequest) {
                return onResult(CheckoutResultJson.invalidRequest(e.message.orEmpty()))
            }
        val presenter = from ?: keyWindowRoot()
        val checkout = PayrdrCheckout(request.config)
        val opened =
            presenter != null &&
                when (val order = request.checkout) {
                    is CheckoutConfig.MdOrder ->
                        checkout.present(
                            order.value,
                            presenter,
                        ) { finish(it.withOrderId(request.orderId), onResult) }
                    is CheckoutConfig.SessionId -> checkout.presentSession(order.value, presenter) { finish(it, onResult) }
                }
        if (opened) {
            open = checkout
        } else {
            onResult(CheckoutResultJson.failure(NotOpened.NO_SCREEN, "Nothing on screen to present the checkout over", request.orderId))
        }
    }

    /** Closes the open checkout; its result still comes, with what it had reached. */
    public fun dismiss() {
        open?.dismiss()
    }

    private fun finish(
        result: CheckoutResult,
        onResult: (String) -> Unit,
    ) {
        open = null
        onResult(CheckoutResultJson.encode(result))
    }

    private fun keyWindowRoot(): UIViewController? =
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
            .firstOrNull { it.isKeyWindow() }
            ?.rootViewController
}
