package net.payrdr.mobile.payment.sdk.payment.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.flow.PaymentFlowController
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.result.ProcessingStep
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.Route
import net.payrdr.mobile.payment.sdk.payment.ui.Step
import net.payrdr.mobile.payment.sdk.payment.ui.components.WalletAvailability
import net.payrdr.mobile.payment.sdk.payment.ui.isForward
import net.payrdr.mobile.payment.sdk.payment.ui.isSheet
import net.payrdr.mobile.payment.sdk.payment.ui.key
import net.payrdr.mobile.payment.sdk.payment.ui.loading.KeyValueStore
import net.payrdr.mobile.payment.sdk.payment.ui.loading.LoadingPhase
import net.payrdr.mobile.payment.sdk.payment.ui.loading.SavedCardCountCache
import net.payrdr.mobile.payment.sdk.payment.ui.loading.loadingPhases
import net.payrdr.mobile.payment.sdk.payment.ui.rootRoute
import net.payrdr.mobile.payment.sdk.payment.ui.screens.needsSavedCardScreen
import net.payrdr.mobile.payment.sdk.payment.ui.screens.offersPlatformWallet
import net.payrdr.mobile.payment.sdk.payment.ui.sessionStep
import net.payrdr.mobile.payment.sdk.payment.ui.stackFor

/**
 * The checkout between the payment flow and a renderer: which step is on screen, where back leads, when the skeleton
 * shows and when the result reaches the app. It draws nothing; a renderer shows [ui] and reports what the buyer did.
 *
 * Everything is called on the thread of the scope the model was made with (the main one).
 */
@PayrdrUiApi
@Suppress("TooManyFunctions")
public class CheckoutModel internal constructor(
    private val config: SdkPaymentConfig,
    private val controller: PaymentFlowController,
    store: KeyValueStore,
    wallets: WalletAvailability?,
    private val sheetPresentation: Boolean,
    private val reduceMotion: () -> Boolean,
    scope: CoroutineScope,
    onResult: (CheckoutResult) -> Unit,
) {
    private val scope = scope + SupervisorJob(scope.coroutineContext[Job])
    private val cardCount = SavedCardCountCache(store, config.baseUrl)
    private val skeleton = skeletonLayout(config, cardCount.read(), cardCount.readWalletOffered())
    private val handOver = ResultHandOver(slidesAway = sheetPresentation, onResult = onResult)
    private val loaded = MutableStateFlow(false)
    private val roomForBank = MutableStateFlow(false)

    private var wallets: WalletAvailability? = wallets
    private var state: PaymentState = controller.state.value
    private var previous: PaymentState? = null
    private var order: OrderSummary? = controller.order.value
    private var phase = LoadingPhase.Blank
    private var skeletonShown = false
    private var stack: List<Route> = emptyList()
    private var heldResult: PaymentState.Completed? = null
    private var frozenStep: Step? = null
    private var lastStep: Step = Step.Loading
    private var lastForm: PaymentState.CardFormShown? = null
    private var shownStep: Step = Step.Loading
    private var revealKey: String? = null
    private var forward = true
    private var threeDs = false

    private val mutableUi = MutableStateFlow(snapshot(Step.Loading))

    /** What to draw now; a new value after anything in it changes. */
    public val ui: StateFlow<CheckoutUi> = mutableUi.asStateFlow()

    init {
        controller.beforeChallenge = { roomForBank.first { it } }
        this.scope.launch { controller.state.collect(::onState) }
        this.scope.launch {
            controller.order.collect {
                order = it
                render()
            }
        }
        this.scope.launch {
            loadingPhases(loaded).collect {
                phase = it
                if (it == LoadingPhase.Skeleton) skeletonShown = true
                render()
            }
        }
    }

    /** The system back gesture or button. */
    public fun back() {
        when (mutableUi.value.step) {
            is Step.Form -> pop()
            is Step.Result -> closeResult()
            Step.Loading -> cancel()
            is Step.Processing, Step.ThreeDs -> Unit
        }
    }

    /** A tap on the dimmed app behind the sheet. */
    public fun scrimTapped() {
        val step = mutableUi.value.step
        when {
            step.isSheet -> cancel()
            step is Step.Result -> closeResult()
        }
    }

    /** The buyer swiped the sheet away or tapped its close control. */
    public fun sheetDismissed() {
        val step = mutableUi.value.step
        val leaves = step !is Step.Form || step.route == Route.Methods || step.isRoot
        when {
            step is Step.Result -> closeResult()
            leaves -> cancel()
            else -> pop()
        }
    }

    /** Opens [route] over the current step. */
    public fun push(route: Route) {
        stack = stack + route
        render()
    }

    /** Back one step; from the first one, out of the checkout. */
    public fun pop() {
        if (stack.size > 1) {
            stack = stack.dropLast(1)
            render()
        } else {
            cancel()
        }
    }

    /** [card] needs its own step before it can pay: the gateway asks for its CVC or for payer data. */
    public fun opensCardStep(card: SavedCardInfo): Boolean = currentForm()?.let { needsSavedCardScreen(it, card) } ?: false

    /** A tap on a saved card: its step when it [needs one][opensCardStep], the payment otherwise. */
    public fun paySaved(card: SavedCardInfo) {
        val form = currentForm() ?: return
        if (needsSavedCardScreen(form, card)) {
            push(Route.Saved(card))
        } else {
            controller.handle(PaymentIntent.SubmitSavedCard(card.bindingId, cvc = null))
        }
    }

    /** Passes what the buyer did on a step to the payment flow. */
    public fun handle(intent: PaymentIntent) {
        controller.handle(intent)
    }

    /** "Try again" on a failed result: back to the form it was paid from. */
    public fun retryFromResult() {
        val result = mutableUi.value.step as? Step.Result ?: return
        // The result stays on screen until the form is back, and for good when the flow refuses to return.
        frozenStep = result
        heldResult = null
        controller.handle(PaymentIntent.BackToPaymentMethods)
        render()
    }

    /** The buyer closed the result screen: the result goes to the app. */
    public fun closeResult() {
        val held = heldResult
        if (held != null) {
            frozenStep = Step.Result(held)
            heldResult = null
            handOver.deliver(held.result)
        } else {
            // Retry was tapped but the form did not come back: closing still hands over the result.
            (frozenStep as? Step.Result)?.let { handOver.deliver(it.completed.result) }
        }
        render()
    }

    /** The sheet has grown to the whole screen: the bank's page may open. */
    public fun sheetExpanded() {
        if (state == PaymentState.ThreeDsChallenge) roomForBank.value = true
    }

    /** The sheet has slid away after [CheckoutUi.shown] turned false. */
    public fun exitFinished() {
        handOver.exitFinished()
    }

    /** The renderer is gone: stops following the flow and lets a bank page that waits for the sheet go on. */
    public fun dispose() {
        controller.beforeChallenge = {}
        roomForBank.value = true
        scope.cancel()
    }

    /** Which wallets the device can pay with, when that was not known at the start. */
    internal fun walletsKnown(available: WalletAvailability) {
        wallets = available
        restack()
        render()
    }

    private fun cancel() {
        controller.handle(PaymentIntent.Cancel)
    }

    private fun currentForm(): PaymentState.CardFormShown? = (mutableUi.value.step as? Step.Form)?.form

    private fun onState(next: PaymentState) {
        state = next
        if (next is PaymentState.CardFormShown) {
            lastForm = next
            frozenStep = null
            // Adding a card shows neither saved cards nor wallets; remembering that would draw the next payment's
            // skeleton as a bare card form, which then flips to the methods.
            if (!next.saveCardOnly) {
                cardCount.write(next.savedCards.size)
                cardCount.writeWalletOffered(next.offersPlatformWallet())
            }
        }
        if (next is PaymentState.Completed) {
            val hasScreen = next.result is CheckoutResult.Completed || next.result is CheckoutResult.Failed
            if (config.showResultScreen && hasScreen) heldResult = next else handOver.deliver(next.result)
        }
        restack()
        render()
    }

    private fun restack() {
        val form = state as? PaymentState.CardFormShown
        val available = wallets
        if (form != null && available != null) {
            stack = stackFor(form, rootRoute(form, available), stack, restart = previous is PaymentState.Completed)
        }
        previous = state
    }

    private fun render() {
        loaded.value = wallets != null &&
            state !is PaymentState.Initializing &&
            (state as? PaymentState.Processing)?.step != ProcessingStep.LOADING_SESSION
        val session = sessionStep(state, stack, wallets, heldResult, lastStep)
        val step = frozenStep ?: if (phase != LoadingPhase.Content && session is Step.Form) Step.Loading else session
        if (step.key != shownStep.key) {
            revealKey = if (shownStep == Step.Loading && step is Step.Form) step.key else null
            forward = isForward(shownStep, step)
            shownStep = step
        }
        if (step !is Step.Result) lastStep = step
        if ((step == Step.ThreeDs) != threeDs) {
            threeDs = step == Step.ThreeDs
            roomForBank.value = threeDs && (!sheetPresentation || reduceMotion())
        }
        mutableUi.value = snapshot(step)
    }

    private fun snapshot(step: Step): CheckoutUi =
        CheckoutUi(
            step = step,
            revealKey = revealKey,
            forward = forward,
            skeleton = skeleton,
            skeletonVisible = phase == LoadingPhase.Skeleton,
            zoomReveal = !skeletonShown,
            shown = handOver.shown,
            order = order,
            transfer = lastForm?.transfer == true,
            saveCardOnly = lastForm?.saveCardOnly == true,
            canReturnToForm = controller.canReturnToForm,
            wallets = (step as? Step.Form)?.let { (wallets ?: WalletAvailability.None).offeredIn(it.form) } ?: WalletAvailability.None,
            walletConfigured = config.applePay != null,
            sheetPresentation = sheetPresentation,
            reduceMotion = reduceMotion(),
        )
}
