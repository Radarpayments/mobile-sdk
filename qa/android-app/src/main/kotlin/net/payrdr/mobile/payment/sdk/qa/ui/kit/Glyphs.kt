package net.payrdr.mobile.payment.sdk.qa.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Text in the app's type and colour; every screen text goes through it so that nothing falls back to Material's. */
@Composable
internal fun QaText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = qaColors.ink,
    maxLines: Int = Int.MAX_VALUE,
) {
    QaText(AnnotatedString(text), style, modifier, color, maxLines)
}

/** [QaText] for text with spans, such as a bold method in front of a path. */
@Composable
internal fun QaText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = qaColors.ink,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color),
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}

internal enum class Pointing { LEFT, RIGHT, DOWN }

/** The two strokes of a chevron, the sides of a rotated square of [side]. */
@Composable
internal fun Chevron(
    pointing: Pointing,
    side: Dp,
    stroke: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    // A square of side s turned by 45 degrees spans s * sqrt(2) along its diagonal and half that across.
    val long = side * DIAGONAL
    val short = long / 2
    val box = if (pointing == Pointing.DOWN) Modifier.size(long, short) else Modifier.size(short, long)
    Canvas(modifier.then(box)) {
        val w = size.width
        val h = size.height
        val inset = stroke.toPx() / 2
        val points =
            when (pointing) {
                Pointing.LEFT -> listOf(Offset(w - inset, inset), Offset(inset, h / 2), Offset(w - inset, h - inset))
                Pointing.RIGHT -> listOf(Offset(inset, inset), Offset(w - inset, h / 2), Offset(inset, h - inset))
                Pointing.DOWN -> listOf(Offset(inset, inset), Offset(w / 2, h - inset), Offset(w - inset, inset))
            }
        val path = Path().apply { points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
        drawPath(path, color, style = Stroke(width = stroke.toPx(), cap = StrokeCap.Square))
    }
}

/** The chevron at the end of a row that opens a screen. */
@Composable
internal fun RowChevron() {
    Chevron(Pointing.RIGHT, side = 8.dp, stroke = 1.5.dp, color = qaColors.muted)
}

/** The overflow menu's mark: three small squares. */
@Composable
internal fun MenuDots() {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(DOTS) { Box(Modifier.size(4.dp).background(qaColors.ink)) }
    }
}

private const val DIAGONAL = 1.4142f
private const val DOTS = 3
