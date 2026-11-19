package net.payrdr.mobile.payment.sdk.qa.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The mark of a one-of choice: an empty square, or a filled one inside a ring when chosen. */
@Composable
internal fun Mark(selected: Boolean) {
    val colors = qaColors
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp).border(QaSize.line, colors.ink)) {
        if (selected) Box(Modifier.size(8.dp).background(colors.ink))
    }
}

/** The square switch: a knob on the left when off; on, the track fills and the knob moves right in the background colour. */
@Composable
internal fun QaSwitch(checked: Boolean) {
    val colors = qaColors
    Box(
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        modifier =
            Modifier
                .size(44.dp, 24.dp)
                .background(if (checked) colors.ink else Color.Transparent)
                .border(QaSize.line, colors.ink)
                .padding(3.dp),
    ) {
        Box(Modifier.size(16.dp).background(if (checked) colors.bg else colors.ink))
    }
}

/** The small coloured square in front of a run's outcome. */
@Composable
internal fun OutcomeSquare(color: Color) {
    Box(Modifier.size(10.dp).background(color))
}

/** One option of a [Segmented] control. */
internal class Segment(
    val value: String,
    val label: String,
    val tag: String,
)

/**
 * Two to four options side by side in one ink frame, the chosen one inverted. [tag] goes on the frame, which reads as the
 * chosen [Segment.value], so the field's identifier keeps carrying the field's value; each option has its own tag.
 */
@Composable
internal fun Segmented(
    segments: List<Segment>,
    selected: String,
    tag: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = qaColors
    Row(
        modifier
            .padding(horizontal = QaSize.screen)
            .fillMaxWidth()
            .height(QaSize.control)
            .border(QaSize.line, colors.ink)
            .semantics { text = AnnotatedString(selected) }
            .testTag(tag)
            .selectableGroup(),
    ) {
        segments.forEachIndexed { index, segment ->
            if (index > 0) Box(Modifier.width(QaSize.line).fillMaxHeight().background(colors.ink))
            val on = segment.value == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (on) colors.ink else Color.Transparent)
                        .selectable(selected = on, role = Role.RadioButton) { onSelect(segment.value) }
                        .testTag(segment.tag),
            ) {
                QaText(segment.label, QaType.label, color = if (on) colors.bg else colors.ink, maxLines = 1)
            }
        }
    }
}

/** The line under a field: a hint in grey or the field's problem in red, 8 below the field. */
@Composable
internal fun Help(
    text: String,
    isProblem: Boolean = false,
    modifier: Modifier = Modifier,
) {
    QaText(
        text,
        QaType.help,
        color = if (isProblem) qaColors.bad else qaColors.muted,
        modifier = modifier.fillMaxWidth().padding(start = QaSize.screen, end = QaSize.screen, top = QaSize.gap),
    )
}

/** A grey block that explains something about the screen. */
@Composable
internal fun Note(text: String) {
    QaText(
        text,
        QaType.help,
        color = qaColors.muted,
        modifier =
            Modifier
                .padding(start = QaSize.screen, end = QaSize.screen, top = 24.dp)
                .fillMaxWidth()
                .background(qaColors.fill)
                .padding(horizontal = QaSize.screen, vertical = 12.dp),
    )
}

/** An underlined word that acts inside a row or a field; its touch area is at least 44 square. */
@Composable
internal fun Link(
    text: String,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .heightIn(min = QaSize.control)
                .widthIn(min = QaSize.control)
                .clickable(role = Role.Button, onClick = onClick)
                .testTag(tag),
    ) {
        QaText(text, QaType.link)
    }
}

/**
 * A chip of a set: 36 tall, inverted when on. It sits in a 44 tall touch area, and [ChipFlow] stacks those areas with no
 * gap, which leaves the 8 between the chips the design asks for.
 */
@Composable
internal fun Chip(
    text: String,
    on: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val colors = qaColors
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .height(QaSize.control)
                .toggleable(value = on, role = Role.Checkbox, onValueChange = { onClick() })
                .testTag(tag),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .height(QaSize.chip)
                    .background(if (on) colors.ink else Color.Transparent)
                    .border(QaSize.line, if (on) colors.ink else colors.line)
                    .padding(horizontal = 12.dp),
        ) {
            QaText(text, QaType.value, color = if (on) colors.bg else colors.ink, maxLines = 1)
        }
    }
}

/**
 * Lays its children out in lines, [spacing] between neighbours and no gap between the lines: the chips' 44 tall touch
 * areas meet, and the 36 tall chips in them show 8 between the lines.
 */
@Composable
internal fun ChipFlow(
    modifier: Modifier = Modifier,
    spacing: Dp = QaSize.gap,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier.padding(horizontal = QaSize.screen)) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        val width = constraints.maxWidth
        var x = 0
        var y = 0
        var lineHeight = 0
        val positions =
            placeables.map { placeable ->
                if (x > 0 && x + placeable.width > width) {
                    x = 0
                    y += lineHeight
                    lineHeight = 0
                }
                val position = x to y
                x += placeable.width + gap
                lineHeight = maxOf(lineHeight, placeable.height)
                position
            }
        layout(width, y + lineHeight) {
            placeables.forEachIndexed { index, placeable -> placeable.place(positions[index].first, positions[index].second) }
        }
    }
}

/** A frame for an input: 52 tall, a grey line that turns ink on focus and red on a problem, 16 inside. */
@Composable
internal fun FieldFrame(
    focused: Boolean,
    isProblem: Boolean,
    modifier: Modifier = Modifier,
    minHeight: Dp = QaSize.field,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = qaColors
    val border =
        when {
            isProblem -> colors.bad
            focused -> colors.ink
            else -> colors.line
        }
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
            modifier
                .padding(horizontal = QaSize.screen)
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .border(QaSize.line, border),
        content = content,
    )
}
