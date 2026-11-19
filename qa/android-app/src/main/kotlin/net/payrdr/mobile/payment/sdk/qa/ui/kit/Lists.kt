package net.payrdr.mobile.payment.sdk.qa.ui.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A 1 dp line along the top edge, over the content. */
@Composable
internal fun Modifier.topLine(): Modifier {
    val color = qaColors.line
    return drawWithContent {
        drawContent()
        drawRect(color, size = Size(size.width, QaSize.line.toPx()))
    }
}

/** A 1 dp line along the bottom edge, from [start] to the end, over the content. */
@Composable
internal fun Modifier.bottomLine(start: Dp = 0.dp): Modifier {
    val color = qaColors.line
    return drawWithContent {
        drawContent()
        val height = QaSize.line.toPx()
        drawRect(color, Offset(start.toPx(), size.height - height), Size(size.width - start.toPx(), height))
    }
}

/** A group's name over it: small capitals, 24 above and 8 below. */
@Composable
internal fun Caption(text: String) {
    QaText(
        text.uppercase(),
        QaType.caption,
        color = qaColors.muted,
        modifier =
            Modifier.fillMaxWidth().padding(
                start = QaSize.screen,
                end = QaSize.screen,
                top = QaSize.captionTop,
                bottom = QaSize.captionBottom,
            ),
    )
}

/**
 * Rows between two full lines. Every row draws its own divider inset by 16 at its bottom; the group's bottom line is
 * drawn over the last one, so a hidden row never leaves a double line.
 */
@Composable
internal fun ListGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().topLine().bottomLine(), content = content)
}

/** The base of every row: the height, the side padding, the 16 between its parts and the inset divider. */
@Composable
internal fun ListRow(
    modifier: Modifier = Modifier,
    minHeight: Dp = QaSize.row,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QaSize.screen),
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .bottomLine(start = QaSize.screen)
                .padding(horizontal = QaSize.screen),
        content = content,
    )
}

/** A row that opens a screen: a label, its value on the right (red when it is a problem) and a chevron. */
@Composable
internal fun NavRow(
    label: String,
    value: String,
    tag: String,
    onClick: () -> Unit,
    valueIsProblem: Boolean = false,
    readsAs: String? = null,
) {
    // A row that stands for a field reads as the field's value, as the field's element does on every other screen.
    var modifier = Modifier.clickable(role = Role.Button, onClick = onClick)
    if (readsAs != null) modifier = modifier.semantics { text = AnnotatedString(readsAs) }
    val drawn = if (readsAs != null) Modifier.clearAndSetSemantics {} else Modifier
    ListRow(modifier.testTag(tag)) {
        QaText(label, QaType.body, maxLines = 1, modifier = drawn)
        if (readsAs != null) {
            QaText(
                value,
                QaType.value.copy(textAlign = TextAlign.End),
                color = if (valueIsProblem) qaColors.bad else qaColors.muted,
                maxLines = 1,
                modifier = drawn.weight(1f),
            )
        } else {
            RowValue(value, valueIsProblem)
        }
        RowChevron()
    }
}

/** The right-aligned value of a row; it takes the room left and ends in an ellipsis when it does not fit. */
@Composable
internal fun RowScope.RowValue(
    value: String,
    isProblem: Boolean = false,
    tag: String? = null,
    text: String = value,
) {
    val style = QaType.value.copy(textAlign = TextAlign.End)
    val color = if (isProblem) qaColors.bad else qaColors.muted
    if (tag == null) {
        QaText(value, style, color = color, maxLines = 1, modifier = Modifier.weight(1f))
    } else {
        ValueText(value, text, tag, style, color, maxLines = 1, modifier = Modifier.weight(1f))
    }
}

/**
 * Text that a test finds by [tag] and reads as [value], which may differ from what is shown: a row's merged text would
 * otherwise swallow it, so it is a semantics node of its own that keeps [value] and hides the drawn text.
 */
@Composable
internal fun ValueText(
    display: String,
    value: String,
    tag: String,
    style: TextStyle,
    color: Color = qaColors.ink,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .semantics(mergeDescendants = true) { text = AnnotatedString(value) }
            .testTag(tag),
    ) {
        QaText(display, style, color = color, maxLines = maxLines, modifier = Modifier.fillMaxWidth().clearAndSetSemantics {})
    }
}

/** A row of two lines: the label and a monospace line under it; [trailing] goes on the right. */
@Composable
internal fun TwoLineRow(
    modifier: Modifier = Modifier,
    leading: @Composable RowScope.() -> Unit = {},
    text: @Composable ColumnScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    ListRow(modifier, minHeight = QaSize.rowTwoLines) {
        leading()
        Column(Modifier.weight(1f).padding(vertical = 12.dp), content = text)
        trailing()
    }
}

/** The second line of a two-line row. */
@Composable
internal fun SecondLine(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = qaColors.muted,
) {
    QaText(text, QaType.value, color = color, maxLines = 1, modifier = modifier)
}

/** A row that turns a setting on and off; the whole row toggles and carries [tag]. */
@Composable
internal fun SwitchRow(
    label: String,
    checked: Boolean,
    tag: String,
    onChange: (Boolean) -> Unit,
) {
    ListRow(Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange).testTag(tag)) {
        QaText(label, QaType.body, maxLines = 1, modifier = Modifier.weight(1f))
        QaSwitch(checked)
    }
}

/** A row of a one-of list: the mark, the label (bold when chosen). */
@Composable
internal fun MarkRow(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    ListRow(Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick).testTag(tag)) {
        Mark(selected)
        QaText(label, if (selected) QaType.bodyStrong else QaType.body, maxLines = 1, modifier = Modifier.weight(1f))
    }
}
