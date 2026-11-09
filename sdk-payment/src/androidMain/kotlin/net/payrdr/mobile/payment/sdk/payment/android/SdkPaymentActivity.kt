package net.payrdr.mobile.payment.sdk.payment.android

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.payment.SdkPayment
import net.payrdr.mobile.payment.sdk.payment.applepay.NoOpApplePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.RestorableCheckout
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.gpay.AndroidGooglePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayAvailability
import net.payrdr.mobile.payment.sdk.payment.gpay.NoOpGooglePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResultCodec
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.ui.CheckoutPresentation
import net.payrdr.mobile.payment.sdk.payment.ui.PaymentCheckout
import java.util.concurrent.ConcurrentHashMap

/**
 * Spec of one checkout run; passed from the merchant Activity to [SdkPaymentActivity] via the
 * [SdkPaymentRegistry] since these references are not safely Parcelable.
 */
internal class SdkPaymentRunSpec(
    val sdk: SdkPayment,
    val checkout: CheckoutConfig,
)

/**
 * Thread-safe registry keyed by the `runId` that `SdkPaymentContract` puts in the launch intent.
 */
internal object SdkPaymentRegistry {
    private val map: ConcurrentHashMap<String, SdkPaymentRunSpec> = ConcurrentHashMap()

    fun register(
        runId: String,
        spec: SdkPaymentRunSpec,
    ) {
        map[runId] = spec
    }

    fun take(runId: String): SdkPaymentRunSpec? = map.remove(runId)
}

/**
 * Turnkey Activity that hosts the SDK checkout UI. The merchant does not instantiate this directly —
 * open it through [PayrdrCheckout] instead.
 */
internal class SdkPaymentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runId =
            intent.getStringExtra(EXTRA_RUN_ID)
                ?: return finishWithNoSpec("Missing run id")
        // Recreated (Android ended the process, or a configuration change): the in-memory spec is gone, so the checkout
        // is rebuilt from the intent and starts over — the gateway says whether the order was paid meanwhile.
        val restored = savedInstanceState != null
        val spec =
            (if (restored) null else SdkPaymentRegistry.take(runId))
                ?: restoredSpec()
                ?: return finishWithNoSpec("Run spec not registered for id=$runId")

        val webChallengeHost = AndroidWebChallengeHost(this)
        val controller =
            spec.sdk.newFlow(
                webChallengeHost = webChallengeHost,
                scope = lifecycleScope,
                // Registers its activity-result launcher, so it has to be created here in onCreate.
                googlePayTokenProvider =
                    if (spec.sdk.config.googlePay != null && GooglePayAvailability.isAvailable) {
                        AndroidGooglePayTokenProvider(this)
                    } else {
                        NoOpGooglePayTokenProvider
                    },
                applePayTokenProvider = NoOpApplePayTokenProvider,
            )

        // Transparent, edge-to-edge window: the merchant screen stays visible under the sheet's scrim.
        // The legacy translucent theme does not draw the system bar backgrounds, so the system would
        // paint opaque black bars (with dark icons on them once edge-to-edge asks for light bars).
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            PaymentCheckout(
                config = spec.sdk.config,
                controller = controller,
                onResult = { result -> finishWithResult(result) },
                presentation = CheckoutPresentation.Sheet,
            )
        }

        controller.restored = restored
        lifecycleScope.launch {
            controller.handle(PaymentIntent.Start(spec.checkout))
        }
    }

    /** The run rebuilt from the intent: a new SdkPayment on the config it was opened with. */
    private fun restoredSpec(): SdkPaymentRunSpec? {
        val restorable = intent.getStringExtra(EXTRA_RESTORABLE) ?: return null
        val (config, checkout) = runCatching { RestorableCheckout.decode(restorable) }.getOrNull() ?: return null
        return SdkPaymentRunSpec(sdk = SdkPayment.create(config), checkout = checkout)
    }

    private fun finishWithResult(result: CheckoutResult) {
        setResult(RESULT_OK, result.toResultIntent())
        finish()
    }

    private fun finishWithNoSpec(reason: String) {
        setResult(RESULT_OK, CheckoutResult.Failed(orderId = "", error = SdkException(reason)).toResultIntent())
        finish()
    }

    companion object {
        internal const val EXTRA_RUN_ID: String = "net.payrdr.sdk.payment.RUN_ID"
        internal const val EXTRA_RESTORABLE: String = "net.payrdr.sdk.payment.RESTORABLE"
    }
}

/** The activity result for [this]: what [SdkPaymentContract.parseResult] reads back. */
internal fun CheckoutResult.toResultIntent(): android.content.Intent =
    android.content.Intent().apply {
        CheckoutResultCodec.encode(this@toResultIntent).forEach { (key, value) -> putExtra(key, value) }
    }
