package net.payrdr.mobile.payment.sdk.payment.presentation

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.flow.PaymentFlowController
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerProvider
import net.payrdr.mobile.payment.sdk.payment.ui.components.deviceWalletAvailability
import net.payrdr.mobile.payment.sdk.payment.ui.loading.UserDefaultsStore
import platform.Foundation.NSClassFromString
import platform.Foundation.NSLocale
import platform.Foundation.NSSelectorFromString
import platform.Foundation.preferredLanguages
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.NSObjectMeta

/**
 * One checkout as its SwiftUI screens see it: what to draw with ([theme], [strings]) and, once the screen knows how
 * it is presented, the [CheckoutModel] to draw.
 *
 * @property theme the merchant's theme in plain numbers.
 * @property strings the checkout's texts in its language.
 * @property rightToLeft the checkout's language is laid out right to left.
 */
@PayrdrUiApi
public class CheckoutSession internal constructor(
    private val config: SdkPaymentConfig,
    private val controller: PaymentFlowController,
    private val scope: CoroutineScope,
    private val deliver: (CheckoutResult) -> Unit,
) {
    public val theme: ThemeSpec = ThemeSpec.of(config.theme)
    public val strings: PaymentStrings =
        PaymentStrings(resolveLanguage(config.locale, NSLocale.preferredLanguages.firstOrNull() as? String ?: "en"))
    public val rightToLeft: Boolean = isRtl(strings.language)

    /** The card scanner the integrator configured. The SwiftUI form offers scanning when it is a [NativeCardScanner]. */
    public val cardScannerProvider: CardScannerProvider? = config.cardScannerProvider

    private var model: CheckoutModel? = null

    /**
     * The checkout's model, made on the first call: [sheetPresentation] says whether the checkout draws its own sheet
     * over the app's screen or fills a container the app put it in, which is known only once the view is on screen.
     */
    public fun start(sheetPresentation: Boolean): CheckoutModel =
        model ?: CheckoutModel(
            config = config,
            controller = controller,
            store = UserDefaultsStore(),
            wallets = deviceWalletAvailability(config),
            sheetPresentation = sheetPresentation,
            reduceMotion = { UIAccessibilityIsReduceMotionEnabled() },
            scope = scope,
            onResult = deliver,
        ).also { model = it }

    /** Calls [onChange] with what [model] shows now and after every change, on the main thread, until the checkout ends. */
    public fun observe(
        model: CheckoutModel,
        onChange: (CheckoutUi) -> Unit,
    ) {
        scope.launch { model.ui.collect { onChange(it) } }
    }

    /** The screen was taken away: the app gets what the checkout had reached, unless it already has a result. */
    public fun closed() {
        model?.dispose()
        deliver(controller.resultIfClosedNow())
    }
}

/** Draws a checkout; the SwiftUI screens compiled into the framework are one. */
@PayrdrUiApi
public interface CheckoutRenderer {
    /** The view controller showing [session], transparent around what it draws. */
    public fun viewController(session: CheckoutSession): UIViewController
}

/** Where the Swift side of the framework leaves its renderer for the Kotlin side to find. */
@PayrdrUiApi
public object CheckoutRenderers {
    /** The SwiftUI screens; set by the Swift class named [SWIFT_RENDERER_CLASS] when asked to install itself. */
    public var swiftUi: CheckoutRenderer? = null

    /**
     * The SwiftUI renderer when the framework has one. Kotlin cannot name a Swift type of its own framework, so the
     * class is found by name and installs itself through [swiftUi].
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    internal fun resolve(): CheckoutRenderer? {
        if (swiftUi == null) {
            val installer = (NSClassFromString(SWIFT_RENDERER_CLASS) as? NSObjectMeta)?.new() as? NSObject
            installer?.performSelector(NSSelectorFromString("install"))
        }
        return swiftUi
    }

    private const val SWIFT_RENDERER_CLASS = "PayrdrSwiftUICheckoutRenderer"
}
