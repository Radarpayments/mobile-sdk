package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.collectLatest
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.colorsAreDark
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView
import kotlin.math.roundToInt

/**
 * iOS actual: the real `PKPaymentButton`, drawn under the steps where its slot is. Every frame its step moves, it takes
 * the slot's place and size from layout (which the step's zoom is part of) and its opacity from the step.
 *
 * One button for the whole checkout, made once the sheet stands ([enabled]) or as a step shows it, whichever comes first,
 * and kept while no step shows it: PassKit draws its face out of process and late, and a button made only as its step
 * comes in would show without one.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun WalletButtonLayerHost(
    layer: WalletButtonLayer,
    enabled: Boolean,
    modifier: Modifier,
) {
    val slot = layer.slot
    if (!enabled && slot == null) return
    val colors = LocalPaymentColors.current
    val dark = colorsAreDark(colors)
    val radius = LocalPaymentShapes.current.button.walletButtonCornerRadius()
    val density = LocalDensity.current
    val latestSlot = rememberUpdatedState(slot)
    val host = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val button = remember { arrayOfNulls<UIView>(1) }
    // Read where the view is placed and how big it is, not in composition: the step's frames move it without recomposing.
    val center = remember { mutableStateOf<Offset?>(null) }
    val size = remember { mutableStateOf(IntSize.Zero) }
    val placed = remember { mutableStateOf(false) }
    // Once a step has shown the button PassKit has drawn its face, and the idle button can go fully transparent.
    val shown = remember { booleanArrayOf(false) }

    // Puts the button where the slot is now, at its step's zoom and opacity; with no slot, out of sight where it was.
    fun follow() {
        val from = host[0] ?: return
        if (!from.isAttached) return
        val current = latestSlot.value
        val to = current?.coordinates?.takeIf { it.isAttached }
        var scale = 1f
        if (to != null) {
            val bounds = from.localBoundingBoxOf(to, clipBounds = false)
            scale = if (to.size.width > 0) bounds.width / to.size.width else 1f
            center.value = bounds.center
            size.value = to.size
        } else if (!shown[0]) {
            // Before any step has shown it: under the main button, which hides it; across the bottom of the sheet while
            // there is none. Followed as the sheet grows: placed only once, it stayed where the sheet's first, short frame
            // had put it, and showed through the clear step there.
            val parking = layer.parking()
            val under = parking?.coordinates?.takeIf { it.isAttached }
            val height = with(density) { WalletButtonHeight.roundToPx() }
            if (parking != null && under != null) {
                if (parking.settled.value || center.value == null) {
                    size.value = IntSize(under.size.width, height)
                    center.value = from.localBoundingBoxOf(under, clipBounds = false).center
                }
            } else {
                val side = with(density) { SIDE_PADDING.roundToPx() }
                size.value = IntSize(from.size.width - 2 * side, height)
                center.value = Offset(from.size.width / 2f, from.size.height - height / 2f)
            }
        }
        placed.value = true
        val pt = density.density.toDouble()
        val slotSize = size.value
        button[0]?.apply {
            setBounds(CGRectMake(0.0, 0.0, slotSize.width / pt, slotSize.height / pt))
            setCenter(CGPointMake(slotSize.width * VIEW_MARGIN / pt / 2, slotSize.height * VIEW_MARGIN / pt / 2))
            transform = CGAffineTransformMakeScale(scale.toDouble(), scale.toDouble())
            alpha =
                when {
                    to != null -> current.alpha.value.toDouble()
                    shown[0] -> 0.0
                    else -> DRAWING_ALPHA
                }
            if (to != null && current.alpha.value > SHOWN_ALPHA) shown[0] = true
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            val parking = layer.parking()
            val moves = listOf(latestSlot.value, latestSlot.value?.placements, parking?.placements, parking?.settled?.value)
            moves to (latestSlot.value?.settled?.value ?: true)
        }.collectLatest { (_, settled) ->
            follow()
            // Frame by frame while the step moves, and once more on the frame after it stands.
            do {
                withFrameNanos { }
                follow()
            } while (!settled)
        }
    }

    Box(
        modifier.onPlaced {
            host[0] = it
            follow()
        },
    ) {
        if (!placed.value) return@Box
        key(dark, radius, colors.background) {
            UIKitView(
                // A little larger than the slot, so that the button zoomed past its size (a step leaving forward) stays
                // whole: the view shows only inside its own hole in the canvas.
                modifier =
                    Modifier
                        .offset {
                            val c = center.value ?: Offset.Zero
                            val s = size.value
                            IntOffset((c.x - s.width * VIEW_MARGIN / 2).roundToInt(), (c.y - s.height * VIEW_MARGIN / 2).roundToInt())
                        }.size(
                            with(density) { (size.value.width * VIEW_MARGIN).toDp() },
                            with(density) { (size.value.height * VIEW_MARGIN).toDp() },
                        ),
                factory = {
                    val pay = payButton(dark, radius) { latestSlot.value?.onClick?.invoke() }
                    button[0] = pay
                    follow()
                    // The interop view sits under a hole in the Compose canvas: without an opaque backdrop the rounded
                    // corners, and the whole button while it is faint, would show the host screen.
                    UIView().apply {
                        backgroundColor = colors.background.toUIColor()
                        addSubview(pay)
                    }
                },
            )
        }
    }
}

/** How much larger than its slot the button's view is, for the zoom past full size. */
private const val VIEW_MARGIN = 1.1f

/** All but transparent before a step first shows the button: PassKit still draws the face of a button this faint. */
private const val DRAWING_ALPHA = 0.01

/** Opacity past which the button counts as shown, its face drawn. */
private const val SHOWN_ALPHA = 0.5f
