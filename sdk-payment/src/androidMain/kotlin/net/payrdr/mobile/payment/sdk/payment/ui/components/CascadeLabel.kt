package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.ui.motion.SheetMotion
import kotlin.math.max
import kotlin.math.roundToInt

/** A piece of a button's label that moves on its own when the label changes. */
internal sealed interface LabelPart {
    data class Icon(
        val vector: ImageVector,
    ) : LabelPart

    data class Word(
        val text: String,
    ) : LabelPart

    data object Spinner : LabelPart
}

/** A button's label as [CascadeLabel] moves it: the icon, then the words of [text]; only a spinner while [loading]. */
internal fun labelParts(
    text: String,
    icon: ImageVector?,
    loading: Boolean,
): List<LabelPart> =
    if (loading) {
        listOf(LabelPart.Spinner)
    } else {
        listOfNotNull(icon?.let(LabelPart::Icon)) + cascadeWords(text).map(LabelPart::Word)
    }

/** The words of [text]. A run isolated left to right ([ltr][net.payrdr.mobile.payment.sdk.payment.ui.util.ltr] — an amount, a card number) stays one word. */
internal fun cascadeWords(text: String): List<String> {
    val words = mutableListOf<String>()
    val word = StringBuilder()
    var isolated = 0
    for (c in text) {
        when (c) {
            LEFT_TO_RIGHT_ISOLATE -> isolated++
            POP_DIRECTIONAL_ISOLATE -> isolated = (isolated - 1).coerceAtLeast(0)
        }
        if (c == ' ' && isolated == 0) {
            if (word.isNotEmpty()) words += word.toString()
            word.clear()
        } else {
            word.append(c)
        }
    }
    if (word.isNotEmpty()) words += word.toString()
    return words
}

private const val LEFT_TO_RIGHT_ISOLATE = '⁦'
private const val POP_DIRECTIONAL_ISOLATE = '⁩'

/** How a button's label moves on to a new one. */
internal enum class LabelMotion {
    /** To the next screen: the whole label turns over to the start side. */
    Forward,

    /** Back to the screen before: the whole label turns over to the end side. */
    Back,

    /** On the same screen: the words both labels share stay and glide into place, the others change in a cascade. */
    InPlace,
}

private fun fraction(
    elapsedMs: Float,
    delayMs: Int,
    durationMs: Int,
): Float = ((elapsedMs - delayMs) / durationMs).coerceIn(0f, 1f)

/** How far the [index]-th old word has left, [elapsedMs] into a change. */
internal fun leaveFraction(
    index: Int,
    elapsedMs: Float,
): Float = fraction(elapsedMs, index * SheetMotion.LABEL_LEAVE_STEP_MS, SheetMotion.LABEL_LEAVE_MS)

/** How far the [index]-th new word has arrived over [durationMs], [elapsedMs] into a change. */
internal fun arriveFraction(
    index: Int,
    elapsedMs: Float,
    durationMs: Int,
): Float = fraction(elapsedMs, SheetMotion.LABEL_ARRIVE_DELAY_MS + index * SheetMotion.LABEL_ARRIVE_STEP_MS, durationMs)

/** From the first old word leaving until the last of the [leaving] has gone and the last of the [arriving] has arrived. */
internal fun cascadeDurationMs(
    leaving: Int,
    arriving: Int,
): Int =
    max(
        (leaving - 1).coerceAtLeast(0) * SheetMotion.LABEL_LEAVE_STEP_MS + SheetMotion.LABEL_LEAVE_MS,
        SheetMotion.LABEL_ARRIVE_DELAY_MS + (arriving - 1).coerceAtLeast(0) * SheetMotion.LABEL_ARRIVE_STEP_MS +
            SheetMotion.LABEL_ARRIVE_MOVE_MS,
    )

/**
 * For each part of [to], the index of the part of [from] it carries on, or -1: the longest run of parts both labels
 * share, in order.
 */
internal fun matchParts(
    from: List<LabelPart>,
    to: List<LabelPart>,
): List<Int> {
    val common = Array(from.size + 1) { IntArray(to.size + 1) }
    for (i in from.indices.reversed()) {
        for (j in to.indices.reversed()) {
            common[i][j] = if (from[i] == to[j]) common[i + 1][j + 1] + 1 else max(common[i + 1][j], common[i][j + 1])
        }
    }
    val match = MutableList(to.size) { -1 }
    var i = 0
    var j = 0
    while (i < from.size && j < to.size) {
        when {
            from[i] == to[j] -> match[j++] = i++
            common[i + 1][j] >= common[i][j + 1] -> i++
            else -> j++
        }
    }
    return match
}

/** How long the label takes to move from [from] to [to] by [motion]. */
internal fun labelChangeDurationMs(
    from: List<LabelPart>,
    to: List<LabelPart>,
    motion: LabelMotion,
): Int {
    if (motion != LabelMotion.InPlace) {
        return max(SheetMotion.LABEL_SLIDE_MS, SheetMotion.LABEL_SLIDE_IN_DELAY_MS + SheetMotion.LABEL_SLIDE_IN_FADE_MS)
    }
    val kept = matchParts(from, to).count { it >= 0 }
    return max(cascadeDurationMs(from.size - kept, to.size - kept), if (kept > 0) SheetMotion.LABEL_KEEP_MOVE_MS else 0)
}

private class LabelChange(
    val from: List<LabelPart>,
    val to: List<LabelPart>,
    val motion: LabelMotion,
) {
    /** Turning over with the screen, nothing carries over: the whole label goes and comes. */
    val match = if (motion == LabelMotion.InPlace) matchParts(from, to) else List(to.size) { -1 }
    val durationMs = labelChangeDurationMs(from, to, motion)
}

/**
 * A button's label that moves on when it changes ([LabelMotion]): with a change of screen it turns over sideways, the
 * way the flow went; on the same screen only the words that differ change, one after another, and the rest glide into
 * their new place. At rest, and with reduced motion, [rest] draws the label as it is.
 */
@Composable
internal fun CascadeLabel(
    parts: List<LabelPart>,
    style: TextStyle,
    color: Color,
    motion: LabelMotion,
    rest: @Composable () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    var shown by remember { mutableStateOf(parts) }
    var change by remember { mutableStateOf<LabelChange?>(null) }
    if (parts != shown) {
        change = if (reduceMotion) null else LabelChange(from = shown, to = parts, motion = motion)
        shown = parts
    }
    val running = change ?: return rest()
    val elapsed = remember(running) { Animatable(0f) }
    LaunchedEffect(running) {
        elapsed.animateTo(running.durationMs.toFloat(), tween(running.durationMs, easing = LinearEasing))
        if (change === running) change = null
    }
    MovingLabel(running, style, color) { elapsed.value }
}

/**
 * Both labels of [change] laid out as the button lays its label out — the icon [ICON_GAP] before the words, a space
 * between words, centred — each part placed where [elapsed] has taken it. A label wider than the button overflows it
 * on both sides while it changes, rather than squeezing its last words.
 */
@Composable
private fun MovingLabel(
    change: LabelChange,
    style: TextStyle,
    color: Color,
    elapsed: () -> Float,
) {
    val measurer = rememberTextMeasurer()
    val space = remember(style) { measurer.measure("a a", style).size.width - measurer.measure("aa", style).size.width }
    val density = LocalDensity.current
    val rise = with(density) { SheetMotion.LABEL_SHIFT.toPx() }
    val slide = with(density) { SheetMotion.LABEL_SLIDE.toPx() }
    val iconGap = with(density) { ICON_GAP.roundToPx() }
    val arriving = change.to.indices.filter { change.match[it] < 0 }
    val kept = change.match.filter { it >= 0 }.toSet()
    Layout(
        content = {
            change.from.forEach { LabelPartView(it, style, color) }
            arriving.forEach { LabelPartView(change.to[it], style, color) }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val old = placeables.subList(0, change.from.size)
        val fresh = placeables.subList(change.from.size, placeables.size).iterator()
        val new = change.to.indices.map { j -> change.match[j].takeIf { it >= 0 }?.let(old::get) ?: fresh.next() }

        fun lineUp(
            parts: List<LabelPart>,
            pieces: List<Placeable>,
        ): Pair<IntArray, Int> {
            var x = 0
            val xs =
                IntArray(parts.size) { i ->
                    if (i > 0) x += if (parts[i] is LabelPart.Word && parts[i - 1] is LabelPart.Word) space else iconGap
                    x.also { x += pieces[i].width }
                }
            return xs to x
        }
        val (oldXs, oldWidth) = lineUp(change.from, old)
        val (newXs, newWidth) = lineUp(change.to, new)
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else max(oldWidth, newWidth)
        val height = (placeables.maxOfOrNull { it.height } ?: 0).coerceIn(constraints.minHeight, constraints.maxHeight)
        val oldStart = (width - oldWidth) / 2
        val newStart = (width - newWidth) / 2
        // Forward the label leaves to the start side, as a screen does: to the left, or to the right reading right to left.
        val toStart = if (layoutDirection == LayoutDirection.Ltr) -1f else 1f

        layout(width, height) {
            val t = elapsed()

            fun Placeable.put(
                x: Int,
                shiftX: Float,
                shiftY: Float,
                opacity: Float,
            ) = placeRelativeWithLayer(x, (height - this.height) / 2) {
                translationX = shiftX
                translationY = shiftY
                alpha = opacity
            }

            if (change.motion != LabelMotion.InPlace) {
                val way = if (change.motion == LabelMotion.Forward) toStart else -toStart
                val moved = SheetMotion.LabelEasing.transform(fraction(t, 0, SheetMotion.LABEL_SLIDE_MS))
                val faded = FastOutSlowInEasing.transform(fraction(t, 0, SheetMotion.LABEL_SLIDE_OUT_FADE_MS))
                val shown =
                    FastOutSlowInEasing.transform(fraction(t, SheetMotion.LABEL_SLIDE_IN_DELAY_MS, SheetMotion.LABEL_SLIDE_IN_FADE_MS))
                old.forEachIndexed { i, p -> p.put(oldStart + oldXs[i], way * slide * moved, 0f, 1f - faded) }
                new.forEachIndexed { j, p -> p.put(newStart + newXs[j], -way * slide * (1f - moved), 0f, shown) }
                return@layout
            }
            val glide = SheetMotion.LabelEasing.transform(fraction(t, 0, SheetMotion.LABEL_KEEP_MOVE_MS))
            var leaving = 0
            old.forEachIndexed { i, p ->
                if (i in kept) {
                    val to = newStart + newXs[change.match.indexOf(i)]
                    val from = oldStart + oldXs[i]
                    p.put(from + ((to - from) * glide).roundToInt(), 0f, 0f, 1f)
                } else {
                    val left = FastOutSlowInEasing.transform(leaveFraction(leaving++, t))
                    p.put(oldStart + oldXs[i], 0f, -rise * left, 1f - left)
                }
            }
            arriving.forEachIndexed { k, j ->
                val faded = FastOutSlowInEasing.transform(arriveFraction(k, t, SheetMotion.LABEL_ARRIVE_FADE_MS))
                val moved = SheetMotion.LabelEasing.transform(arriveFraction(k, t, SheetMotion.LABEL_ARRIVE_MOVE_MS))
                new[j].put(newStart + newXs[j], 0f, rise * (1f - moved), faded)
            }
        }
    }
}

@Composable
private fun LabelPartView(
    part: LabelPart,
    style: TextStyle,
    color: Color,
) = when (part) {
    is LabelPart.Icon -> Icon(part.vector, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
    LabelPart.Spinner -> CircularProgressIndicator(color = color, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
    is LabelPart.Word -> Text(part.text, style = style, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
}

/** Between a button's icon and its text, as [CheckoutButton] lays them out. */
internal val ICON_GAP = 10.dp
