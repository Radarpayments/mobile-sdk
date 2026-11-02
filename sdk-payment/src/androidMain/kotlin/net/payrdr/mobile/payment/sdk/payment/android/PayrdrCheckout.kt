package net.payrdr.mobile.payment.sdk.payment.android

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import net.payrdr.mobile.payment.sdk.payment.SdkPayment
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.RestorableCheckout
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.withOrderId
import java.util.UUID

/**
 * Opens the payment sheet on Android, from any screen: build it with [Builder] in an `Activity` or `Fragment`, or with
 * [rememberPayrdrCheckout] in Compose, then call [present] when the buyer taps "Pay".
 *
 * ```kotlin
 * class CartActivity : AppCompatActivity() {
 *     private val checkout = PayrdrCheckout.Builder(config) { result -> handle(result) }.build(this)
 *
 *     fun onPayClick(mdOrder: String) = checkout.present(mdOrder)
 * }
 * ```
 *
 * The result comes back through the activity result, so it reaches the screen even after a rotation recreates it.
 */
public class PayrdrCheckout internal constructor(
    private val sdk: () -> SdkPayment,
    private val onResult: () -> (CheckoutResult) -> Unit,
    private val presented: PresentedCheckout,
) {
    internal lateinit var launcher: ActivityResultLauncher<SdkPaymentRun>

    /** Opens the checkout for an order the merchant's server registered (`mdOrder`). Does nothing while one is open. */
    @MainThread
    public fun present(mdOrder: String): Unit = open(CheckoutConfig.MdOrder(mdOrder), orderId = mdOrder)

    /** Opens the checkout for an API v2 session id (`ps_…`). Does nothing while one is open. */
    @MainThread
    public fun presentSession(sessionId: String): Unit = open(CheckoutConfig.SessionId(sessionId), orderId = "")

    /** @param orderId the `mdOrder` a result without one is given; empty when only a session id is known */
    private fun open(
        checkout: CheckoutConfig,
        orderId: String,
    ) {
        if (presented.value != null) return
        presented.value = orderId
        val runId = UUID.randomUUID().toString()
        val sdk = sdk()
        SdkPaymentRegistry.register(runId, SdkPaymentRunSpec(sdk = sdk, checkout = checkout))
        try {
            launcher.launch(SdkPaymentRun(runId, RestorableCheckout.encode(sdk.config, checkout)))
        } catch (e: RuntimeException) {
            // Not opened (e.g. a fragment not attached yet): free again, and the run must not wait in the registry.
            presented.value = null
            SdkPaymentRegistry.take(runId)
            throw e
        }
    }

    internal fun deliver(result: CheckoutResult) {
        val orderId = presented.value.orEmpty()
        presented.value = null
        onResult()(result.withOrderId(orderId))
    }

    /**
     * Builds a [PayrdrCheckout] for a screen that is not written in Compose.
     *
     * @param config what the checkout looks like and which gateway it pays through
     * @param onResult gets the outcome of every checkout this object opens
     */
    public class Builder(
        private val config: SdkPaymentConfig,
        private val onResult: (CheckoutResult) -> Unit,
    ) {
        /**
         * Call it unconditionally while [host] (a `ComponentActivity` or a `Fragment`) is being created — in a field
         * or in `onCreate`: it registers an activity result, which Android allows only before the screen starts.
         */
        public fun build(host: ActivityResultCaller): PayrdrCheckout {
            val state = (host as? LifecycleOwner)?.lifecycle?.currentState
            check(state == null || !state.isAtLeast(Lifecycle.State.STARTED)) {
                "PayrdrCheckout.Builder.build() must be called before the activity or fragment starts: " +
                    "create it in a field or in onCreate"
            }
            val sdk = SdkPayment.create(config)
            val presented = (host as? SavedStateRegistryOwner)?.let { SavedStatePresented(it.savedStateRegistry) } ?: InMemoryPresented()
            val checkout = PayrdrCheckout(sdk = { sdk }, onResult = { onResult }, presented = presented)
            checkout.launcher = host.registerForActivityResult(SdkPaymentContract) { checkout.deliver(it) }
            return checkout
        }
    }
}

/**
 * A [PayrdrCheckout] for a Compose screen. A new [config] takes effect on the next [PayrdrCheckout.present]. Call it in
 * a composable that stays on screen while the checkout is open: the result is delivered to it.
 *
 * @param config what the checkout looks like and which gateway it pays through
 * @param onResult gets the outcome of every checkout this object opens
 */
@Composable
public fun rememberPayrdrCheckout(
    config: SdkPaymentConfig,
    onResult: (CheckoutResult) -> Unit,
): PayrdrCheckout {
    val sdk = remember(config) { SdkPayment.create(config) }
    val currentSdk by rememberUpdatedState(sdk)
    val currentOnResult by rememberUpdatedState(onResult)
    val presented = rememberSaveable { mutableStateOf<String?>(null) }
    val checkout =
        remember {
            PayrdrCheckout(
                sdk = { currentSdk },
                onResult = { currentOnResult },
                presented =
                    object : PresentedCheckout {
                        override var value: String? by presented
                    },
            )
        }
    checkout.launcher = rememberLauncherForActivityResult(SdkPaymentContract) { checkout.deliver(it) }
    return checkout
}

/**
 * Whether a checkout is open, and the `mdOrder` it pays ("" when only a session id is known); null when none is.
 * Kept across a recreation of the merchant screen, which the translucent sheet over it does not prevent.
 */
internal interface PresentedCheckout {
    var value: String?
}

internal class InMemoryPresented : PresentedCheckout {
    override var value: String? = null
}

/** [PresentedCheckout] in the screen's saved state; restored lazily, as it is restored after the field that builds it. */
private class SavedStatePresented(
    private val registry: SavedStateRegistry,
) : PresentedCheckout {
    // One key per PayrdrCheckout of the screen, in build order, so a recreated screen finds its own again.
    private val key = generateSequence(0) { it + 1 }.map { "$KEY_PREFIX$it" }.first { registry.getSavedStateProvider(it) == null }
    private var restored = false
    private var current: String? = null

    init {
        registry.registerSavedStateProvider(key) { Bundle().apply { putString(KEY_VALUE, current) } }
    }

    override var value: String?
        get() {
            restore()
            return current
        }
        set(newValue) {
            restore()
            current = newValue
        }

    private fun restore() {
        if (restored || !registry.isRestored) return
        restored = true
        registry.consumeRestoredStateForKey(key)?.let { current = it.getString(KEY_VALUE) }
    }

    private companion object {
        const val KEY_PREFIX = "net.payrdr.sdk.payment.checkout."
        const val KEY_VALUE = "presented"
    }
}
