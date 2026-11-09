package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography

/** What one input of a field group shows besides its text. */
internal data class GroupFieldStatus(
    val isError: Boolean = false,
    val isValid: Boolean = false,
    /** What is wrong, read by screen readers with the field (the group shows it under its frame). */
    val message: String? = null,
)

/**
 * Where a cell sits in its group: true for each side that lies on the group's outer frame. The other sides
 * border a hairline, which a highlight covers.
 */
internal data class GroupCellEdges(
    val top: Boolean,
    val bottom: Boolean,
    val start: Boolean,
    val end: Boolean,
) {
    companion object {
        /** Edges of cell [column] of [columns] in row [row] of [rows]. */
        fun of(
            row: Int,
            rows: Int,
            column: Int = 0,
            columns: Int = 1,
        ) = GroupCellEdges(top = row == 0, bottom = row == rows - 1, start = column == 0, end = column == columns - 1)
    }
}

/** Label above a field group, with optional trailing content (an action or a note) on the same line. */
@Composable
internal fun FieldGroupHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    Row(modifier = modifier.fillMaxWidth().heightIn(min = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = typography.label.copy(color = colors.onSurfaceVariant), modifier = Modifier.weight(1f))
        // Pulls 40dp tap targets into the label's line height.
        if (trailing != null) Row(modifier = Modifier.height(20.dp), verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

/**
 * One frame split by hairlines; rows go in [content], separated with [GroupRowDivider].
 *
 * @param labeled the fields have no label of their own (billing, a transfer's sender and recipient): each cell names its
 *   field with a floating label, in taller rows. Never for the card, whose fields are named by their hints.
 */
@Composable
internal fun FieldGroupFrame(
    modifier: Modifier = Modifier,
    labeled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalGroupLabels provides labeled) { GroupFrame(modifier, content) }
}

@Composable
private fun GroupFrame(
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalPaymentColors.current
    val frame = LocalPaymentShapes.current.field
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(frame)
                .background(colors.background)
                // The frame goes under the cells, so a highlighted cell can draw over it.
                .drawBehind { drawFrame(frame, colors.outline) },
        content = content,
    )
}

/** A row of cells; [highlighted] lifts it over the hairlines around it while one of its cells is outlined. */
@Composable
internal fun GroupRow(
    highlighted: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(FIELD_HEIGHT).zIndex(if (highlighted) 1f else 0f),
        content = content,
    )
}

/** Hairline between two rows. */
@Composable
internal fun GroupRowDivider() {
    Box(Modifier.fillMaxWidth().height(GROUP_STROKE).background(LocalPaymentColors.current.outline))
}

/** Hairline between two cells of a row. */
@Composable
internal fun GroupCellDivider() {
    Box(Modifier.width(GROUP_STROKE).fillMaxHeight().background(LocalPaymentColors.current.outline))
}

/**
 * Text input cell. [highlight] outlines it (focus or error) over the frame and the hairlines next to it.
 *
 * @param placeholder the field's name: the hint inside the field, or in a [FieldGroupFrame] `labeled` its floating label
 * @param hint in a labeled group, shown under the risen label while the field is empty (a date's format)
 * @param trailing content after the input, before the check mark (e.g. a remove button)
 * @param autoFocus take focus when the cell appears (a row the user has just added)
 * @param label the field's name for screen readers, when [placeholder] is only a format ("MM/YY")
 */
@Composable
internal fun GroupTextCell(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    contentType: TextFieldContentType,
    status: GroupFieldStatus,
    highlight: Color?,
    edges: GroupCellEdges,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    autoFocus: Boolean = false,
    hint: String = "",
    label: String = placeholder,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val labeled = LocalGroupLabels.current
    GroupCell(highlight = highlight, edges = edges, modifier = modifier) {
        CompositionLocalProvider(LocalNativeFieldAutoFocus provides autoFocus) {
            if (labeled) {
                // Labeled groups hold no card data, so their fields are Compose ones on iOS too: the label is drawn
                // inside the field, where taps on it focus the field and the screen reader reads it with the value.
                ComposeTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = hint,
                    keyboardOptions = keyboardOptions,
                    contentType = contentType,
                    enabled = enabled,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onFocusChange = onFocusChange,
                    floatingLabel = FloatingLabel(placeholder, status.isError),
                    errorMessage = status.message,
                )
            } else {
                NativeTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = placeholder,
                    keyboardOptions = keyboardOptions,
                    contentType = contentType,
                    isError = status.isError,
                    enabled = enabled,
                    onFocusChange = onFocusChange,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    label = label,
                    errorMessage = status.message,
                )
            }
        }
        trailing?.invoke(this)
        if (status.isValid && !status.isError) ValidMark()
    }
}

/** Cell that only shows something (the card being paid with), laid out and outlined like a text cell. */
@Composable
internal fun GroupInfoCell(
    highlight: Color?,
    edges: GroupCellEdges,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) = GroupCell(highlight = highlight, edges = edges, modifier = modifier, content = content)

/** Tappable cell (a picker) laid out like a text cell. */
@Composable
internal fun GroupButtonCell(
    onClick: () -> Unit,
    highlight: Color?,
    edges: GroupCellEdges,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    GroupCell(
        highlight = highlight,
        edges = edges,
        modifier = modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        content = content,
    )
}

@Composable
private fun GroupCell(
    highlight: Color?,
    edges: GroupCellEdges,
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val frame = LocalPaymentShapes.current.field
    Row(
        modifier =
            modifier
                .height(FIELD_HEIGHT)
                .cellHighlight(highlight, frame.cornersOf(edges), edges)
                // Kept off the cell edges: on iOS a native input clears whatever was drawn under it, which would
                // erase the frame and the highlight strokes.
                .padding(start = 14.dp, end = 10.dp, top = EDGE_GAP, bottom = EDGE_GAP),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/**
 * A picker's content in a labeled group: the field's [label] alone while nothing is picked, risen over the picked
 * [value] once something is.
 */
@Composable
internal fun RowScope.GroupPickerContent(
    label: String,
    isError: Boolean,
    value: (@Composable RowScope.() -> Unit)?,
) {
    Box(Modifier.weight(1f).fillMaxHeight()) {
        if (value != null) {
            Row(
                modifier = Modifier.fillMaxSize().padding(top = LABELED_INPUT_TOP),
                verticalAlignment = Alignment.CenterVertically,
                content = value,
            )
        }
        GroupFieldLabel(label, raised = value != null, tone = groupLabelTone(raised = value != null, focused = false, isError))
    }
}

/** A field's name over its input in a labeled group, see [FieldGroupFrame]. */
internal class FloatingLabel(
    val text: String,
    val isError: Boolean,
)

/** How a floating label is coloured. */
internal enum class GroupLabelTone { HINT, NAME, FOCUS, ERROR }

/** A floating label stands over the value while the field has one or has focus, and in its place otherwise. */
internal fun groupLabelRaised(
    focused: Boolean,
    value: String,
): Boolean = focused || value.isNotEmpty()

internal fun groupLabelTone(
    raised: Boolean,
    focused: Boolean,
    isError: Boolean,
): GroupLabelTone =
    when {
        isError -> GroupLabelTone.ERROR
        !raised -> GroupLabelTone.HINT
        focused -> GroupLabelTone.FOCUS
        else -> GroupLabelTone.NAME
    }

/**
 * A field's name in the middle of its cell, as a hint, or [raised] to the top in small type over the value. One motion
 * for the change: it rises and shrinks together.
 */
@Composable
internal fun GroupFieldLabel(
    text: String,
    raised: Boolean,
    tone: GroupLabelTone,
) {
    val colors = LocalPaymentColors.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val progress by animateFloatAsState(if (raised) 1f else 0f, tween(LABEL_MOTION_MS), label = "group-label")
    val target =
        when (tone) {
            GroupLabelTone.HINT -> colors.placeholder
            GroupLabelTone.NAME -> colors.onSurfaceVariant
            GroupLabelTone.FOCUS -> colors.primary
            GroupLabelTone.ERROR -> colors.error
        }
    val color by animateColorAsState(target, tween(LABEL_MOTION_MS), label = "group-label-color")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
        Text(
            text = text,
            style = LocalPaymentTypography.current.input.copy(color = color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier.graphicsLayer {
                    val scale = 1f - (1f - RAISED_LABEL_SCALE) * progress
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(if (rtl) 1f else 0f, 0.5f)
                    translationY = -LABEL_RISE.toPx() * progress
                },
        )
    }
}

/** Set by a [FieldGroupFrame] whose cells carry floating labels. */
private val LocalGroupLabels = staticCompositionLocalOf { false }

@Composable
private fun ValidMark() {
    Icon(PaymentIcons.Check, contentDescription = null, tint = LocalPaymentColors.current.success, modifier = Modifier.size(18.dp))
}

/**
 * Outlines a cell in [color] over the hairlines on its inner sides and over the frame on its outer ones; drawn
 * last among its siblings so the hairlines do not cover it.
 */
private fun Modifier.cellHighlight(
    color: Color?,
    shape: Shape,
    edges: GroupCellEdges,
): Modifier =
    if (color == null) {
        this
    } else {
        zIndex(1f).drawWithContent {
            drawContent()
            val w = GROUP_STROKE.toPx()
            val left = if (edges.start) 0f else -w
            val up = if (edges.top) 0f else -w
            val right = size.width + if (edges.end) 0f else w
            val down = size.height + if (edges.bottom) 0f else w
            translate(left, up) { drawFrame(shape, color, Size(right - left, down - up)) }
        }
    }

/** Strokes [shape] inside [area] (the whole draw area by default), the way `Modifier.border` does. */
private fun DrawScope.drawFrame(
    shape: Shape,
    color: Color,
    area: Size = size,
) {
    val w = GROUP_STROKE.toPx()
    val outline = shape.createOutline(Size(area.width - w, area.height - w), layoutDirection, this)
    translate(w / 2, w / 2) { drawOutline(outline, color, style = Stroke(w)) }
}

/** This frame's corners where the cell meets them, square elsewhere. */
private fun Shape.cornersOf(edges: GroupCellEdges): Shape {
    val corners = this as? CornerBasedShape ?: return RectangleShape
    val square = CornerSize(0.dp)
    return corners.copy(
        topStart = if (edges.top && edges.start) corners.topStart else square,
        topEnd = if (edges.top && edges.end) corners.topEnd else square,
        bottomStart = if (edges.bottom && edges.start) corners.bottomStart else square,
        bottomEnd = if (edges.bottom && edges.end) corners.bottomEnd else square,
    )
}

private val GROUP_STROKE = 1.5.dp
private val EDGE_GAP = 2.dp

/** Where the value starts under a risen label, from the top of the cell's content. */
internal val LABELED_INPUT_TOP = 20.dp

/** How far the label rises from the middle of the cell, and to what size (16sp to 12sp). */
private val LABEL_RISE = 14.dp
private const val RAISED_LABEL_SCALE = 0.75f
private const val LABEL_MOTION_MS = 150
