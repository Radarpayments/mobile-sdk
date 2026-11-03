package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.payment.ui.components.DraggableSheet
import net.payrdr.mobile.payment.sdk.payment.ui.components.SIDE_PADDING
import net.payrdr.mobile.payment.sdk.payment.ui.components.tapsOnly
import net.payrdr.mobile.payment.sdk.payment.ui.motion.SheetMotion
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Between the status bar and the top of a sheet that is not opened to the whole screen. */
private val SHEET_TOP_GAP = 24.dp

/** The default sheet's top corners ([PaymentShapes.sheet][net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentShapes]). */
private val SHEET_CORNER = 26.dp

/** The sheet slides away, the scrim fades: on the sheet's curve, or a short fade with reduced motion. */
internal fun <T> sheetCloseSpec(reduceMotion: Boolean) =
    if (reduceMotion) {
        tween<T>(SheetMotion.REDUCED_SHEET_CLOSE_MS)
    } else {
        tween(SheetMotion.SHEET_CLOSE_MS, easing = SheetMotion.SheetEasing)
    }

/** The dimmed merchant screen under the sheet: fades in with the sheet, [onClick] on a tap. */
@Composable
internal fun Scrim(
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
internal fun SheetHost(
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
            val corner = SHEET_CORNER * (1f - open)
            Box(
                Modifier.fillMaxSize().padding(top = (statusBar + SHEET_TOP_GAP) * (1f - open)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                DraggableSheet(
                    onDismiss = onDismiss,
                    draggable = draggable,
                    showGrabber = showGrabber,
                    sidePadding = sidePadding,
                    shape = if (open > 0f) RoundedCornerShape(topStart = corner, topEnd = corner) else null,
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
internal val LocalSheetEntered = staticCompositionLocalOf<State<Boolean>> { mutableStateOf(true) }

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
internal fun Modifier.smoothHeight(): Modifier =
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
