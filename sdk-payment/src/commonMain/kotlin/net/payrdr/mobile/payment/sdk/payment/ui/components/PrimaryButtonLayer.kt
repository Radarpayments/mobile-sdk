package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

/** What a step's main button shows. */
internal data class PrimaryButtonFace(
    val text: String,
    val icon: ImageVector?,
    val kind: ButtonKind,
    val enabled: Boolean,
    val loading: Boolean,
)

/**
 * Where a step's main button stands: the step lays out an empty slot, and the sheet's [PrimaryButtonLayer] draws the
 * button over it from outside the moving steps.
 *
 * @param alpha how far the step has faded in or out
 * @param settled false while the step moves: the layer follows the slot frame by frame only then
 */
internal class PrimaryButtonSlot(
    face: PrimaryButtonFace,
    val alpha: State<Float>,
    val settled: State<Boolean>,
) {
    var face by mutableStateOf(face)

    /** Read when the button is tapped, not drawn. */
    var onClick: () -> Unit = {}

    /** The slot's layout, read for where the step's zoom has taken it; null until it is placed. */
    var coordinates: LayoutCoordinates? = null

    /** Counts the slot's placements, so that the layer follows it when it moves while its step stands. */
    var placements by mutableIntStateOf(0)
}

/**
 * A layer of the sheet, over the steps and out of their transitions, that holds their main button. From a step with
 * one to the next with one, the button stays where it stood and only its label changes ([CascadeLabel]); coming or
 * going with a step that has none, it zooms and fades with its own step as any of its content.
 */
internal class PrimaryButtonLayer {
    /** The steps' slots on screen, the step that came in last at the end. */
    val slots = mutableStateListOf<PrimaryButtonSlot>()

    /** Whether the latest change of step went deeper into the flow: the label turns over the way the flow went. */
    var forward: Boolean = true
}

/** The sheet's [PrimaryButtonLayer]; null outside it, where a main button draws itself in place. */
internal val LocalPrimaryButtonLayer = staticCompositionLocalOf<PrimaryButtonLayer?> { null }

/**
 * Where the layer draws the button: its unzoomed [size] about a centre [centerX] across and [centerAbove] above the
 * bottom, at [scale] and [alpha]. From the bottom, as the sheet stands on it: a frame read before the sheet's height
 * changed still puts the button in place.
 */
private class ButtonFrame(
    val centerX: Float,
    val centerAbove: Float,
    val size: IntSize,
    val scale: Float,
    val alpha: Float,
)

/** Where the button stood last time its step stood still, measured from the bottom: the sheet changes height above it. */
private class ButtonAnchor(
    val centerX: Float,
    val size: IntSize,
    val bottomGap: Float,
)

/** Draws [layer]'s button; [modifier] gives it the whole area the steps take. */
@Composable
internal fun PrimaryButtonLayerHost(
    layer: PrimaryButtonLayer,
    modifier: Modifier,
) {
    val slot = layer.slots.lastOrNull()
    val host = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val anchor = remember { arrayOfNulls<ButtonAnchor>(1) }
    // Read where the button goes in layout, not in composition: the steps' frames move it without recomposing.
    var frame by remember { mutableStateOf<ButtonFrame?>(null) }

    fun follow() {
        val from = host[0]?.takeIf { it.isAttached } ?: return
        val newest = layer.slots.lastOrNull()
        val to = newest?.coordinates?.takeIf { it.isAttached }
        if (to == null) {
            frame = null
            return
        }
        val held = anchor[0]
        frame =
            if (layer.slots.size > 1 && held != null) {
                // One step hands the button to the next: it stays where it stood, and grows or narrows to the new
                // step's width as that step comes in.
                val progress = newest.alpha.value
                val centerX = from.localBoundingBoxOf(to, clipBounds = false).center.x
                ButtonFrame(
                    centerX = lerp(held.centerX, centerX, progress),
                    centerAbove = held.bottomGap + held.size.height / 2f,
                    size =
                        IntSize(
                            lerp(held.size.width.toFloat(), to.size.width.toFloat(), progress).roundToInt(),
                            lerp(held.size.height.toFloat(), to.size.height.toFloat(), progress).roundToInt(),
                        ),
                    scale = 1f,
                    alpha = 1f,
                )
            } else {
                val bounds = from.localBoundingBoxOf(to, clipBounds = false)
                if (newest.settled.value) anchor[0] = ButtonAnchor(bounds.center.x, to.size, from.size.height - bounds.bottom)
                ButtonFrame(
                    centerX = bounds.center.x,
                    centerAbove = from.size.height - bounds.center.y,
                    size = to.size,
                    scale = if (to.size.width > 0) bounds.width / to.size.width else 1f,
                    alpha = newest.alpha.value,
                )
            }
    }

    LaunchedEffect(layer) {
        snapshotFlow { layer.slots.map { it.settled.value to it.placements } }
            .collectLatest { slots ->
                follow()
                // Frame by frame while a step moves, and once more on the frame after they all stand.
                while (slots.any { !it.first }) {
                    withFrameNanos { }
                    follow()
                }
                withFrameNanos { }
                follow()
            }
    }

    Box(
        modifier.onPlaced {
            host[0] = it
            follow()
        },
    ) {
        if (slot == null) return@Box
        val face = slot.face
        ButtonSurface(
            face = face,
            onClick = {
                layer.slots
                    .lastOrNull()
                    ?.onClick
                    ?.invoke()
            },
            motion =
                when {
                    layer.slots.size <= 1 -> LabelMotion.InPlace
                    layer.forward -> LabelMotion.Forward
                    else -> LabelMotion.Back
                },
            modifier =
                Modifier.layout { measurable, constraints ->
                    val f = frame ?: return@layout layout(0, 0) {}
                    val placeable = measurable.measure(Constraints.fixed(f.size.width, f.size.height))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.placeWithLayer(
                            (f.centerX - f.size.width / 2f).roundToInt(),
                            (constraints.maxHeight - f.centerAbove - f.size.height / 2f).roundToInt(),
                        ) {
                            scaleX = f.scale
                            scaleY = f.scale
                            alpha = f.alpha
                        }
                    }
                },
        )
    }
}
