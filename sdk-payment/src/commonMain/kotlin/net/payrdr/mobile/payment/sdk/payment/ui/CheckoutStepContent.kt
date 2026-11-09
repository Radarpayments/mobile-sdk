package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.components.WalletAvailability
import net.payrdr.mobile.payment.sdk.payment.ui.screens.CardFormScreen
import net.payrdr.mobile.payment.sdk.payment.ui.screens.CardFormSkeleton
import net.payrdr.mobile.payment.sdk.payment.ui.screens.CardListScreen
import net.payrdr.mobile.payment.sdk.payment.ui.screens.PaymentMethodsContent
import net.payrdr.mobile.payment.sdk.payment.ui.screens.PaymentMethodsSkeleton
import net.payrdr.mobile.payment.sdk.payment.ui.screens.ProcessingContent
import net.payrdr.mobile.payment.sdk.payment.ui.screens.ResultContent
import net.payrdr.mobile.payment.sdk.payment.ui.screens.SavedCardScreen
import net.payrdr.mobile.payment.sdk.payment.ui.screens.SkeletonLayout
import net.payrdr.mobile.payment.sdk.payment.ui.screens.ThreeDsChallengeContent
import net.payrdr.mobile.payment.sdk.payment.ui.screens.isCardFormRoot
import net.payrdr.mobile.payment.sdk.payment.ui.screens.needsSavedCardScreen
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors

/** The first step's skeleton: the card form's when that will be the first step, the payment sheet's otherwise. */
@Composable
private fun ColumnScope.LoadingSkeleton(
    layout: SkeletonLayout,
    visible: Boolean,
    onClose: () -> Unit,
) {
    if (layout.isCardFormRoot) CardFormSkeleton(visible, onClose) else PaymentMethodsSkeleton(layout, visible, onClose)
}

/**
 * The payment sheet's first step: the [skeleton] while the session loads ([form] is null), then the payment methods
 * the form offers.
 *
 * @param skeletonVisible the loading has taken long enough for the skeleton to show
 * @param onPaySaved pays with a saved card from its row, or opens its own step when it needs one
 */
@Suppress("LongParameterList")
@Composable
internal fun ColumnScope.MethodsStep(
    form: PaymentState.CardFormShown?,
    skeleton: SkeletonLayout,
    skeletonVisible: Boolean,
    wallets: WalletAvailability?,
    order: OrderSummary?,
    onClose: () -> Unit,
    onIntent: (PaymentIntent) -> Unit,
    onPaySaved: (PaymentState.CardFormShown, SavedCardInfo) -> Unit,
    onPush: (Route) -> Unit,
) {
    if (form == null) {
        LoadingSkeleton(skeleton, visible = skeletonVisible, onClose = onClose)
    } else {
        PaymentMethodsContent(
            wallets = (wallets ?: WalletAvailability.None).offeredIn(form),
            order = order,
            savedCards = form.savedCards,
            onClose = onClose,
            onGooglePay = { onIntent(PaymentIntent.TapGooglePay) },
            onApplePay = { onIntent(PaymentIntent.TapApplePay) },
            onPaySaved = { onPaySaved(form, it) },
            onNewCard = { onPush(Route.NewCard) },
            onAllCards = { onPush(Route.AllCards) },
            opensCardStep = { needsSavedCardScreen(form, it) },
        )
    }
}

/**
 * Processing, the bank-page wait and the result: inside the sheet ([inSheet]) as tall as they need, filling the screen
 * as pages.
 *
 * @param canReturnToForm the checkout has a form to go back to (one that failed to start has none)
 * @param onClose leaves the result screen
 * @param onRetry goes back from a declined [Step.Result] to the payment methods
 */
@Suppress("LongParameterList")
@Composable
internal fun WaitOrResultStep(
    step: Step,
    inSheet: Boolean,
    order: OrderSummary?,
    canReturnToForm: Boolean,
    onCancel: () -> Unit,
    onClose: () -> Unit,
    onRetry: (Step.Result) -> Unit,
) {
    // As a page it fills the host and paints its own background (the host view may be clear).
    val fill =
        if (inSheet) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.fillMaxSize().background(LocalPaymentColors.current.background)
        }
    when (step) {
        is Step.Processing -> ProcessingContent(step = step.step, order = order, modifier = fill)
        Step.ThreeDs ->
            ThreeDsChallengeContent(
                onCancel = onCancel,
                order = order,
                bottomPadding = if (inSheet) 0.dp else 18.dp,
            )
        is Step.Result -> {
            val result = step.completed.result
            val retry = { onRetry(step) }
            ResultContent(
                result = result,
                receipt = step.completed.receipt,
                order = order,
                onClose = onClose,
                // Only with a form to go back to: a checkout that failed to start has none.
                onRetry = retry.takeIf { result is CheckoutResult.Failed && canReturnToForm },
                fillHeight = !inSheet,
                modifier = if (inSheet) fill else fill.windowInsetsPadding(WindowInsets.safeDrawing),
            )
        }
        else -> Unit
    }
}

/** A card step of the stack: the new card, all saved cards or one saved card. */
@Suppress("LongParameterList")
@Composable
internal fun FormStep(
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
