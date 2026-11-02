package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentShapes

/**
 * Bottom-sheet surface: rounded top corners, drag-handle pill, themed background, 14dp gaps
 * between children.
 *
 * The container is declarative only — dragging and scrim handling live in the host
 * (`PaymentCheckout` draws them for the turnkey checkout).
 *
 * @param onDismissRequest invoked when the drag-handle is tapped (optional affordance)
 * @param showGrabber hide the handle when the sheet is not draggable (e.g. inside a system sheet)
 */
@Composable
public fun BottomSheetContainer(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showGrabber: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    SheetSurface(onDismissRequest, modifier, showGrabber, SIDE_PADDING, content = content)
}

/** [BottomSheetContainer] with its side padding chosen by the caller (0 for steps that keep their own). */
@Composable
internal fun SheetSurface(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    showGrabber: Boolean,
    sidePadding: Dp,
    shape: Shape? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalPaymentColors.current
    val sheetShape = shape ?: LocalPaymentShapes.current.sheet

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(24.dp, sheetShape, clip = false)
                .background(color = colors.background, shape = sheetShape)
                // Swallow taps so they don't reach the scrim behind the sheet.
                .tapsOnly()
                .padding(horizontal = sidePadding)
                // Under the last button: the system inset (home indicator, gesture bar, keyboard) comes on top, so 12dp
                // is enough on its own and still keeps the button clear of the keyboard's top row.
                .padding(top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showGrabber) {
            SheetGrabber(modifier = Modifier.tapsOnly(onDismissRequest))
        }
        content()
    }
}

/** Side padding of every checkout screen's content — the sheet, its overlays and the pages alike. */
internal val SIDE_PADDING = 20.dp
