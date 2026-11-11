package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.payment.ui.motion.SheetMotion
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors

/** The strokes of [PaymentIcons.Check] and [PaymentIcons.Cross], on their 24-unit grid with the same width. */
private val CHECK_STROKES = listOf("M5 12.5l4.5 4.5L19 7.5")
private val CROSS_STROKES = listOf("M6 6l12 12", "M18 6L6 18")
private const val ICON_GRID = 24f
private const val ICON_STROKE = 2.4f

/**
 * The result badge: the circle springs in, then the check (or cross) draws itself, and a declined one shakes once.
 * [onMarkStart] fires as the mark starts to draw — the moment for haptics. With [reduceMotion] everything stands at
 * once and [onMarkStart] fires straight away.
 */
@Composable
internal fun ResultMark(
    success: Boolean,
    reduceMotion: Boolean,
    onMarkStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPaymentColors.current
    val scale = remember { Animatable(if (reduceMotion) 1f else 0.5f) }
    val drawn = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val shake = remember { Animatable(0f) }
    val latestOnMarkStart by rememberUpdatedState(onMarkStart)
    LaunchedEffect(Unit) {
        if (reduceMotion) {
            latestOnMarkStart()
            return@LaunchedEffect
        }
        launch {
            delay(SheetMotion.BADGE_DELAY_MS.toLong())
            scale.animateTo(1f, spring(SheetMotion.BADGE_DAMPING, SheetMotion.BADGE_STIFFNESS))
        }
        launch {
            delay(SheetMotion.MARK_DELAY_MS.toLong())
            latestOnMarkStart()
            drawn.animateTo(1f, tween(SheetMotion.MARK_MS, easing = SheetMotion.MarkEasing))
        }
        if (!success) {
            delay(SheetMotion.SHAKE_DELAY_MS.toLong())
            shake.animateTo(
                0f,
                keyframes {
                    durationMillis = SheetMotion.SHAKE_MS
                    val step = SheetMotion.SHAKE_MS / (SheetMotion.SHAKE_OFFSETS.size - 1)
                    SheetMotion.SHAKE_OFFSETS.forEachIndexed { i, dx -> dx.toFloat() at step * i }
                },
            )
        }
    }
    val tint = if (success) colors.success else colors.error
    val strokes =
        remember(success) {
            (if (success) CHECK_STROKES else CROSS_STROKES).map {
                PathMeasure().apply {
                    setPath(
                        PathParser().parsePathString(it).toPath(),
                        false,
                    )
                }
            }
        }
    Box(
        modifier =
            modifier
                .offset(x = shake.value.dp)
                .scale(scale.value)
                .size(76.dp)
                .background(if (success) colors.successSoft else colors.errorSoft, CircleShape)
                // Decorative: the headline under it says the same in words.
                .clearAndSetSemantics { testTag = if (success) "SuccessIcon" else "FailureIcon" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(38.dp)) {
            // The strokes draw one after another (the cross has two), along their whole length.
            var remaining = strokes.sumOf { it.length.toDouble() }.toFloat() * drawn.value
            val partial = Path()
            strokes.forEach { stroke ->
                val take = remaining.coerceAtMost(stroke.length)
                if (take > 0f) stroke.getSegment(0f, take, partial, true)
                remaining -= take
            }
            val unit = size.minDimension / ICON_GRID
            scale(unit, unit, pivot = Offset.Zero) {
                drawPath(partial, tint, style = Stroke(width = ICON_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
