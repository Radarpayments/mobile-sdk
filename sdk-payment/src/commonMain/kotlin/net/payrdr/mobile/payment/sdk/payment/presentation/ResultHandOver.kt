package net.payrdr.mobile.payment.sdk.payment.presentation

import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult

/**
 * Hands the result to the app once: at once for a page, and for a sheet only after it has slid away, so the app's
 * own screen does not change under a sheet that is still closing.
 */
internal class ResultHandOver(
    private val slidesAway: Boolean,
    private val onResult: (CheckoutResult) -> Unit,
) {
    var shown: Boolean = true
        private set

    private var delivered = false

    private var pending: CheckoutResult? = null

    fun deliver(result: CheckoutResult) {
        if (delivered) return
        delivered = true
        if (slidesAway) {
            pending = result
            shown = false
        } else {
            onResult(result)
        }
    }

    /** The sheet is gone: the result held for it goes to the app. */
    fun exitFinished() {
        val result = pending
        if (result != null && !shown) {
            pending = null
            onResult(result)
        }
    }
}
