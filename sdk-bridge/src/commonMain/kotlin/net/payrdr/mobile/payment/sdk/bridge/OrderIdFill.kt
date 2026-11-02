package net.payrdr.mobile.payment.sdk.bridge

import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult

/** This result naming [orderId] when it names none (the checkout closed before it learned the order). */
internal fun CheckoutResult.orOrder(orderId: String): CheckoutResult =
    if (this.orderId.isNotEmpty()) {
        this
    } else {
        when (this) {
            is CheckoutResult.Completed -> copy(orderId = orderId)
            is CheckoutResult.Failed -> copy(orderId = orderId)
            is CheckoutResult.Canceled -> copy(orderId = orderId)
            is CheckoutResult.Unknown -> copy(orderId = orderId)
        }
    }

/** Codes of a checkout that did not open. */
internal object NotOpened {
    /** Another checkout of the same plugin is on screen. */
    const val BUSY = "busy"

    /** The app has no screen to put the checkout over (no activity, no key window). */
    const val NO_SCREEN = "no_screen"
}
