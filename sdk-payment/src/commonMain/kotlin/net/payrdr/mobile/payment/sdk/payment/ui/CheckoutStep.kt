package net.payrdr.mobile.payment.sdk.payment.ui

import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.result.ProcessingStep
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.components.WalletAvailability

/** Local navigation inside the card-form part of the flow. */
internal sealed interface Route {
    data object Methods : Route

    data object NewCard : Route

    data object AllCards : Route

    data class Saved(
        val card: SavedCardInfo,
    ) : Route
}

/** What is on screen right now. */
internal sealed interface Step {
    data object Loading : Step

    /**
     * @property isRoot the first step of the stack (close instead of back); kept with the step so a
     * page leaving after a pop keeps its header while it slides away.
     */
    data class Form(
        val route: Route,
        val form: PaymentState.CardFormShown,
        val isRoot: Boolean,
    ) : Step

    data class Processing(
        val step: ProcessingStep,
    ) : Step

    data object ThreeDs : Step

    data class Result(
        val completed: PaymentState.Completed,
    ) : Step
}

internal val Step.isSheet: Boolean
    get() = this == Step.Loading || (this is Step.Form && route == Route.Methods)

/** Drawn inside the payment sheet: every step in the sheet presentation ([sheetPresentation]), [isSheet] ones otherwise. */
internal fun Step.inSheet(sheetPresentation: Boolean): Boolean = isSheet || sheetPresentation

/**
 * Passes through the light blur while it comes in or leaves. Not the step revealed under the loading header ([revealKey])
 * nor the loading layer staying under it: its header would show through the new one as a blurred halo.
 */
internal fun Step.blursWhileChanging(revealKey: String?): Boolean =
    if (revealKey == null) true else key != revealKey && this != Step.Loading

/**
 * How deep a step sits in the flow, for the direction of its zoom: deeper zooms in, shallower zooms out. Processing,
 * the bank page and the result go on deeper than any card step; a retry from the result comes back out to the methods.
 */
internal val Step.depth: Int
    get() =
        when (this) {
            Step.Loading -> 0
            is Step.Form ->
                when (route) {
                    Route.Methods -> 0
                    Route.NewCard, Route.AllCards -> 1
                    is Route.Saved -> 2
                }
            is Step.Processing -> 3
            Step.ThreeDs -> 4
            is Step.Result -> 5
        }

/** The step [to] lies deeper in the flow than [from], or as deep: it zooms in. */
internal fun isForward(
    from: Step,
    to: Step,
): Boolean = to.depth >= from.depth

/** Steps with the same key update in place; every other change of step is a transition. */
internal val Step.key: String
    get() =
        when (this) {
            Step.Loading -> "loading"
            is Step.Form ->
                when (val r = route) {
                    Route.Methods -> "methods"
                    Route.NewCard -> "new-card"
                    Route.AllCards -> "all-cards"
                    is Route.Saved -> "saved-${r.card.bindingId}"
                }
            // The processing stages update one step in place; the bank page wait is a step of its own (the sheet opens
            // to full height for it).
            is Step.Processing -> "processing"
            Step.ThreeDs -> "three-ds"
            is Step.Result -> "result"
        }

/** Nothing to choose between → skip the sheet and open the card form directly. */
internal fun rootRoute(
    form: PaymentState.CardFormShown,
    wallets: WalletAvailability,
): Route = if (form.savedCards.isEmpty() && !wallets.offeredIn(form).any) Route.NewCard else Route.Methods

/**
 * The step [state] puts on screen. The card form opens on the top of [stack] (its root route while the stack is empty)
 * once [wallets] are known; a finished run shows [heldResult] on the result screen, or keeps [lastStep] while the host
 * closes.
 */
internal fun sessionStep(
    state: PaymentState,
    stack: List<Route>,
    wallets: WalletAvailability?,
    heldResult: PaymentState.Completed?,
    lastStep: Step,
): Step =
    when (state) {
        PaymentState.Initializing -> Step.Loading
        is PaymentState.Processing ->
            if (state.step == ProcessingStep.LOADING_SESSION) Step.Loading else Step.Processing(state.step)
        is PaymentState.CardFormShown -> {
            val route = wallets?.let { stack.lastOrNull() ?: rootRoute(state, it) }
            if (route == null) Step.Loading else Step.Form(route, state, isRoot = stack.size <= 1)
        }
        PaymentState.ThreeDsChallenge -> Step.ThreeDs
        // Without a result screen the host is about to close — keep the last frame.
        is PaymentState.Completed -> heldResult?.let { Step.Result(it) } ?: lastStep
    }

/**
 * The card steps for a new [form]: its [root] alone for the first form or one back from a result ([restart]), otherwise
 * [stack] without a saved card the form no longer has.
 */
internal fun stackFor(
    form: PaymentState.CardFormShown,
    root: Route,
    stack: List<Route>,
    restart: Boolean,
): List<Route> =
    if (stack.isEmpty() || restart) {
        listOf(root)
    } else {
        // A removed card can't stay open.
        stack
            .filter { it !is Route.Saved || form.savedCards.any { c -> c.bindingId == it.card.bindingId } }
            .ifEmpty { listOf(root) }
    }
