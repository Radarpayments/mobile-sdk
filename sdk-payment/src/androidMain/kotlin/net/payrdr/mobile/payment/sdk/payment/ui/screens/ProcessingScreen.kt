package net.payrdr.mobile.payment.sdk.payment.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.result.ProcessingStep
import net.payrdr.mobile.payment.sdk.payment.ui.components.PaymentIcons
import net.payrdr.mobile.payment.sdk.payment.ui.components.ProgressRing
import net.payrdr.mobile.payment.sdk.payment.ui.motion.SheetMotion
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.theme.tabular
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.StringResource
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.processing_finalizing
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.processing_hint
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.processing_loading_public_key
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.processing_loading_session
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.processing_submitting_payment
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.processing_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.processing_tokenizing_card
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.stringResource

/**
 * Full-screen wait while the payment is in flight: spinning ring around a lock, amount and
 * merchant, and a reassurance line so the buyer reads it as "paying", not "frozen".
 *
 * @param step current backend step; announced to accessibility services
 * @param order amount / merchant shown under the headline; null hides the line
 */
@Composable
internal fun ProcessingScreen(
    step: ProcessingStep?,
    modifier: Modifier = Modifier,
    order: OrderSummary? = null,
) {
    ProcessingContent(step, order, modifier.fillMaxSize().background(LocalPaymentColors.current.background))
}

/**
 * The wait itself, as tall as it needs: the full-screen [ProcessingScreen] and the checkout sheet both show it. The
 * current stage reads under the headline and changes with a short cross-fade.
 */
@Composable
internal fun ProcessingContent(
    step: ProcessingStep?,
    order: OrderSummary?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    val stepText = stringResource(stepLabel(step))

    Column(
        modifier =
            modifier
                .padding(24.dp)
                .testTag("ProcessingScreen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ProgressRing(icon = PaymentIcons.Lock, modifier = Modifier.semantics { contentDescription = stepText })
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(Res.string.processing_title),
            style = typography.headline.copy(color = colors.onBackground),
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (step != null) {
            // The whole width and no size animation: a status of another length would otherwise grow or shrink its
            // clipped box and seem to slide in from the side. It fades out and in where it stands.
            AnimatedContent(
                targetState = stepText,
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
                transitionSpec = {
                    (
                        fadeIn(tween(SheetMotion.STATUS_IN_MS, delayMillis = SheetMotion.STATUS_OUT_MS)) togetherWith
                            fadeOut(tween(SheetMotion.STATUS_OUT_MS))
                    ) using SizeTransform(clip = false) { _, _ -> snap() }
                },
                label = "processing-step",
            ) { text ->
                Text(
                    text = text,
                    style = typography.body.copy(color = colors.onSurfaceVariant),
                    textAlign = TextAlign.Center,
                    // Each new stage is read out, so a screen reader user knows the payment is moving on.
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .testTag("processing_step")
                            .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        val amountLine = order?.formattedAmount.orEmpty()
        if (amountLine.isNotEmpty()) {
            Text(
                text = ltr(amountLine),
                style = typography.body.tabular().copy(color = colors.onSurfaceVariant, fontWeight = typography.title.fontWeight),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Text(
            text = stringResource(Res.string.processing_hint),
            style = typography.body.copy(color = colors.onSurfaceVariant),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp).widthIn(max = 280.dp),
        )
    }
}

internal fun stepLabel(step: ProcessingStep?): StringResource =
    when (step) {
        ProcessingStep.LOADING_SESSION -> Res.string.processing_loading_session
        ProcessingStep.LOADING_PUBLIC_KEY -> Res.string.processing_loading_public_key
        ProcessingStep.TOKENIZING_CARD -> Res.string.processing_tokenizing_card
        ProcessingStep.SUBMITTING_PAYMENT -> Res.string.processing_submitting_payment
        ProcessingStep.FINALIZING -> Res.string.processing_finalizing
        null -> Res.string.processing_title
    }
