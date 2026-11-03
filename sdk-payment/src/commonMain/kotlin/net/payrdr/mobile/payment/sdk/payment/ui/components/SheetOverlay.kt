package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.payment.ui.PlatformBackHandler
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import kotlin.math.roundToInt

/**
 * Dimmed scrim with a bottom sheet sliding up over it. Tapping the scrim or dragging the sheet
 * down past a threshold calls [onDismiss].
 *
 * @param showScrim false when something behind already dims the screen
 */
@Composable
internal fun SheetOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showScrim: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalPaymentColors.current
    // System back closes the sheet, not the screen under it.
    PlatformBackHandler(enabled = visible, onBack = onDismiss)
    CoverWhile(visible)
    Box(modifier = modifier.fillMaxSize()) {
        if (showScrim) {
            AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(colors.scrim)
                        .tapsOnly(onDismiss),
                )
            }
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(280)) { it },
            exit = slideOutVertically(tween(220)) { it },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 24.dp),
        ) {
            DraggableSheet(onDismiss = onDismiss, content = content)
        }
    }
}

/** [SheetSurface] that follows a downward drag and either dismisses or springs back. */
@Composable
internal fun DraggableSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    draggable: Boolean = true,
    // The handle may stay while dragging is off (a payment in flight), so the sheet's top does not jump.
    showGrabber: Boolean = draggable,
    sidePadding: Dp = SIDE_PADDING,
    shape: Shape? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val threshold = with(LocalDensity.current) { 120.dp.toPx() }
    SheetSurface(
        onDismissRequest = if (draggable) onDismiss else ({}),
        showGrabber = showGrabber,
        sidePadding = sidePadding,
        shape = shape,
        modifier =
            modifier
                .offset { IntOffset(0, offset.value.roundToInt()) }
                .then(
                    if (draggable) {
                        Modifier.draggable(
                            orientation = Orientation.Vertical,
                            state =
                                rememberDraggableState { delta ->
                                    scope.launch { offset.snapTo((offset.value + delta).coerceAtLeast(0f)) }
                                },
                            onDragStopped = { velocity ->
                                if (offset.value > threshold || velocity > 2400f) {
                                    onDismiss()
                                    // The host may keep the sheet (a step inside it goes back instead of closing).
                                    offset.animateTo(0f)
                                } else {
                                    offset.animateTo(0f)
                                }
                            },
                        )
                    } else {
                        Modifier
                    },
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .sheetSafeInsets(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}
