package net.payrdr.mobile.payment.sdk.payment.ios

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.payment.SdkPayment
import net.payrdr.mobile.payment.sdk.payment.applepay.IosApplePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.gpay.NoOpGooglePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.ui.CheckoutPresentation
import net.payrdr.mobile.payment.sdk.payment.ui.PaymentCheckout
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIModalPresentationOverFullScreen
import platform.UIKit.UIModalTransitionStyleCrossDissolve
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.accessibilityElementsHidden
import platform.UIKit.setAccessibilityElementsHidden
import platform.UIKit.setAccessibilityViewIsModal

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
     * @return a `UIViewController` wrapping the Compose tree
     */
    @OptIn(ExperimentalComposeUiApi::class) // ComposeUIViewControllerConfiguration.opaque
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
        // We need a reference to the resulting VC to use as the presenter for WKWebView challenges.
        // Kotlin lets us capture it after construction by using a late-bound holder.
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

        val vc =
            ComposeUIViewController(configure = { opaque = false }) {
                // Presented directly (`present(vc)`) the controller covers the merchant screen
                // transparently and the checkout draws its own sheet; embedded in another controller
                // (e.g. a SwiftUI `.sheet`) the container is already a sheet, so steps fill it.
                // The hierarchy is only settled once the view is on screen — decide on the first frame.
                var presentation by remember { mutableStateOf<CheckoutPresentation?>(null) }
                var hidden by remember { mutableStateOf<List<UIView>>(emptyList()) }
                DisposableEffect(Unit) { onDispose { hidden.forEach { it.setAccessibilityElementsHidden(false) } } }
                LaunchedEffect(Unit) {
                    withFrameNanos { }
                    presentation =
                        if (presentedVc?.parentViewController == null) CheckoutPresentation.Sheet else CheckoutPresentation.Page
                    hidden = presentedVc?.let(::hideWhatLiesUnder).orEmpty()
                }
                // Taken off screen without a result (the merchant dismissed it): the merchant still gets an outcome,
                // and the work stops.
                DisposableEffect(Unit) {
                    onDispose {
                        deliver(controller.resultIfClosedNow())
                        scope.cancel()
                    }
                }
                presentation?.let {
                    PaymentCheckout(
                        config = sdk.config,
                        controller = controller,
                        onResult = deliver,
                        presentation = it,
                    )
                }
            }
        vc.view.backgroundColor = UIColor.clearColor
        // The merchant screen stays visible under the transparent sheet; VoiceOver must not reach it.
        vc.view.setAccessibilityViewIsModal(true)
        vc.modalPresentationStyle = UIModalPresentationOverFullScreen
        vc.modalTransitionStyle = UIModalTransitionStyleCrossDissolve
        presentedVc = vc
        scope.launch(Dispatchers.Main) {
            controller.handle(PaymentIntent.Start(checkout))
        }
        return MadeCheckout(viewController = vc, closeWithoutResult = { deliver(controller.resultIfClosedNow()) })
    }

    /**
     * Keeps VoiceOver off the merchant screen under the transparent checkout: presented over the full screen, the
     * presenting controller's view stays on screen, and a modal view hides only its own siblings, not it. Returns the
     * views hidden, to show again when the checkout goes.
     */
    private fun hideWhatLiesUnder(vc: UIViewController): List<UIView> =
        listOfNotNull(vc.presentingViewController?.view)
            .filter { !it.accessibilityElementsHidden }
            .onEach { it.setAccessibilityElementsHidden(true) }

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
