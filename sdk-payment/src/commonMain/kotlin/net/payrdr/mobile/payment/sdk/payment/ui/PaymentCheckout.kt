package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.flow.PaymentFlowController
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.result.PaymentEffect
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.result.ProcessingStep
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.components.DraggableSheet
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalOverlayCover
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalPrimaryButtonLayer
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalSaveCardOnly
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalStepAlpha
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalStepSettled
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalTransfer
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalWalletButtonLayer
import net.payrdr.mobile.payment.sdk.payment.ui.components.PrimaryButtonLayer
import net.payrdr.mobile.payment.sdk.payment.ui.components.PrimaryButtonLayerHost
import net.payrdr.mobile.payment.sdk.payment.ui.components.SIDE_PADDING
import net.payrdr.mobile.payment.sdk.payment.ui.components.SystemBarsAppearance
import net.payrdr.mobile.payment.sdk.payment.ui.components.WalletAvailability
import net.payrdr.mobile.payment.sdk.payment.ui.components.WalletButtonLayer
import net.payrdr.mobile.payment.sdk.payment.ui.components.WalletButtonLayerHost
import net.payrdr.mobile.payment.sdk.payment.ui.components.hiddenWhileCovered
import net.payrdr.mobile.payment.sdk.payment.ui.components.rememberKeyboardDismiss
import net.payrdr.mobile.payment.sdk.payment.ui.components.rememberReduceMotion
import net.payrdr.mobile.payment.sdk.payment.ui.components.rememberWalletAvailability
import net.payrdr.mobile.payment.sdk.payment.ui.components.tapsOnly
import net.payrdr.mobile.payment.sdk.payment.ui.loading.LoadingPhase
import net.payrdr.mobile.payment.sdk.payment.ui.loading.SavedCardCountCache
import net.payrdr.mobile.payment.sdk.payment.ui.loading.loadingPhases
import net.payrdr.mobile.payment.sdk.payment.ui.loading.rememberKeyValueStore
import net.payrdr.mobile.payment.sdk.payment.ui.motion.LocalStepBodyModifier
import net.payrdr.mobile.payment.sdk.payment.ui.motion.SheetMotion
import net.payrdr.mobile.payment.sdk.payment.ui.motion.loadingRevealTransform
import net.payrdr.mobile.payment.sdk.payment.ui.motion.revealBody
import net.payrdr.mobile.payment.sdk.payment.ui.motion.stepAlpha
import net.payrdr.mobile.payment.sdk.payment.ui.motion.stepBlur
import net.payrdr.mobile.payment.sdk.payment.ui.motion.stepTransform
import net.payrdr.mobile.payment.sdk.payment.ui.screens.CardFormScreen
import net.payrdr.mobile.payment.sdk.payment.ui.screens.CardFormSkeleton
import net.payrdr.mobile.payment.sdk.payment.ui.screens.CardListScreen
import net.payrdr.mobile.payment.sdk.payment.ui.screens.LocalSheetPageHost
import net.payrdr.mobile.payment.sdk.payment.ui.screens.PaymentMethodsContent
import net.payrdr.mobile.payment.sdk.payment.ui.screens.PaymentMethodsSkeleton
import net.payrdr.mobile.payment.sdk.payment.ui.screens.ProcessingContent
import net.payrdr.mobile.payment.sdk.payment.ui.screens.ResultContent
import net.payrdr.mobile.payment.sdk.payment.ui.screens.SavedCardScreen
import net.payrdr.mobile.payment.sdk.payment.ui.screens.SheetPageHost
import net.payrdr.mobile.payment.sdk.payment.ui.screens.SkeletonLayout
import net.payrdr.mobile.payment.sdk.payment.ui.screens.ThreeDsChallengeContent
import net.payrdr.mobile.payment.sdk.payment.ui.screens.isCardFormRoot
import net.payrdr.mobile.payment.sdk.payment.ui.screens.needsSavedCardScreen
import net.payrdr.mobile.payment.sdk.payment.ui.screens.offersPlatformWallet
import net.payrdr.mobile.payment.sdk.payment.ui.screens.skeletonLayout
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentThemeProvider
import net.payrdr.mobile.payment.sdk.payment.ui.theme.colorsAreDark
import net.payrdr.mobile.payment.sdk.payment.ui.util.WarmUpText
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** How the checkout sits in its host. */
public enum class CheckoutPresentation {
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
public fun PaymentCheckout(
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
        val latestOnResult by rememberUpdatedState(onResult)
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

        // The host removes its window or controller as soon as it gets the result (a cross-dissolve on iOS,
        // a window fade on Android), so in the sheet presentation the checkout slides away first.
        val shown = remember { MutableTransitionState(true) }
        var closingResult by remember { mutableStateOf<CheckoutResult?>(null) }
        // The sheet leaves by its own height, as it came in; a full-screen slide would flick it away.
        var sheetHeight by remember { mutableIntStateOf(0) }
        // The merchant gets one result per run, whichever way the checkout closes.
        var delivered by remember { mutableStateOf(false) }
        val deliver = { result: CheckoutResult ->
            delivered = true
            if (presentation == CheckoutPresentation.Sheet) {
                closingResult = result
                shown.targetState = false
            } else {
                latestOnResult(result)
            }
        }
        LaunchedEffect(closingResult, shown.isIdle, shown.currentState) {
            val result = closingResult
            if (result != null && shown.isIdle && !shown.currentState) {
                closingResult = null
                latestOnResult(result)
            }
        }

        // Watch for terminal state: deliver it, or hold it for the result screen.
        LaunchedEffect(controller) {
            controller.effects.filterIsInstance<PaymentEffect.Dismiss>().collect {
                val terminal = controller.state.value as? PaymentState.Completed ?: return@collect
                // A cancel needs no screen; an unknown outcome has none of its own (the merchant checks the order).
                val hasScreen = terminal.result is CheckoutResult.Completed || terminal.result is CheckoutResult.Failed
                if (config.showResultScreen && hasScreen) heldResult = terminal else deliver(terminal.result)
            }
        }

        // First form, or back from a declined result: start over from the root step.
        LaunchedEffect(state, wallets) {
            val form = state as? PaymentState.CardFormShown
            if (form != null && wallets != null) {
                val root = rootRoute(form, wallets)
                stack =
                    if (stack.isEmpty() || previous is PaymentState.Completed) {
                        listOf(root)
                    } else {
                        // A removed card can't stay open.
                        stack
                            .filter { it !is Route.Saved || form.savedCards.any { c -> c.bindingId == it.card.bindingId } }
                            .ifEmpty { listOf(root) }
                    }
            }
            previous = state
        }

        val sessionStep: Step =
            when (val s = state) {
                PaymentState.Initializing -> Step.Loading
                is PaymentState.Processing ->
                    if (s.step == ProcessingStep.LOADING_SESSION) Step.Loading else Step.Processing(s.step)
                is PaymentState.CardFormShown -> {
                    val route = wallets?.let { stack.lastOrNull() ?: rootRoute(s, it) }
                    if (route == null) Step.Loading else Step.Form(route, s, isRoot = stack.size <= 1)
                }
                PaymentState.ThreeDsChallenge -> Step.ThreeDs
                // Without a result screen the host is about to close — keep the last frame.
                is PaymentState.Completed -> heldResult?.let { Step.Result(it) } ?: lastStep
            }
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
        // The bank page opens once the sheet has made room for it: open to the whole screen as a sheet, at once as a page.
        val roomForBank = remember { MutableStateFlow(false) }
        DisposableEffect(controller) {
            controller.beforeChallenge = { roomForBank.first { it } }
            onDispose {
                controller.beforeChallenge = {}
                // Gone without a sheet to open: let a waiting challenge go on as it would without the UI.
                roomForBank.value = true
            }
        }
        LaunchedEffect(step == Step.ThreeDs) {
            roomForBank.value = step == Step.ThreeDs && (presentation == CheckoutPresentation.Page || reduceMotion)
        }
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
                deliver(held.result)
            } else {
                // Retry was tapped but the form did not come back: closing still hands the merchant the result.
                (frozenStep as? Step.Result)?.let { frozen ->
                    if (!delivered) deliver(frozen.completed.result)
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
                AnimatedVisibility(visibleState = shown, enter = EnterTransition.None, exit = fadeOut(sheetCloseSpec(reduceMotion))) {
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
                visibleState = shown,
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
                    transitionSpec = {
                        if (initialState == Step.Loading && targetState is Step.Form) {
                            loadingRevealTransform(reduceMotion)
                        } else {
                            stepTransform(isForward(initialState, targetState), reduceMotion)
                        }
                    },
                    label = "checkout-step",
                ) { current ->
                    val outerSettled = remember { derivedStateOf { transition.currentState == transition.targetState } }
                    val outerBody =
                        if (!formsInSheet &&
                            current.key == revealKey
                        ) {
                            Modifier.revealBody(this, zoom = !skeletonShown, reduceMotion)
                        } else {
                            Modifier
                        }
                    // Processing, the bank-page wait and the result: inside the sheet ([inSheet]) as tall as they need,
                    // filling the screen as pages.
                    val waitOrResult: @Composable (Step, Boolean) -> Unit = { s, inSheet ->
                        // As a page it fills the host and paints its own background (the host view may be clear).
                        val fill =
                            if (inSheet) {
                                Modifier.fillMaxWidth()
                            } else {
                                Modifier.fillMaxSize().background(
                                    LocalPaymentColors.current.background,
                                )
                            }
                        when (s) {
                            is Step.Processing -> ProcessingContent(step = s.step, order = order, modifier = fill)
                            Step.ThreeDs ->
                                ThreeDsChallengeContent(
                                    onCancel = cancel,
                                    order = order,
                                    bottomPadding = if (inSheet) 0.dp else 18.dp,
                                )
                            is Step.Result -> {
                                val result = s.completed.result
                                // Only with a form to go back to: a checkout that failed to start has none.
                                val canRetry = result is CheckoutResult.Failed && controller.canReturnToForm
                                ResultContent(
                                    result = result,
                                    receipt = s.completed.receipt,
                                    order = order,
                                    onClose = deliverHeld,
                                    onRetry =
                                        if (canRetry) {
                                            {
                                                frozenStep = s
                                                heldResult = null
                                                controller.handle(PaymentIntent.BackToPaymentMethods)
                                            }
                                        } else {
                                            null
                                        },
                                    fillHeight = !inSheet,
                                    modifier = if (inSheet) fill else fill.windowInsetsPadding(WindowInsets.safeDrawing),
                                )
                            }
                            else -> Unit
                        }
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
                    CompositionLocalProvider(
                        LocalStepSettled provides outerSettled,
                        LocalStepBodyModifier provides outerBody,
                        LocalTransfer provides transfer,
                        LocalSaveCardOnly provides saveCardOnly,
                    ) {
                        val pageBlur = !formsInSheet && current.blursWhileChanging(revealKey)
                        Box((if (pageBlur) Modifier.stepBlur(this, reduceMotion) else Modifier).hiddenWhileCovered(sheetPages.cover)) {
                            if (current.inSheet(formsInSheet) && formsInSheet) {
                                // Loading, the payment methods and the card steps share one sheet: it slides in once, then
                                // grows or shrinks to each step while the steps slide sideways. Dragging it down on a pushed
                                // step goes back, so typed card data is not thrown away with the whole checkout.
                                val leaves = current !is Step.Form || current.route == Route.Methods || current.isRoot
                                SheetHost(
                                    presentation,
                                    onDismiss =
                                        when {
                                            current is Step.Result -> deliverHeld
                                            leaves -> cancel
                                            else -> pop
                                        },
                                    // Nothing to drag away while the payment is in flight.
                                    draggable = current !is Step.Processing && current != Step.ThreeDs,
                                    showGrabber = current != Step.ThreeDs,
                                    expanded = step == Step.ThreeDs,
                                    onExpanded = { if (state == PaymentState.ThreeDsChallenge) roomForBank.value = true },
                                    onSheetHeight = { sheetHeight = it },
                                    sidePadding = 0.dp,
                                    reduceMotion = reduceMotion,
                                ) {
                                    // The native wallet button is drawn under the steps, the main button over them: both out of the steps'
                                    // transitions (see WalletButtonLayer, PrimaryButtonLayer).
                                    Box {
                                        WalletButtonLayerHost(
                                            walletLayer,
                                            // Made once the sheet stands unless a step shows it sooner: making it cost a long
                                            // frame as the sheet set off.
                                            enabled = config.applePay != null && LocalSheetEntered.current.value,
                                            Modifier.matchParentSize(),
                                        )
                                        CompositionLocalProvider(
                                            LocalWalletButtonLayer provides walletLayer,
                                            LocalPrimaryButtonLayer provides buttonLayer,
                                        ) {
                                            AnimatedContent(
                                                targetState = current,
                                                contentKey = { it.key },
                                                transitionSpec = {
                                                    if (initialState == Step.Loading && targetState is Step.Form) {
                                                        loadingRevealTransform(reduceMotion)
                                                    } else {
                                                        stepTransform(isForward(initialState, targetState), reduceMotion)
                                                    }
                                                },
                                                // On the sheet's bottom edge, as the sheet itself: a taller step rises above the shorter
                                                // one and both pay buttons sit in the same place. Coming out of loading the headers must
                                                // coincide instead — the loading layer stays under the new one — so the top edge.
                                                contentAlignment =
                                                    if (step.key ==
                                                        revealKey
                                                    ) {
                                                        Alignment.TopCenter
                                                    } else {
                                                        Alignment.BottomCenter
                                                    },
                                                label = "sheet-step",
                                            ) { inner ->
                                                val sheetEntered = LocalSheetEntered.current
                                                val settled =
                                                    remember {
                                                        derivedStateOf {
                                                            transition.currentState == transition.targetState && sheetEntered.value
                                                        }
                                                    }
                                                // Changes inside one step (loading → methods, fields appearing) move the sheet's top
                                                // as smoothly as a step change does, the bottom staying put.
                                                // Every step learns when it has settled: native (UIKit) fields stand in for themselves
                                                // until then.
                                                val reveals = inner.key == revealKey
                                                val body =
                                                    if (reveals) {
                                                        Modifier.revealBody(
                                                            this,
                                                            zoom = !skeletonShown,
                                                            reduceMotion,
                                                        )
                                                    } else {
                                                        Modifier
                                                    }
                                                val blurs = inner.blursWhileChanging(revealKey)
                                                val blur = if (blurs) Modifier.stepBlur(this, reduceMotion) else Modifier
                                                val alpha = stepAlpha(this, reveals, reduceMotion)
                                                Box(Modifier.smoothHeight().then(blur)) {
                                                    CompositionLocalProvider(
                                                        LocalStepSettled provides settled,
                                                        LocalStepBodyModifier provides body,
                                                        LocalStepAlpha provides alpha,
                                                    ) {
                                                        if (inner is Step.Processing || inner == Step.ThreeDs || inner is Step.Result) {
                                                            waitOrResult(inner, true)
                                                        } else if (inner is Step.Form && inner.route != Route.Methods) {
                                                            CompositionLocalProvider(LocalSheetPageHost provides sheetPages) {
                                                                formStep(inner)
                                                            }
                                                        } else {
                                                            val form = (inner as? Step.Form)?.form
                                                            Column(
                                                                // Clear: the sheet's surface is the background, and a zooming step shows the one it replaces.
                                                                modifier =
                                                                    Modifier.fillMaxWidth().padding(
                                                                        horizontal = SIDE_PADDING,
                                                                    ),
                                                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                                            ) {
                                                                if (form == null) {
                                                                    LoadingSkeleton(
                                                                        skeleton,
                                                                        visible = phase == LoadingPhase.Skeleton,
                                                                        onClose = cancel,
                                                                    )
                                                                } else {
                                                                    PaymentMethodsContent(
                                                                        wallets = (wallets ?: WalletAvailability.None).offeredIn(form),
                                                                        order = order,
                                                                        savedCards = form.savedCards,
                                                                        onClose = cancel,
                                                                        onGooglePay = { controller.handle(PaymentIntent.TapGooglePay) },
                                                                        onApplePay = { controller.handle(PaymentIntent.TapApplePay) },
                                                                        onPaySaved = { paySaved(form, it) },
                                                                        onNewCard = { push(Route.NewCard) },
                                                                        onAllCards = { push(Route.AllCards) },
                                                                        opensCardStep = { needsSavedCardScreen(form, it) },
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        PrimaryButtonLayerHost(buttonLayer, Modifier.matchParentSize())
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
                                    val form = (current as? Step.Form)?.form
                                    if (form == null) {
                                        LoadingSkeleton(skeleton, visible = phase == LoadingPhase.Skeleton, onClose = cancel)
                                    } else {
                                        PaymentMethodsContent(
                                            wallets = (wallets ?: WalletAvailability.None).offeredIn(form),
                                            order = order,
                                            savedCards = form.savedCards,
                                            onClose = cancel,
                                            onGooglePay = { controller.handle(PaymentIntent.TapGooglePay) },
                                            onApplePay = { controller.handle(PaymentIntent.TapApplePay) },
                                            onPaySaved = { paySaved(form, it) },
                                            onNewCard = { push(Route.NewCard) },
                                            onAllCards = { push(Route.AllCards) },
                                            opensCardStep = { needsSavedCardScreen(form, it) },
                                        )
                                    }
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

/** Between the status bar and the top of a sheet that is not opened to the whole screen. */
private val SHEET_TOP_GAP = 24.dp

/** The default sheet's top corners ([PaymentShapes.sheet][net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentShapes]). */
private val SHEET_CORNER = 26.dp

/** The sheet slides away, the scrim fades: on the sheet's curve, or a short fade with reduced motion. */
private fun <T> sheetCloseSpec(reduceMotion: Boolean) =
    if (reduceMotion) {
        tween<T>(SheetMotion.REDUCED_SHEET_CLOSE_MS)
    } else {
        tween(SheetMotion.SHEET_CLOSE_MS, easing = SheetMotion.SheetEasing)
    }

/** The first step's skeleton: the card form's when that will be the first step, the payment sheet's otherwise. */
@Composable
private fun ColumnScope.LoadingSkeleton(
    layout: SkeletonLayout,
    visible: Boolean,
    onClose: () -> Unit,
) {
    if (layout.isCardFormRoot) CardFormSkeleton(visible, onClose) else PaymentMethodsSkeleton(layout, visible, onClose)
}

@Composable
private fun Scrim(
    reduceMotion: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalPaymentColors.current
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    val enter =
        if (reduceMotion) {
            tween<Float>(SheetMotion.REDUCED_SHEET_ENTER_MS)
        } else {
            tween(SheetMotion.SHEET_ENTER_MS, easing = SheetMotion.SheetEasing)
        }
    AnimatedVisibility(visibleState = visible, enter = fadeIn(enter)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.scrim)
                .tapsOnly(onClick),
        )
    }
}

/**
 * The first step's container: a draggable bottom sheet sliding up in [CheckoutPresentation.Sheet],
 * a plain top-aligned page in [CheckoutPresentation.Page].
 */
@Composable
private fun SheetHost(
    presentation: CheckoutPresentation,
    onDismiss: () -> Unit,
    onSheetHeight: (Int) -> Unit,
    sidePadding: Dp = SIDE_PADDING,
    reduceMotion: Boolean = false,
    draggable: Boolean = true,
    showGrabber: Boolean = draggable,
    // Opened to the whole screen (the bank page comes over it): no gap above, no rounded top, the status bar inside.
    expanded: Boolean = false,
    onExpanded: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalPaymentColors.current
    when (presentation) {
        CheckoutPresentation.Sheet -> {
            val entered = remember { mutableStateOf(false) }
            val open by animateFloatAsState(
                targetValue = if (expanded) 1f else 0f,
                animationSpec = if (reduceMotion) snap() else tween(SheetMotion.SHEET_EXPAND_MS, easing = SheetMotion.SheetEasing),
                label = "sheet-expand",
                finishedListener = { if (it == 1f) onExpanded() },
            )
            val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Box(
                Modifier.fillMaxSize().padding(top = (statusBar + SHEET_TOP_GAP) * (1f - open)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                DraggableSheet(
                    onDismiss = onDismiss,
                    draggable = draggable,
                    showGrabber = showGrabber,
                    sidePadding = sidePadding,
                    shape =
                        if (open >
                            0f
                        ) {
                            RoundedCornerShape(topStart = SHEET_CORNER * (1f - open), topEnd = SHEET_CORNER * (1f - open))
                        } else {
                            null
                        },
                    modifier =
                        Modifier
                            .slideUpOnEnter(reduceMotion, entered)
                            .onSizeChanged { onSheetHeight(it.height) },
                ) {
                    CompositionLocalProvider(LocalSheetEntered provides entered) {
                        Column(
                            Modifier.padding(top = statusBar * open),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            content = content,
                        )
                    }
                }
            }
        }
        CheckoutPresentation.Page ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(colors.background)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = SIDE_PADDING, vertical = 16.dp)
                        .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
    }
}

/**
 * True once the sheet has slid in. UIKit views wait for it where they can: native card fields as well as for their step,
 * the wallet button while no step shows it. Made while the sheet rose, each cost a long frame of its rise.
 */
private val LocalSheetEntered = staticCompositionLocalOf<State<Boolean>> { mutableStateOf(true) }

/**
 * Slides the sheet in from below by its own height, on the sheet's curve; with reduced motion it fades in where it
 * stands. Done in placement rather than with `AnimatedVisibility(slideInVertically)`: on iOS that animation layer cut
 * the sheet's top off while the sheet also grew (loading → payment methods), so the handle was missing for a few frames.
 */
private fun Modifier.slideUpOnEnter(
    reduceMotion: Boolean,
    entered: MutableState<Boolean>,
): Modifier =
    composed {
        val hidden = remember { Animatable(1f) }
        LaunchedEffect(Unit) {
            val spec =
                if (reduceMotion) {
                    tween<Float>(SheetMotion.REDUCED_SHEET_ENTER_MS)
                } else {
                    tween(SheetMotion.SHEET_ENTER_MS, easing = SheetMotion.SheetEasing)
                }
            hidden.animateTo(0f, spec)
            entered.value = true
        }
        if (reduceMotion) {
            graphicsLayer { alpha = 1f - hidden.value }
        } else {
            layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(0, (placeable.height * hidden.value).roundToInt())
                }
            }
        }
    }

/**
 * Animates the height when the content itself grows or shrinks (loading → methods, a row appearing), clipped and
 * with the content under the top edge, so the sheet's top and the content move together. A height the space allows
 * rather than the content asks for (a long form filling the screen as the keyboard comes or goes) applies at once:
 * animating it against the keyboard tore the sheet. Content that animates its own size (the footer leaving over the
 * keyboard) is followed closely by a stiff spring: easing each of its steps anew lagged and settled afterwards.
 */
private fun Modifier.smoothHeight(): Modifier =
    composed {
        val height = remember { Animatable(0f) }
        var target by remember { mutableIntStateOf(-1) }
        var instant by remember { mutableStateOf(true) }
        // Read only here and in the layout below, so plain holders rather than state.
        val lastMax = remember { intArrayOf(Int.MAX_VALUE) }
        val lastChange = remember { arrayOfNulls<TimeMark>(1) }
        // One collector for the whole life: an effect keyed on the target is cancelled by every next change
        // before it runs, so content resizing frame by frame was never followed.
        LaunchedEffect(Unit) {
            snapshotFlow { target }.filter { it >= 0 }.collect { to ->
                val following = lastChange[0]?.let { it.elapsedNow() < FOLLOW_GAP } == true
                if (!instant) lastChange[0] = TimeSource.Monotonic.markNow()
                launch {
                    when {
                        instant || height.value == 0f -> height.snapTo(to.toFloat())
                        following -> height.animateTo(to.toFloat(), spring(stiffness = Spring.StiffnessMedium))
                        else -> height.animateTo(to.toFloat(), tween(SheetMotion.HEIGHT_MS, easing = SheetMotion.HeightEasing))
                    }
                }
            }
        }
        clipToBounds().layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            if (placeable.height != target) {
                // Filling the space, or growing with it: the size follows the space, not the content.
                instant =
                    target < 0 ||
                    placeable.height >= constraints.maxHeight ||
                    (placeable.height > target && target >= lastMax[0])
                target = placeable.height
            }
            lastMax[0] = constraints.maxHeight
            val animated = height.value.roundToInt()
            val shown =
                if (instant || animated == 0) {
                    placeable.height
                } else {
                    animated.coerceIn(constraints.minHeight, constraints.maxHeight)
                }
            layout(placeable.width, shown) { placeable.place(0, 0) }
        }
    }

/** Size changes closer together than this (about two frames) come from content animating its own size. */
private val FOLLOW_GAP = 40.milliseconds

@Composable
private fun FormStep(
    route: Route,
    form: PaymentState.CardFormShown,
    config: SdkPaymentConfig,
    order: OrderSummary?,
    isRoot: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onPush: (Route) -> Unit,
    onPaySaved: (SavedCardInfo) -> Unit,
    onIntent: (PaymentIntent) -> Unit,
) {
    when (route) {
        Route.Methods -> Unit // drawn by the shared sheet in PaymentCheckout
        Route.NewCard ->
            CardFormScreen(
                config = config,
                onSubmit = onIntent,
                onBack = onBack,
                onClose = onClose,
                backIsClose = isRoot,
                order = order,
                canSaveCard = form.canSaveCard,
                payerData = form.payerData,
                showWallets = false,
                transfer = form.transfer,
                saveCardByDefault = form.saveCardByDefault,
                saveCardLocked = form.saveCardLocked,
                saveCardOnly = form.saveCardOnly,
            )
        Route.AllCards ->
            CardListScreen(
                savedCards = form.savedCards,
                onCardClick = onPaySaved,
                onAddNewCard = { onPush(Route.NewCard) },
                onUnbind = onIntent,
                onBack = onBack,
                onClose = onClose,
            )
        is Route.Saved ->
            SavedCardScreen(
                card = route.card,
                order = order,
                onBack = onBack,
                onSubmit = onIntent,
                cvcRequired = form.cvcRequired,
                payerData = form.payerData,
                onClose = onClose,
            )
    }
}
