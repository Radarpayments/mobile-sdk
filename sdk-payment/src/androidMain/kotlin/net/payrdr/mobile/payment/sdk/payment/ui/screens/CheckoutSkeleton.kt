package net.payrdr.mobile.payment.sdk.payment.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.presentation.SkeletonLayout
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.components.FIELD_HEIGHT
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalPlaceholder
import net.payrdr.mobile.payment.sdk.payment.ui.components.PlaceholderBlock
import net.payrdr.mobile.payment.sdk.payment.ui.components.StepHeader
import net.payrdr.mobile.payment.sdk.payment.ui.components.WalletAvailability
import net.payrdr.mobile.payment.sdk.payment.ui.motion.SheetMotion
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentShapes
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.a11y_loading
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.checkout_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.stringResource

/** Stand-ins for the saved cards: realistic text so the blocks come out about as wide as the real lines. */
private fun skeletonCards(count: Int): List<SavedCardInfo> =
    List(count) { SavedCardInfo("skeleton-$it", "000000**0000", "VISA", "", canDelete = true, expiry = "00/00") }

/**
 * Alpha of the skeleton's blocks: 0 while [visible] is false (the sheet rises with the body laid out but empty), then
 * a short fade in and a slow pulse.
 */
@Composable
private fun skeletonAlpha(visible: Boolean): Float {
    val shown by animateFloatAsState(if (visible) 1f else 0f, tween(SheetMotion.SKELETON_FADE_MS), label = "skeleton-in")
    val pulse by rememberInfiniteTransition(label = "skeleton-pulse").animateFloat(
        initialValue = 1f,
        targetValue = SheetMotion.PULSE_MIN_ALPHA,
        animationSpec = infiniteRepeatable(tween(SheetMotion.PULSE_MS / 2), RepeatMode.Reverse),
        label = "skeleton-pulse",
    )
    return shown * pulse
}

/**
 * The payment sheet before the session is in: the real header, then [PaymentMethodsBody] drawn as blocks — the same
 * components and spacing, so the sheet is as tall as it will be with the content.
 */
@Composable
internal fun ColumnScope.PaymentMethodsSkeleton(
    layout: SkeletonLayout,
    visible: Boolean,
    onClose: () -> Unit,
) {
    StepHeader(title = stringResource(Res.string.checkout_title), onBack = null, onClose = onClose)
    val alpha = skeletonAlpha(visible)
    val loading = stringResource(Res.string.a11y_loading)
    CompositionLocalProvider(LocalPlaceholder provides true) {
        Column(
            // The stand-in rows are not read: the screen reader hears that the methods are loading.
            modifier =
                Modifier.graphicsLayer { this.alpha = alpha }.clearAndSetSemantics {
                    contentDescription = loading
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                },
            verticalArrangement = Arrangement.spacedBy(SHEET_ITEM_SPACING),
        ) {
            PaymentMethodsBody(
                wallets = WalletAvailability(googlePay = layout.wallet, applePay = false),
                order = null,
                savedCards = skeletonCards(layout.savedCards),
                onGooglePay = {},
                onApplePay = {},
                onPaySaved = {},
                onNewCard = {},
                onAllCards = {},
            )
        }
    }
}

/**
 * The card form as the first step, before the session is in: the real header, the card fields' frame and the pay
 * button as blocks. Its payer fields and the save-card switch depend on the session, so it is only about as tall.
 */
@Composable
internal fun CardFormSkeleton(
    visible: Boolean,
    onClose: () -> Unit,
) {
    val colors = LocalPaymentColors.current
    val shapes = LocalPaymentShapes.current
    val alpha = skeletonAlpha(visible)
    Column(Modifier.fillMaxWidth()) {
        StepHeader(title = stringResource(Res.string.card_form_title), onBack = null, onClose = onClose)
        val loading = stringResource(Res.string.a11y_loading)
        Column(
            modifier =
                Modifier
                    .graphicsLayer { this.alpha = alpha }
                    .clearAndSetSemantics {
                        contentDescription = loading
                        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                    }.padding(top = 6.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
                PlaceholderBlock(Modifier.size(width = 96.dp, height = 13.dp))
                Box(Modifier.weight(1f))
                PlaceholderBlock(Modifier.size(width = 104.dp, height = 13.dp))
            }
            Column(Modifier.fillMaxWidth().border(1.5.dp, colors.outline, shapes.card)) {
                Box(Modifier.fillMaxWidth().height(FIELD_HEIGHT), contentAlignment = Alignment.CenterStart) {
                    PlaceholderBlock(Modifier.padding(start = 14.dp).size(width = 110.dp, height = 14.dp))
                }
                Box(Modifier.fillMaxWidth().height(FIELD_HEIGHT), contentAlignment = Alignment.CenterStart) {
                    PlaceholderBlock(Modifier.padding(start = 14.dp).size(width = 64.dp, height = 14.dp))
                }
            }
        }
        PlaceholderBlock(
            Modifier
                .graphicsLayer { this.alpha = alpha }
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(54.dp),
            shapes.button,
        )
    }
}
