package net.payrdr.mobile.payment.sdk.payment.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayAvailability
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayEnvironment
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayRequestBuilder

/** Google Play services answer within a second; a stuck check must not hold the checkout. */
private const val READY_TO_PAY_TIMEOUT_MS = 3_000L

/**
 * Which wallets of [config] the device can pay with, or null while the platform check runs.
 * A button for an unavailable wallet only ends the checkout with an error, so it is not shown.
 */
@Composable
internal fun rememberWalletAvailability(config: SdkPaymentConfig): WalletAvailability? {
    val googlePay = config.googlePay?.takeIf { GooglePayAvailability.isAvailable } ?: return WalletAvailability.None
    val context = LocalContext.current
    return produceState<WalletAvailability?>(null, googlePay) {
        val ready = withTimeoutOrNull(READY_TO_PAY_TIMEOUT_MS) { context.isReadyToPay(googlePay) } ?: false
        value = WalletAvailability(googlePay = ready, applePay = false)
    }.value
}

@Suppress("TooGenericExceptionCaught") // no Play services, bad config, API errors: all mean "can't pay"
private suspend fun Context.isReadyToPay(config: GooglePayConfig): Boolean =
    try {
        // Before the gateway names the environment (a card form shown on its own) the device check runs in TEST.
        val environment =
            when (config.environment ?: GooglePayEnvironment.TEST) {
                GooglePayEnvironment.TEST -> WalletConstants.ENVIRONMENT_TEST
                GooglePayEnvironment.PRODUCTION -> WalletConstants.ENVIRONMENT_PRODUCTION
            }
        val options =
            Wallet.WalletOptions
                .Builder()
                .setEnvironment(environment)
                .build()
        val request = IsReadyToPayRequest.fromJson(GooglePayRequestBuilder.buildIsReadyToPayRequest(config))
        Wallet.getPaymentsClient(this, options).isReadyToPay(request).await() == true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }
