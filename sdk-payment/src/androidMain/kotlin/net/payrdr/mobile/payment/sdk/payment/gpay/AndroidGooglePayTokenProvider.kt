package net.payrdr.mobile.payment.sdk.payment.gpay

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import kotlinx.coroutines.CompletableDeferred
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

/**
 * Android actual of [GooglePayTokenProvider]. Must be constructed from a [ComponentActivity]
 * (because it registers an [ActivityResultLauncher] against the activity's
 * [androidx.activity.result.ActivityResultRegistry]).
 *
 * Lifecycle:
 *  1. Merchant creates the provider in `Activity.onCreate` (or lazily via `activity.lazy { ... }`
 *     before `onStart`).
 *  2. [requestPaymentToken] suspends, triggers the GPay sheet, and resumes with a
 *     [GooglePayTokenResult] on sheet dismissal.
 *  3. The launcher is automatically unregistered when the activity is destroyed.
 *
 * Concurrency: only one GPay request may be in flight at a time. A second concurrent call
 * returns [GooglePayTokenResult.Failure] with message "GPay request already in flight"
 * instead of clobbering the first deferred.
 */
public class AndroidGooglePayTokenProvider(
    private val activity: ComponentActivity,
) : GooglePayTokenProvider {
    private val pending: AtomicReference<CompletableDeferred<GooglePayTokenResult>?> =
        AtomicReference(null)

    // ComponentActivity is the host — the Fragment-version check does not apply here.
    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            handleResult(result)
        }

    @Suppress("ReturnCount")
    override suspend fun requestPaymentToken(
        config: GooglePayConfig,
        amountMinorUnits: Long,
        currencyCode: String,
        countryCode: String,
    ): GooglePayTokenResult {
        if (!GooglePayAvailability.isAvailable) return GooglePayTokenResult.NotAvailable

        val deferred = CompletableDeferred<GooglePayTokenResult>()
        if (!pending.compareAndSet(null, deferred)) {
            return GooglePayTokenResult.Failure("GPay request already in flight")
        }

        val resolvedCountry = config.countryCode ?: countryCode
        val totalPriceMajor = formatMajor(amountMinorUnits)

        // Inside the try: a request the builder rejects must release `pending`, or every later tap is "in flight".
        return try {
            val json =
                GooglePayRequestBuilder.buildPaymentDataRequest(
                    config = config,
                    totalPriceMajor = totalPriceMajor,
                    currencyCode = currencyCode,
                    countryCode = resolvedCountry,
                )
            val paymentsClient = createPaymentsClient(requireNotNull(config.environment) { "GooglePayConfig.environment is not known" })
            val request = PaymentDataRequest.fromJson(json)
            val task = paymentsClient.loadPaymentData(request)
            task.addOnCompleteListener { completed ->
                when {
                    completed.isSuccessful -> {
                        val paymentData = completed.result
                        resolveSuccessFor(deferred, paymentData)
                    }
                    else -> {
                        val ex = completed.exception
                        when (ex) {
                            is com.google.android.gms.common.api.ResolvableApiException -> {
                                runCatching {
                                    val sender: IntentSender = ex.resolution.intentSender
                                    launcher.launch(IntentSenderRequest.Builder(sender).build())
                                }.onFailure { resumeWith(deferred, GooglePayTokenResult.Failure("Launcher failure", it)) }
                            }
                            is ApiException ->
                                resumeWith(
                                    deferred,
                                    when (ex.statusCode) {
                                        CommonStatusCodes.CANCELED -> GooglePayTokenResult.Cancelled
                                        else -> GooglePayTokenResult.Failure("GPay ApiException ${ex.statusCode}: ${ex.message}", ex)
                                    },
                                )
                            null -> resumeWith(deferred, GooglePayTokenResult.Failure("Unknown GPay failure"))
                            else -> resumeWith(deferred, GooglePayTokenResult.Failure("GPay failure: ${ex.message}", ex))
                        }
                    }
                }
            }
            try {
                deferred.await()
            } finally {
                pending.compareAndSet(deferred, null)
            }
        } catch (t: Throwable) {
            pending.compareAndSet(deferred, null)
            GooglePayTokenResult.Failure("GPay requestPaymentToken threw", t)
        }
    }

    private fun handleResult(activityResult: ActivityResult) {
        val deferred = pending.get() ?: return // nothing in-flight; drop silently
        val data: Intent? = activityResult.data
        when (activityResult.resultCode) {
            android.app.Activity.RESULT_OK -> {
                val paymentData = data?.let { PaymentData.getFromIntent(it) }
                if (paymentData != null) {
                    resolveSuccessFor(deferred, paymentData)
                } else {
                    resumeWith(deferred, GooglePayTokenResult.Failure("Missing PaymentData in result"))
                }
            }
            android.app.Activity.RESULT_CANCELED -> resumeWith(deferred, GooglePayTokenResult.Cancelled)
            AutoResolveHelper.RESULT_ERROR -> {
                val status = AutoResolveHelper.getStatusFromIntent(data)
                resumeWith(
                    deferred,
                    GooglePayTokenResult.Failure("GPay returned error status ${status?.statusCode}: ${status?.statusMessage}"),
                )
            }
            else -> resumeWith(deferred, GooglePayTokenResult.Failure("Unknown launcher resultCode ${activityResult.resultCode}"))
        }
    }

    private fun resolveSuccessFor(
        deferred: CompletableDeferred<GooglePayTokenResult>,
        data: PaymentData,
    ) {
        val json =
            data.toJson() ?: run {
                resumeWith(deferred, GooglePayTokenResult.Failure("PaymentData.toJson() returned null"))
                return
            }
        val token =
            runCatching {
                JSONObject(json)
                    .getJSONObject("paymentMethodData")
                    .getJSONObject("tokenizationData")
                    .getString("token")
            }.getOrNull()
        if (token.isNullOrBlank()) {
            resumeWith(deferred, GooglePayTokenResult.Failure("Missing paymentMethodData.tokenizationData.token"))
        } else {
            resumeWith(deferred, GooglePayTokenResult.Success(token))
        }
    }

    private fun resumeWith(
        deferred: CompletableDeferred<GooglePayTokenResult>,
        result: GooglePayTokenResult,
    ) {
        // complete returns false if already completed; harmless — avoids double-resume surprise
        deferred.complete(result)
    }

    private fun createPaymentsClient(env: GooglePayEnvironment): PaymentsClient =
        Wallet.getPaymentsClient(
            activity,
            Wallet.WalletOptions
                .Builder()
                .setEnvironment(
                    when (env) {
                        GooglePayEnvironment.TEST -> WalletConstants.ENVIRONMENT_TEST
                        GooglePayEnvironment.PRODUCTION -> WalletConstants.ENVIRONMENT_PRODUCTION
                    },
                ).build(),
        )

    private fun formatMajor(minor: Long): String {
        // NOTE: hardcoded 2-decimal formatting. Currencies with non-2 fractional digits
        // (JPY=0, KWD/BHD/IQD/JOD/OMR/TND=3) will produce wrong totalPrice.
        val whole = minor / 100
        val fraction = (minor % 100).let { if (it < 0) -it else it }
        val frac = fraction.toString().padStart(2, '0')
        return "$whole.$frac"
    }
}
