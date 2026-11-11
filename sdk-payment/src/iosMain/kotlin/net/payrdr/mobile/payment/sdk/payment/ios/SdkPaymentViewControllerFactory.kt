package net.payrdr.mobile.payment.sdk.payment.ios

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.payment.SdkPayment
import net.payrdr.mobile.payment.sdk.payment.applepay.IosApplePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.gpay.NoOpGooglePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.presentation.CheckoutRenderers
import net.payrdr.mobile.payment.sdk.payment.presentation.CheckoutSession
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

/**
 * Builds the `UIViewController` of the turnkey checkout. Merchants open it through [PayrdrCheckout] (UIKit) or the
 * `payrdrCheckout` SwiftUI modifier, which present it over their screen and close it again.
 */
internal class SdkPaymentViewControllerFactory {
    /**
     * Build a modally-presentable view controller.
     *
     * @param sdk SDK instance; it brings its own network layer
     * @param checkout order/session to check out
     * @param onResult terminal callback (invoked on the main thread); the checkout's work stops after it returns
     * @return the SwiftUI checkout's controller, transparent around its sheet
     */
    fun make(
        sdk: SdkPayment,
        checkout: CheckoutConfig,
        onResult: (CheckoutResult) -> Unit,
    ): MadeCheckout {
        // The checkout's own scope: it lives as long as the checkout and ends once the merchant has the result.
        val scope = MainScope()
        // Exactly one result: the checkout's own, or — if it goes away without one — what it had reached by then.
        var delivered = false
        val deliver = { result: CheckoutResult ->
            if (!delivered) {
                delivered = true
                try {
                    onResult(result)
                } finally {
                    scope.cancel()
                }
            }
        }
        // The bank's page is presented from the checkout's controller, which exists only after the flow is made.
        var presentedVc: UIViewController? = null
        val webChallengeHost =
            IosWebChallengeHost(
                presentingViewControllerProvider = {
                    presentedVc ?: topMostViewController()
                },
            )
        val controller =
            sdk.newFlow(
                webChallengeHost = webChallengeHost,
                scope = scope,
                googlePayTokenProvider = NoOpGooglePayTokenProvider,
                applePayTokenProvider = IosApplePayTokenProvider(),
            )
        val renderer = checkNotNull(CheckoutRenderers.resolve()) { "The SwiftUI checkout is missing from the framework" }
        val vc = renderer.viewController(CheckoutSession(sdk.config, controller, scope, deliver))
        presentedVc = vc
        scope.launch(Dispatchers.Main) {
            controller.handle(PaymentIntent.Start(checkout))
        }
        return MadeCheckout(viewController = vc, closeWithoutResult = { deliver(controller.resultIfClosedNow()) })
    }

    /**
     * Fallback presenter resolver — the foreground key-window's root VC chain descended to the
     * topmost presented controller. Used only when the VC built by [make] has not yet been attached
     * to a window at the moment [IosWebChallengeHost] needs a presenter.
     */
    private fun topMostViewController(): UIViewController {
        var vc =
            UIApplication.sharedApplication.keyWindow?.rootViewController
                ?: error("No key window — cannot present 3DS controller")
        while (vc.presentedViewController != null) {
            vc = vc.presentedViewController!!
        }
        return vc
    }
}

/**
 * A checkout [viewController] ready to present, and [closeWithoutResult]: hands the merchant what the checkout had
 * reached (its result, unknown while a payment is in flight, or a cancel) when it is taken away unfinished.
 */
internal class MadeCheckout(
    val viewController: UIViewController,
    val closeWithoutResult: () -> Unit,
)
