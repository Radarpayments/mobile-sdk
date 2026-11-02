package net.payrdr.mobile.payment.sdk.payment.ui.state

import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.result.ProcessingStep
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo

/**
 * UI-layer view of the current [PaymentState]. Produced by [reduceUiAction] and consumed by
 * [net.payrdr.mobile.payment.sdk.payment.ui.PaymentCheckout] to decide which sub-screen to render.
 *
 * Note that `CardFormShown` can render two different sub-screens depending on whether saved cards
 * exist — the reducer picks [ShowCardList] vs [ShowCardForm] here rather than inside the composable.
 */
public sealed interface CheckoutUiAction {
    /** Any transient "doing stuff" state. [step] = null means the very first Initializing state. */
    public data class ShowProcessing(
        val step: ProcessingStep?,
    ) : CheckoutUiAction

    /** Saved-card list with an "Add new card" action. */
    public data class ShowCardList(
        val savedCards: List<SavedCardInfo>,
        val cvcRequired: Boolean,
        val allowNewCard: Boolean,
    ) : CheckoutUiAction

    /** Freshly-typed card form. */
    public data class ShowCardForm(
        val savedCards: List<SavedCardInfo>,
        val cvcRequired: Boolean,
        val allowNewCard: Boolean,
    ) : CheckoutUiAction

    /** 3DS v1 web challenge is running. */
    public data object ShowThreeDs : CheckoutUiAction

    /** Terminal — success or failure. */
    public data class ShowResult(
        val result: CheckoutResult,
    ) : CheckoutUiAction
}

/**
 * Pure reducer: given the previous [CheckoutUiAction] (or null for first emit) and the latest
 * [PaymentState], compute the next [CheckoutUiAction].
 *
 * Keeping this as a pure function makes it unit-testable without a composition runtime.
 */
public fun reduceUiAction(
    prev: CheckoutUiAction?,
    next: PaymentState,
): CheckoutUiAction =
    when (next) {
        PaymentState.Initializing -> CheckoutUiAction.ShowProcessing(step = null)
        is PaymentState.Processing -> CheckoutUiAction.ShowProcessing(step = next.step)
        is PaymentState.CardFormShown -> {
            if (next.savedCards.isNotEmpty()) {
                CheckoutUiAction.ShowCardList(
                    savedCards = next.savedCards,
                    cvcRequired = next.cvcRequired,
                    allowNewCard = next.allowNewCard,
                )
            } else {
                CheckoutUiAction.ShowCardForm(
                    savedCards = next.savedCards,
                    cvcRequired = next.cvcRequired,
                    allowNewCard = next.allowNewCard,
                )
            }
        }
        PaymentState.ThreeDsChallenge -> CheckoutUiAction.ShowThreeDs
        is PaymentState.Completed -> CheckoutUiAction.ShowResult(next.result)
    }
