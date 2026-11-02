package net.payrdr.mobile.payment.sdk.payment.android

import android.content.Context
import android.content.Intent
import androidx.annotation.MainThread
import net.payrdr.mobile.payment.sdk.payment.SdkPayment
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.RestorableCheckout
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.withOrderId
import java.util.UUID

/**
 * Opens the payment sheet with `startActivityForResult`, for a screen that cannot register an activity result: a plain
 * `Activity`, or the host of a cross-platform app such as Flutter or React Native. Anywhere else use [PayrdrCheckout].
 *
 * ```kotlin
 * activity.startActivityForResult(PayrdrCheckoutIntent.create(activity, config, mdOrder), REQUEST_CODE)
 *
 * override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
 *     if (requestCode == REQUEST_CODE) handle(PayrdrCheckoutIntent.parseResult(resultCode, data, mdOrder))
 * }
 * ```
 */
public object PayrdrCheckoutIntent {
    /** The intent that opens the checkout for an order the merchant's server registered (`mdOrder`). */
    @MainThread
    public fun create(
        context: Context,
        config: SdkPaymentConfig,
        mdOrder: String,
    ): Intent = SdkPaymentContract.createIntent(context, run(config, CheckoutConfig.MdOrder(mdOrder)))

    /** The intent that opens the checkout for an API v2 session id (`ps_…`). */
    @MainThread
    public fun createForSession(
        context: Context,
        config: SdkPaymentConfig,
        sessionId: String,
    ): Intent = SdkPaymentContract.createIntent(context, run(config, CheckoutConfig.SessionId(sessionId)))

    /**
     * The outcome of a checkout opened with [create] or [createForSession].
     *
     * @param mdOrder the order that was opened: a result that names none (the sheet closed before it learned it) gets it
     */
    public fun parseResult(
        resultCode: Int,
        data: Intent?,
        mdOrder: String = "",
    ): CheckoutResult = SdkPaymentContract.parseResult(resultCode, data).withOrderId(mdOrder)

    internal fun run(
        config: SdkPaymentConfig,
        checkout: CheckoutConfig,
    ): SdkPaymentRun {
        val runId = UUID.randomUUID().toString()
        SdkPaymentRegistry.register(runId, SdkPaymentRunSpec(sdk = SdkPayment.create(config), checkout = checkout))
        return SdkPaymentRun(runId, RestorableCheckout.encode(config, checkout))
    }
}
