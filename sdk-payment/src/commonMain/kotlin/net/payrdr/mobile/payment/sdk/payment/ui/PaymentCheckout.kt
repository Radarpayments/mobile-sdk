package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.flow.PaymentFlowController
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PaymentEffect
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.result.ProcessingStep
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalOverlayCover
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalSaveCardOnly
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalStepSettled
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalTransfer
import net.payrdr.mobile.payment.sdk.payment.ui.components.PrimaryButtonLayer
import net.payrdr.mobile.payment.sdk.payment.ui.components.SIDE_PADDING
import net.payrdr.mobile.payment.sdk.payment.ui.components.SystemBarsAppearance
import net.payrdr.mobile.payment.sdk.payment.ui.components.WalletButtonLayer
import net.payrdr.mobile.payment.sdk.payment.ui.components.hiddenWhileCovered
import net.payrdr.mobile.payment.sdk.payment.ui.components.rememberKeyboardDismiss
import net.payrdr.mobile.payment.sdk.payment.ui.components.rememberReduceMotion
import net.payrdr.mobile.payment.sdk.payment.ui.components.rememberWalletAvailability
import net.payrdr.mobile.payment.sdk.payment.ui.loading.LoadingPhase
import net.payrdr.mobile.payment.sdk.payment.ui.loading.SavedCardCountCache
import net.payrdr.mobile.payment.sdk.payment.ui.loading.loadingPhases
import net.payrdr.mobile.payment.sdk.payment.ui.loading.rememberKeyValueStore
import net.payrdr.mobile.payment.sdk.payment.ui.motion.LocalStepBodyModifier
import net.payrdr.mobile.payment.sdk.payment.ui.motion.revealBody
import net.payrdr.mobile.payment.sdk.payment.ui.motion.stepBlur
import net.payrdr.mobile.payment.sdk.payment.ui.screens.LocalSheetPageHost
import net.payrdr.mobile.payment.sdk.payment.ui.screens.SheetPageHost
import net.payrdr.mobile.payment.sdk.payment.ui.screens.needsSavedCardScreen
import net.payrdr.mobile.payment.sdk.payment.ui.screens.offersPlatformWallet
import net.payrdr.mobile.payment.sdk.payment.ui.screens.skeletonLayout
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentThemeProvider
import net.payrdr.mobile.payment.sdk.payment.ui.theme.colorsAreDark
import net.payrdr.mobile.payment.sdk.payment.ui.util.WarmUpText

/** How the checkout sits in its host. */
internal enum class CheckoutPresentation {
    /**
     * The host window is transparent and covers the merchant screen: the first step is a bottom
     * sheet over a dimmed scrim, later steps fill the screen. Used by the turnkey Android Activity
     * and by the iOS view controller when it is presented over full screen.
     */
    Sheet,

    /**
     * The host already is a full screen or a system sheet: every step fills it, the first step is
     * drawn as a page without its own scrim and drag handle.
     */
    Page,
}

/**
 * Root composable for the SDK checkout UI. Observes `controller.state` and renders the
 * appropriate step; observes `controller.effects` and invokes [onResult] exactly once when the flow
 * dismisses (or, with [SdkPaymentConfig.showResultScreen], when the buyer leaves the result screen).
 *
 * Steps: payment sheet (wallets, saved cards, new card) → new card / saved-card CVV / all cards →
 * processing → 3-D Secure → result. Navigation between the card steps is local to the UI; the
 * FSM only sees the submitted intents.
 *
 * Use this directly when embedding the checkout in your own screen/sheet. For turnkey Activity /
 * ViewController hosting, go through `PayrdrCheckout` instead.
 *
 * @param config SDK config (drives theme, wallet buttons, scanner and the result screen)
 * @param controller FSM to observe/drive
 * @param onResult terminal callback. Guaranteed to be called at most once per completed run.
 * @param presentation whether to draw the first step as a sheet over a scrim
 */
@Composable
internal fun PaymentCheckout(
    config: SdkPaymentConfig,
    controller: PaymentFlowController,
    onResult: (CheckoutResult) -> Unit,
    modifier: Modifier = Modifier,
    presentation: CheckoutPresentation = CheckoutPresentation.Page,
) = PaymentLocaleProvider(config.locale) {
    PaymentThemeProvider(theme = config.theme) {
        SystemBarsAppearance(dark = colorsAreDark(LocalPaymentColors.current))
        WarmUpText(LocalPaymentTypography.current)
        val state by controller.state.collectAsState()
        val order by controller.order.collectAsState()
        // Decides whether the sheet has anything to offer, so the form waits for it.
        val wallets = rememberWalletAvailability(config)
        val reduceMotion = rememberReduceMotion()

        // The skeleton draws the rows the last session on this device had: its card count, its wallet.
        val store = rememberKeyValueStore()
        val cardCount = remember(store, config.baseUrl) { SavedCardCountCache(store, config.baseUrl) }
        val skeleton = remember(cardCount) { skeletonLayout(config, cardCount.read(), cardCount.readWalletOffered()) }
        // The last form's answer, kept through processing and the result: the buttons keep saying what they did.
        val transferOfForm = remember { booleanArrayOf(false, false) }
        (state as? PaymentState.CardFormShown)?.let {
            transferOfForm[0] = it.transfer
            transferOfForm[1] = it.saveCardOnly
        }
        val transfer = transferOfForm[0]
        val saveCardOnly = transferOfForm[1]
        LaunchedEffect(state) {
            (state as? PaymentState.CardFormShown)?.let {
                cardCount.write(it.savedCards.size)
                cardCount.writeWalletOffered(it.offersPlatformWallet())
            }
        }

        // The sheet rises at once; the content comes in when the session is in, through a skeleton only if that takes a
        // while (see loadingPhases).
        val loaded =
            wallets != null &&
                state !is PaymentState.Initializing &&
                (state as? PaymentState.Processing)?.step != ProcessingStep.LOADING_SESSION
        val latestLoaded by rememberUpdatedState(loaded)
        val phase by remember { loadingPhases(snapshotFlow { latestLoaded }) }.collectAsState(LoadingPhase.Blank)
        var skeletonShown by remember { mutableStateOf(false) }
        LaunchedEffect(phase) { if (phase == LoadingPhase.Skeleton) skeletonShown = true }

        var stack by remember { mutableStateOf(emptyList<Route>()) }
        var heldResult by remember { mutableStateOf<PaymentState.Completed?>(null) }
        // The result stays on screen while the sheet closes from it, or until the form is back after a retry: the steps
        // under it (processing, the bank-page wait) must not flash by on the way out.
        var frozenStep by remember { mutableStateOf<Step?>(null) }
        LaunchedEffect(state) { if (state is PaymentState.CardFormShown) frozenStep = null }
        var previous by remember { mutableStateOf<PaymentState?>(null) }
        var lastStep by remember { mutableStateOf<Step>(Step.Loading) }

        val delivery = rememberResultDelivery(presentation, onResult)
        // The sheet leaves by its own height, as it came in; a full-screen slide would flick it away.
        var sheetHeight by remember { mutableIntStateOf(0) }

        // Watch for terminal state: deliver it, or hold it for the result screen.
        LaunchedEffect(controller) {
            controller.effects.filterIsInstance<PaymentEffect.Dismiss>().collect {
                val terminal = controller.state.value as? PaymentState.Completed ?: return@collect
                // A cancel needs no screen; an unknown outcome has none of its own (the merchant checks the order).
                val hasScreen = terminal.result is CheckoutResult.Completed || terminal.result is CheckoutResult.Failed
                if (config.showResultScreen && hasScreen) heldResult = terminal else delivery.deliver(terminal.result)
            }
        }

        // First form, or back from a declined result: start over from the root step.
        LaunchedEffect(state, wallets) {
            val form = state as? PaymentState.CardFormShown
            if (form != null && wallets != null) {
                stack = stackFor(form, rootRoute(form, wallets), stack, restart = previous is PaymentState.Completed)
            }
            previous = state
        }

        val sessionStep = sessionStep(state, stack, wallets, heldResult, lastStep)
        // The form waits behind the loading step until the opening lets it in: no skeleton flash, or the skeleton's minimum.
        val step =
            frozenStep
                ?: if (phase != LoadingPhase.Content && sessionStep is Step.Form) Step.Loading else sessionStep
        // The first step after loading comes in under the header, which stays (see revealBody); later steps zoom whole.
        var revealKey by remember { mutableStateOf<String?>(null) }
        var shownKey by remember { mutableStateOf(step.key) }
        var shownStep by remember { mutableStateOf(step) }
        val buttonLayer = remember { PrimaryButtonLayer() }
        if (step.key != shownKey) {
            revealKey = if (shownKey == Step.Loading.key && step is Step.Form) step.key else null
            buttonLayer.forward = isForward(shownStep, step)
            shownKey = step.key
            shownStep = step
        }
        SideEffect { if (step !is Step.Result) lastStep = step }

        // In the sheet presentation every step opens inside the one payment sheet, which grows or shrinks to it.
        val formsInSheet = presentation == CheckoutPresentation.Sheet
        val sheetPages = remember { SheetPageHost() }
        val walletLayer = remember { WalletButtonLayer(parking = { buttonLayer.slots.lastOrNull() }) }
        val cancel = { controller.handle(PaymentIntent.Cancel) }
        val push = { route: Route -> stack = stack + route }
        val dismissKeyboard = rememberKeyboardDismiss()
        val roomForBank = rememberRoomForBank(controller, threeDs = step == Step.ThreeDs, presentation, reduceMotion)
        // The card step stays a moment as processing zooms in over it: its keyboard goes at once, not with it.
        LaunchedEffect(step is Step.Processing) { if (step is Step.Processing) dismissKeyboard() }
        // The keyboard goes as the step slides back, not after: a field keeps it until its step is gone.
        val pop = {
            dismissKeyboard()
            if (stack.size > 1) stack = stack.dropLast(1) else cancel()
        }
        val paySaved = { form: PaymentState.CardFormShown, card: SavedCardInfo ->
            if (needsSavedCardScreen(form, card)) {
                push(Route.Saved(card))
            } else {
                controller.handle(PaymentIntent.SubmitSavedCard(card.bindingId, cvc = null))
            }
        }
        val deliverHeld: () -> Unit = {
            val held = heldResult
            if (held != null) {
                frozenStep = Step.Result(held)
                heldResult = null
                delivery.deliver(held.result)
            } else {
                // Retry was tapped but the form did not come back: closing still hands the merchant the result.
                (frozenStep as? Step.Result)?.let { frozen ->
                    if (!delivery.delivered) delivery.deliver(frozen.completed.result)
                }
            }
        }

        PlatformBackHandler(enabled = true) {
            when (step) {
                is Step.Form -> pop()
                is Step.Result -> deliverHeld()
                Step.Loading -> cancel()
                is Step.Processing, Step.ThreeDs -> Unit
            }
        }

        Box(modifier = modifier.fillMaxSize()) {
            if (presentation == CheckoutPresentation.Sheet) {
                AnimatedVisibility(
                    visibleState = delivery.shown,
                    enter = EnterTransition.None,
                    exit = fadeOut(sheetCloseSpec(reduceMotion)),
                ) {
                    Scrim(
                        reduceMotion,
                        onClick = {
                            when {
                                step.isSheet -> cancel()
                                step is Step.Result -> deliverHeld()
                            }
                        },
                    )
                }
            }
            AnimatedVisibility(
                visibleState = delivery.shown,
                enter = EnterTransition.None,
                exit =
                    if (reduceMotion) {
                        fadeOut(sheetCloseSpec(reduceMotion = true))
                    } else {
                        slideOutVertically(sheetCloseSpec(reduceMotion = false)) { full ->
                            if (step.inSheet(formsInSheet) && sheetHeight > 0) sheetHeight else full
                        }
                    },
            ) {
                AnimatedContent(
                    targetState = step,
                    // As a sheet, one content: the steps change inside it. As pages, each step is a content of its own.
                    contentKey = { if (formsInSheet) SHEET_KEY else it.key },
                    transitionSpec = { stepChange(reduceMotion) },
                    label = "checkout-step",
                ) { current ->
                    val outerSettled = remember { derivedStateOf { transition.currentState == transition.targetState } }
                    val outerBody =
                        if (!formsInSheet && current.key == revealKey) {
                            Modifier.revealBody(this, zoom = !skeletonShown, reduceMotion)
                        } else {
                            Modifier
                        }
                    val waitOrResult: @Composable (Step, Boolean) -> Unit = { s, inSheet ->
                        WaitOrResultStep(
                            step = s,
                            inSheet = inSheet,
                            order = order,
                            canReturnToForm = controller.canReturnToForm,
                            onCancel = cancel,
                            onClose = deliverHeld,
                            onRetry = { result ->
                                frozenStep = result
                                heldResult = null
                                controller.handle(PaymentIntent.BackToPaymentMethods)
                            },
                        )
                    }
                    val formStep: @Composable (Step.Form) -> Unit = { s ->
                        FormStep(
                            route = s.route,
                            form = s.form,
                            config = config,
                            order = order,
                            isRoot = s.isRoot,
                            onBack = pop,
                            onClose = cancel,
                            onPush = push,
                            onPaySaved = { paySaved(s.form, it) },
                            onIntent = controller::handle,
                        )
                    }
                    val methods: @Composable ColumnScope.(PaymentState.CardFormShown?) -> Unit = { form ->
                        MethodsStep(
                            form = form,
                            skeleton = skeleton,
                            skeletonVisible = phase == LoadingPhase.Skeleton,
                            wallets = wallets,
                            order = order,
                            onClose = cancel,
                            onIntent = controller::handle,
                            onPaySaved = paySaved,
                            onPush = push,
                        )
                    }
                    CompositionLocalProvider(
                        LocalStepSettled provides outerSettled,
                        LocalStepBodyModifier provides outerBody,
                        LocalTransfer provides transfer,
                        LocalSaveCardOnly provides saveCardOnly,
                    ) {
                        val pageBlur = !formsInSheet && current.blursWhileChanging(revealKey)
                        Box((if (pageBlur) Modifier.stepBlur(this, reduceMotion) else Modifier).hiddenWhileCovered(sheetPages.cover)) {
                            if (current.inSheet(formsInSheet) && formsInSheet) {
                                // Loading, the payment methods and the card steps share one sheet. Dragging it down on a
                                // pushed step goes back, so typed card data is not thrown away with the whole checkout.
                                val leaves = current !is Step.Form || current.route == Route.Methods || current.isRoot
                                SheetSteps(
                                    current = current,
                                    step = step,
                                    revealKey = revealKey,
                                    zoomReveal = !skeletonShown,
                                    reduceMotion = reduceMotion,
                                    onDismiss =
                                        when {
                                            current is Step.Result -> deliverHeld
                                            leaves -> cancel
                                            else -> pop
                                        },
                                    onExpanded = { if (state == PaymentState.ThreeDsChallenge) roomForBank.value = true },
                                    onSheetHeight = { sheetHeight = it },
                                    walletLayer = walletLayer,
                                    buttonLayer = buttonLayer,
                                    walletButton = config.applePay != null,
                                ) { inner ->
                                    when {
                                        inner is Step.Processing || inner == Step.ThreeDs || inner is Step.Result ->
                                            waitOrResult(
                                                inner,
                                                true,
                                            )
                                        inner is Step.Form && inner.route != Route.Methods ->
                                            CompositionLocalProvider(LocalSheetPageHost provides sheetPages) { formStep(inner) }
                                        else ->
                                            Column(
                                                // Clear: the sheet's surface is the background, and a zooming step shows the one it replaces.
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = SIDE_PADDING),
                                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                            ) {
                                                methods((inner as? Step.Form)?.form)
                                            }
                                    }
                                }
                            } else if (current.isSheet) {
                                // Loading and the payment methods share one sheet: it slides in once and swaps its
                                // content in place. Two separate sheets would slide in one after the other.
                                SheetHost(
                                    presentation,
                                    onDismiss = cancel,
                                    onSheetHeight = { sheetHeight = it },
                                    reduceMotion = reduceMotion,
                                ) {
                                    methods((current as? Step.Form)?.form)
                                }
                            } else {
                                when (current) {
                                    Step.Loading -> Unit // the sheet above
                                    is Step.Form -> formStep(current)
                                    is Step.Processing, Step.ThreeDs, is Step.Result -> waitOrResult(current, false)
                                }
                            }
                        }
                    }
                }
            }
            // Sheets a step opens over itself (country picker, scan intro) cover the whole screen, not just the sheet.
            sheetPages.overlay?.let { overlay ->
                CompositionLocalProvider(LocalOverlayCover provides sheetPages.cover) { Box(Modifier.fillMaxSize()) { overlay() } }
            }
        }
    }
}

private const val SHEET_KEY = "sheet"

/**
 * Holds the bank page back until the sheet has made room for it: opened to the whole screen as a sheet, at once as a
 * page or with reduced motion. True lets a waiting challenge go on.
 */
@Composable
private fun rememberRoomForBank(
    controller: PaymentFlowController,
    threeDs: Boolean,
    presentation: CheckoutPresentation,
    reduceMotion: Boolean,
): MutableStateFlow<Boolean> {
    val roomForBank = remember { MutableStateFlow(false) }
    DisposableEffect(controller) {
        controller.beforeChallenge = { roomForBank.first { it } }
        onDispose {
            controller.beforeChallenge = {}
            // Gone without a sheet to open: let a waiting challenge go on as it would without the UI.
            roomForBank.value = true
        }
    }
    LaunchedEffect(threeDs) {
        roomForBank.value = threeDs && (presentation == CheckoutPresentation.Page || reduceMotion)
    }
    return roomForBank
}
