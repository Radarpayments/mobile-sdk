package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult

/**
 * Hands the merchant the result: once per run, whichever way the checkout closes. The host removes its window as soon
 * as it gets the result (a window fade), so in the sheet presentation ([slidesAway]) the checkout slides away first and
 * the result follows once it is gone.
 */
@Stable
internal class ResultDelivery(
    private val slidesAway: Boolean,
    private val onResult: (CheckoutResult) -> Unit,
) {
    /** The checkout is on screen: false once it is sliding away with the result. */
    val shown = MutableTransitionState(true)

    /** A result has gone out, or is on its way out. */
    var delivered by mutableStateOf(false)
        private set

    /** The result waiting for the checkout to slide away. */
    var pending by mutableStateOf<CheckoutResult?>(null)
        private set

    fun deliver(result: CheckoutResult) {
        if (delivered) return
        delivered = true
        if (slidesAway) {
            pending = result
            shown.targetState = false
        } else {
            onResult(result)
        }
    }

    /** Hands the [pending] result over once the checkout has slid away. */
    fun handOver() {
        val result = pending
        if (result != null && shown.isIdle && !shown.currentState) {
            pending = null
            onResult(result)
        }
    }
}

/** The [ResultDelivery] of a checkout shown as [presentation], handing results to the latest [onResult]. */
@Composable
internal fun rememberResultDelivery(
    presentation: CheckoutPresentation,
    onResult: (CheckoutResult) -> Unit,
): ResultDelivery {
    val latestOnResult by rememberUpdatedState(onResult)
    val delivery =
        remember { ResultDelivery(slidesAway = presentation == CheckoutPresentation.Sheet, onResult = { latestOnResult(it) }) }
    LaunchedEffect(delivery.pending, delivery.shown.isIdle, delivery.shown.currentState) { delivery.handOver() }
    return delivery
}
