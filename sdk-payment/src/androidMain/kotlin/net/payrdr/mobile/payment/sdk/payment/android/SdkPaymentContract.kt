package net.payrdr.mobile.payment.sdk.payment.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResultCodec

/**
 * `ActivityResultContract` for launching the turnkey SDK checkout.
 *
 * Input: the pre-registered run id (obtained via `SdkPaymentRegistry.register(...)`) — the full
 * `SdkPaymentRunSpec` cannot travel through an Intent because it holds non-Parcelable references
 * (Ktor HttpClient, CoroutineScope, etc).
 * Output: the terminal [CheckoutResult].
 */
internal object SdkPaymentContract : ActivityResultContract<SdkPaymentRun, CheckoutResult>() {
    override fun createIntent(
        context: Context,
        input: SdkPaymentRun,
    ): Intent =
        Intent(context, SdkPaymentActivity::class.java).apply {
            putExtra(SdkPaymentActivity.EXTRA_RUN_ID, input.runId)
            putExtra(SdkPaymentActivity.EXTRA_RESTORABLE, input.restorable)
        }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): CheckoutResult {
        // The checkout closed without a result (e.g. the system finished it): how the payment ended is not known.
        if (resultCode != Activity.RESULT_OK || intent == null) return CheckoutResultCodec.closedWithoutResult()
        val extras = intent.extras ?: return CheckoutResultCodec.closedWithoutResult()
        return CheckoutResultCodec.decode(extras.keySet().mapNotNull { key -> extras.getString(key)?.let { key to it } }.toMap())
    }
}

/**
 * One checkout launch: [runId] finds its in-memory spec in [SdkPaymentRegistry]; [restorable] (see `RestorableCheckout`)
 * rebuilds it when Android ended the process and the spec is gone.
 */
internal data class SdkPaymentRun(
    val runId: String,
    val restorable: String,
)
