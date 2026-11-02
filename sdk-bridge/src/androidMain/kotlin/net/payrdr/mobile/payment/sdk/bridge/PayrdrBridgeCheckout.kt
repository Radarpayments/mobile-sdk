package net.payrdr.mobile.payment.sdk.bridge

import android.app.Activity
import android.content.Intent
import androidx.annotation.MainThread
import net.payrdr.mobile.payment.sdk.payment.android.PayrdrCheckoutIntent
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig

/**
 * Opens the checkout for a Flutter or React Native plugin over the app's current activity, whatever kind it is: the
 * sheet starts with `startActivityForResult`, and the plugin hands each activity result to [handleActivityResult].
 * The app's own activity needs no changes.
 *
 * One object per plugin instance; one checkout at a time.
 */
public class PayrdrBridgeCheckout {
    private class Waiting(
        val orderId: String,
        val onResult: (String) -> Unit,
    )

    private var waiting: Waiting? = null

    /**
     * Opens the checkout of [requestJson] (`{"config": {...}, "mdOrder": "..."}`, see the module README) over [activity]
     * (null when the plugin has none: the result then says so). [onResult] gets the result JSON once: after the sheet
     * closes, or at once when it could not open.
     */
    @MainThread
    public fun start(
        activity: Activity?,
        requestJson: String,
        onResult: (String) -> Unit,
    ) {
        if (waiting != null) return onResult(CheckoutResultJson.failure(NotOpened.BUSY, "A checkout is already open"))
        val request =
            try {
                CheckoutRequest.parse(requestJson)
            } catch (e: InvalidBridgeRequest) {
                return onResult(CheckoutResultJson.invalidRequest(e.message.orEmpty()))
            }
        activity ?: return onResult(CheckoutResultJson.failure(NotOpened.NO_SCREEN, "The app has no activity to open the checkout over"))
        val intent =
            when (val checkout = request.checkout) {
                is CheckoutConfig.MdOrder -> PayrdrCheckoutIntent.create(activity, request.config, checkout.value)
                is CheckoutConfig.SessionId -> PayrdrCheckoutIntent.createForSession(activity, request.config, checkout.value)
            }
        waiting = Waiting(request.orderId, onResult)
        try {
            activity.startActivityForResult(intent, REQUEST_CODE)
        } catch (e: RuntimeException) {
            waiting = null
            onResult(CheckoutResultJson.failure(NotOpened.NO_SCREEN, e.message ?: "The checkout could not start", request.orderId))
        }
    }

    /** Call it from the plugin's activity result listener; true when the result was the checkout's. */
    public fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ): Boolean {
        if (requestCode != REQUEST_CODE) return false
        // A result for a checkout this object did not open (the app was recreated): nobody is waiting for it any more.
        val current = waiting ?: return true
        waiting = null
        current.onResult(CheckoutResultJson.encode(PayrdrCheckoutIntent.parseResult(resultCode, data, current.orderId)))
        return true
    }

    public companion object {
        /** The request code of the checkout activity; within 16 bits, as fragment activities require. */
        public const val REQUEST_CODE: Int = 0x7A01
    }
}
