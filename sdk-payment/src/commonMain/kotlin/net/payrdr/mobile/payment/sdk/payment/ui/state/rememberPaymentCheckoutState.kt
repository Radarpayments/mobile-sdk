package net.payrdr.mobile.payment.sdk.payment.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import net.payrdr.mobile.payment.sdk.payment.flow.PaymentFlowController

/**
 * Composable bridge between `PaymentFlowController.state` (a `StateFlow<PaymentState>`) and the UI.
 *
 * Returns a [State]<[CheckoutUiAction]> that recomposes whenever the FSM emits a new state.
 *
 * Usage:
 * ```
 * val action by rememberPaymentCheckoutState(controller)
 * when (action) { … }
 * ```
 */
@Composable
public fun rememberPaymentCheckoutState(controller: PaymentFlowController): State<CheckoutUiAction> {
    val stateFlow = remember(controller) { controller.state }
    val current by stateFlow.collectAsState()
    return produceState<CheckoutUiAction>(
        initialValue = reduceUiAction(prev = null, next = current),
        key1 = current,
    ) {
        value = reduceUiAction(prev = null, next = current)
    }
}
