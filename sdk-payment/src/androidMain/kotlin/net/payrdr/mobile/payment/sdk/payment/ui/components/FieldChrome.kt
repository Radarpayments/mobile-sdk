package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography

/** Top edge of a [FormField]'s control: align side-by-side fields with `Modifier.alignBy(FieldFrameTop)`. */
internal val FieldFrameTop = HorizontalAlignmentLine(::minOf)

/**
 * Label above, control, then one line of helper or error text below — the layout every checkout
 * input shares.
 *
 * @param message helper text, or the error text when [isError]; null hides the line
 * @param labelActions optional actions aligned to the right of the label (Scan)
 */
@Composable
internal fun FormField(
    label: String,
    message: String?,
    isError: Boolean,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    labelActions: (@Composable RowScope.() -> Unit)? = null,
    control: @Composable () -> Unit,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    Column(modifier = modifier.keepVisibleWhileFocused(focused), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = typography.label.copy(color = colors.onSurfaceVariant),
                modifier = Modifier.weight(1f),
            )
            if (labelActions != null) {
                // Pull the 40dp tap targets into the label's line height.
                Row(modifier = Modifier.height(20.dp), verticalAlignment = Alignment.CenterVertically, content = labelActions)
            }
        }
        // Reports where the frame starts, so fields side by side line up their frames whatever the label wraps to.
        Box(
            Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height, mapOf(FieldFrameTop to 0)) { placeable.place(0, 0) }
            },
        ) { control() }
        if (!message.isNullOrEmpty()) {
            Text(
                text = message,
                style = typography.caption.copy(color = if (isError) colors.error else colors.onSurfaceVariant),
                // An error is read out as it appears, without the buyer looking for it.
                modifier = if (isError) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier,
            )
        }
    }
}

/**
 * [FIELD_HEIGHT] input frame: 1.5dp outline that turns accent when [focused] and error colour when
 * [isError]. [content] is laid out in a row so prefixes and brand badges sit inside the frame.
 *
 * @param valid shows a green check at the right edge: the value is filled in and correct (never with an error)
 */
@Composable
internal fun FieldBox(
    focused: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    valid: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = LocalPaymentColors.current
    val shapes = LocalPaymentShapes.current
    val borderColor =
        when {
            readOnly -> colors.surface
            isError -> colors.error
            focused -> colors.primary
            else -> colors.outline
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(FIELD_HEIGHT)
                .background(if (readOnly) colors.surface else colors.background, shapes.field)
                .border(1.5.dp, borderColor, shapes.field)
                .padding(start = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        content()
        if (valid && !isError) {
            Icon(PaymentIcons.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Keeps a focused field — label, frame and message — visible above the keyboard. Scrolls to it when it
 * gains focus and again whenever the keyboard height or the field's own size changes (the keyboard
 * type switches, an error appears), one frame later so that the layout has settled. Needed because
 * Compose alone scrolls only once, on focus.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.keepVisibleWhileFocused(focused: Boolean): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val density = LocalDensity.current
    val keyboard = WindowInsets.ime.getBottom(density)
    var size by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(focused, keyboard, size) {
        if (!focused || size == IntSize.Zero) return@LaunchedEffect
        withFrameNanos { }
        val margin = with(density) { FOCUS_MARGIN.toPx() }
        requester.bringIntoView(Rect(0f, -margin, size.width.toFloat(), size.height + margin))
    }
    return bringIntoViewRequester(requester).onSizeChanged { size = it }
}

/** Air kept around a focused field when it is scrolled into view. */
private val FOCUS_MARGIN = 12.dp

/** Height of every input: single fields and the cells of every field group, the labeled ones included. */
internal val FIELD_HEIGHT = 54.dp
