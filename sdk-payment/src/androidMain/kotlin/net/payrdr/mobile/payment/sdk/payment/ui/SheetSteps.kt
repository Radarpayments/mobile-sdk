package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalPrimaryButtonLayer
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalStepAlpha
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalStepSettled
import net.payrdr.mobile.payment.sdk.payment.ui.components.PrimaryButtonLayer
import net.payrdr.mobile.payment.sdk.payment.ui.components.PrimaryButtonLayerHost
import net.payrdr.mobile.payment.sdk.payment.ui.motion.LocalStepBodyModifier
import net.payrdr.mobile.payment.sdk.payment.ui.motion.loadingRevealTransform
import net.payrdr.mobile.payment.sdk.payment.ui.motion.revealBody
import net.payrdr.mobile.payment.sdk.payment.ui.motion.stepAlpha
import net.payrdr.mobile.payment.sdk.payment.ui.motion.stepBlur
import net.payrdr.mobile.payment.sdk.payment.ui.motion.stepTransform

/** A change of step: the first form comes in under the loading header (see revealBody), any other step zooms whole. */
internal fun AnimatedContentTransitionScope<Step>.stepChange(reduceMotion: Boolean): ContentTransform =
    if (initialState == Step.Loading && targetState is Step.Form) {
        loadingRevealTransform(reduceMotion)
    } else {
        stepTransform(isForward(initialState, targetState), reduceMotion)
    }

/**
 * Every step of the sheet presentation in the one payment sheet: it slides in once, then grows or shrinks to each step
 * while the steps slide sideways. The main button is drawn over the steps, out of their transitions (see
 * PrimaryButtonLayer).
 *
 * @param current the step this sheet shows, as the checkout's own transition has it
 * @param step the step the checkout is going to: the bank page opens the sheet to the whole screen
 * @param revealKey the step coming in under the loading header, if one is
 * @param zoomReveal the revealed step zooms in (no skeleton was shown before it)
 * @param content one step inside the sheet
 */
@Suppress("LongParameterList")
@Composable
internal fun SheetSteps(
    current: Step,
    step: Step,
    revealKey: String?,
    zoomReveal: Boolean,
    reduceMotion: Boolean,
    onDismiss: () -> Unit,
    onExpanded: () -> Unit,
    onSheetHeight: (Int) -> Unit,
    buttonLayer: PrimaryButtonLayer,
    content: @Composable (Step) -> Unit,
) {
    SheetHost(
        CheckoutPresentation.Sheet,
        onDismiss = onDismiss,
        // Nothing to drag away while the payment is in flight.
        draggable = current !is Step.Processing && current != Step.ThreeDs,
        showGrabber = current != Step.ThreeDs,
        expanded = step == Step.ThreeDs,
        onExpanded = onExpanded,
        onSheetHeight = onSheetHeight,
        sidePadding = 0.dp,
        reduceMotion = reduceMotion,
    ) {
        Box {
            CompositionLocalProvider(LocalPrimaryButtonLayer provides buttonLayer) {
                AnimatedContent(
                    targetState = current,
                    contentKey = { it.key },
                    transitionSpec = { stepChange(reduceMotion) },
                    // On the sheet's bottom edge, as the sheet itself: a taller step rises above the shorter one and both
                    // pay buttons sit in the same place. Coming out of loading the headers must coincide instead — the
                    // loading layer stays under the new one — so the top edge.
                    contentAlignment = if (step.key == revealKey) Alignment.TopCenter else Alignment.BottomCenter,
                    label = "sheet-step",
                ) { inner ->
                    val sheetEntered = LocalSheetEntered.current
                    val settled =
                        remember { derivedStateOf { transition.currentState == transition.targetState && sheetEntered.value } }
                    // Changes inside one step (loading → methods, fields appearing) move the sheet's top as smoothly as a
                    // step change does, the bottom staying put. Every step learns when it has settled: a field that takes
                    // focus as it appears waits for it, see [LocalStepSettled].
                    val reveals = inner.key == revealKey
                    val body = if (reveals) Modifier.revealBody(this, zoom = zoomReveal, reduceMotion) else Modifier
                    val blur = if (inner.blursWhileChanging(revealKey)) Modifier.stepBlur(this, reduceMotion) else Modifier
                    val alpha = stepAlpha(this, reveals, reduceMotion)
                    Box(Modifier.smoothHeight().then(blur)) {
                        CompositionLocalProvider(
                            LocalStepSettled provides settled,
                            LocalStepBodyModifier provides body,
                            LocalStepAlpha provides alpha,
                        ) {
                            content(inner)
                        }
                    }
                }
            }
            PrimaryButtonLayerHost(buttonLayer, Modifier.matchParentSize())
        }
    }
}
