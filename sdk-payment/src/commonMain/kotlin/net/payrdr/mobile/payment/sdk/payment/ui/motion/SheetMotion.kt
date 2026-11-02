package net.payrdr.mobile.payment.sdk.payment.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Every duration, curve and distance the checkout sheet moves with. Step change and sheet height follow the Family
 * drawer (benji.org/family-values, as rebuilt by Bartosz Bak and cult-ui's FamilyDrawer); the sheet itself follows the
 * iOS sheet curve Vaul uses.
 */
internal object SheetMotion {
    val ContentEasing = CubicBezierEasing(0.26f, 0.08f, 0.25f, 1f)
    val HeightEasing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
    val SheetEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    val MarkEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

    const val STEP_MS = 270
    const val STEP_EXIT_FADE_MS = 190
    const val HEIGHT_MS = 270
    const val SHEET_ENTER_MS = 500
    const val SHEET_CLOSE_MS = 320
    const val SHEET_EXPAND_MS = 500

    /** Scale a step comes in from going forward, and leaves to going back. */
    const val ZOOM_IN = 0.96f

    /** Scale a step leaves to going forward, and comes in from going back. */
    const val ZOOM_OUT = 1.04f
    val BLUR = 2.dp

    const val SKELETON_DELAY_MS = 300
    const val SKELETON_MIN_MS = 400
    const val SKELETON_FADE_MS = 150
    const val PULSE_MS = 1400
    const val PULSE_MIN_ALPHA = 0.45f

    const val STATUS_OUT_MS = 120
    const val STATUS_IN_MS = 160

    const val BADGE_DAMPING = 0.55f
    const val BADGE_STIFFNESS = 520f
    const val BADGE_DELAY_MS = 90
    const val MARK_MS = 320
    const val MARK_DELAY_MS = 220
    const val SHAKE_MS = 420
    const val SHAKE_DELAY_MS = 480

    /** Horizontal offsets of the declined badge's shake, in dp. */
    val SHAKE_OFFSETS = listOf(0, -6, 5, -3, 2, 0)
    const val CASCADE_MS = 260
    const val CASCADE_START_MS = 320
    const val CASCADE_STEP_MS = 45
    val CASCADE_RISE = 4.dp

    /** A button's label changing word by word: the old words leave one after another, then the new ones arrive. */
    val LabelEasing = CubicBezierEasing(0.22f, 0.9f, 0.24f, 1f)
    const val LABEL_LEAVE_MS = 220
    const val LABEL_LEAVE_STEP_MS = 45
    const val LABEL_ARRIVE_DELAY_MS = 140
    const val LABEL_ARRIVE_STEP_MS = 70
    const val LABEL_ARRIVE_FADE_MS = 260
    const val LABEL_ARRIVE_MOVE_MS = 380
    val LABEL_SHIFT = 12.dp

    /** Words both labels share glide to their new place in this time. */
    const val LABEL_KEEP_MOVE_MS = 460

    /** A label turning over with a change of screen: the old one slides and fades out, the new one slides in. */
    const val LABEL_SLIDE_MS = 380
    const val LABEL_SLIDE_OUT_FADE_MS = 180
    const val LABEL_SLIDE_IN_DELAY_MS = 60
    const val LABEL_SLIDE_IN_FADE_MS = 260
    val LABEL_SLIDE = 32.dp

    const val REDUCED_ENTER_MS = 150
    const val REDUCED_EXIT_MS = 100
    const val REDUCED_SHEET_ENTER_MS = 200
    const val REDUCED_SHEET_CLOSE_MS = 150
}

private fun <T> stepTween(durationMs: Int = SheetMotion.STEP_MS): FiniteAnimationSpec<T> =
    tween(durationMs, easing = SheetMotion.ContentEasing)

private fun heightTransform(reduceMotion: Boolean): SizeTransform =
    SizeTransform(clip = true) { _, _ ->
        if (reduceMotion) snap() else tween<IntSize>(SheetMotion.HEIGHT_MS, easing = SheetMotion.HeightEasing)
    }

/**
 * The one step change: the whole content of the sheet zooms. Forward, the leaving step grows to [SheetMotion.ZOOM_OUT]
 * as it fades and the new one grows in from [SheetMotion.ZOOM_IN]; back, the other way round. The sheet's height
 * follows in the same time. With reduced motion, a short cross-fade and an instant height.
 */
internal fun stepTransform(
    forward: Boolean,
    reduceMotion: Boolean,
): ContentTransform {
    if (reduceMotion) {
        return ContentTransform(
            targetContentEnter = fadeIn(tween(SheetMotion.REDUCED_ENTER_MS)),
            initialContentExit = fadeOut(tween(SheetMotion.REDUCED_EXIT_MS)),
            sizeTransform = heightTransform(reduceMotion = true),
        )
    }
    val enterScale = if (forward) SheetMotion.ZOOM_IN else SheetMotion.ZOOM_OUT
    val exitScale = if (forward) SheetMotion.ZOOM_OUT else SheetMotion.ZOOM_IN
    return ContentTransform(
        targetContentEnter = fadeIn(stepTween()) + scaleIn(stepTween(), initialScale = enterScale),
        initialContentExit = fadeOut(stepTween(SheetMotion.STEP_EXIT_FADE_MS)) + scaleOut(stepTween(), targetScale = exitScale),
        sizeTransform = heightTransform(reduceMotion = false),
    )
}

/**
 * Loading → content: the header is the same on both, so the layers themselves do not move — the loading layer stays
 * under the new one until the change is over, and only the new step's body comes in, through [revealBody].
 */
internal fun loadingRevealTransform(reduceMotion: Boolean): ContentTransform =
    // A fade to full opacity: the loading layer stays as it is until the body has come in, then goes.
    ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = fadeOut(stepTween(), targetAlpha = 1f),
        sizeTransform = heightTransform(reduceMotion),
    )

/**
 * The body of a step coming in over the loading layer: fades in, and when it arrived before the skeleton showed
 * ([zoom]) also grows in from [SheetMotion.ZOOM_IN] out of a light blur, as any other step does.
 */
internal fun Modifier.revealBody(
    scope: AnimatedVisibilityScope,
    zoom: Boolean,
    reduceMotion: Boolean,
): Modifier =
    composed {
        val transition = scope.transition
        val duration = if (reduceMotion) SheetMotion.REDUCED_ENTER_MS else SheetMotion.STEP_MS
        val shown =
            transition.animateFloat({ stepTween(duration) }, label = "body-reveal") {
                if (it == EnterExitState.Visible) 1f else 0f
            }
        val progress = shown.value
        val zooms = zoom && !reduceMotion
        graphicsLayer {
            alpha = progress
            if (zooms) {
                val scale = SheetMotion.ZOOM_IN + (1f - SheetMotion.ZOOM_IN) * progress
                scaleX = scale
                scaleY = scale
            }
        }.then(if (zooms) Modifier.blur(SheetMotion.BLUR * (1f - progress)) else Modifier)
    }

/** The body under a step's header: what [revealBody] applies to. */
internal val LocalStepBodyModifier = compositionLocalOf<Modifier> { Modifier }

/**
 * How far the step of [scope] has faded in or out, on the same curves as [stepTransform] (or [revealBody] when it
 * [reveals] under the loading header): for what draws the step's parts outside it.
 */
@Composable
internal fun stepAlpha(
    scope: AnimatedVisibilityScope,
    reveals: Boolean,
    reduceMotion: Boolean,
): State<Float> =
    scope.transition.animateFloat(
        transitionSpec = {
            when {
                reveals -> stepTween(if (reduceMotion) SheetMotion.REDUCED_ENTER_MS else SheetMotion.STEP_MS)
                targetState == EnterExitState.Visible -> if (reduceMotion) tween(SheetMotion.REDUCED_ENTER_MS) else stepTween()
                else -> if (reduceMotion) tween(SheetMotion.REDUCED_EXIT_MS) else stepTween(SheetMotion.STEP_EXIT_FADE_MS)
            }
        },
        label = "step-alpha",
    ) { if (it == EnterExitState.Visible) 1f else 0f }

/** The light blur a step passes through while it zooms in or out; none with reduced motion. */
internal fun Modifier.stepBlur(
    scope: AnimatedVisibilityScope,
    reduceMotion: Boolean,
): Modifier =
    if (reduceMotion) {
        this
    } else {
        composed {
            val radius =
                scope.transition.animateDp({ stepTween() }, label = "step-blur") {
                    if (it == EnterExitState.Visible) 0.dp else SheetMotion.BLUR
                }
            blur(radius.value)
        }
    }

/** When the [index]-th line of the receipt starts to come in, after the mark. */
internal fun cascadeDelayMs(index: Int): Int = SheetMotion.CASCADE_START_MS + index * SheetMotion.CASCADE_STEP_MS

/**
 * The [index]-th line of the receipt comes in after the mark: fades in rising [SheetMotion.CASCADE_RISE]. Off
 * ([enabled] false) it simply stands.
 */
internal fun Modifier.cascadeIn(
    index: Int,
    enabled: Boolean,
): Modifier =
    if (!enabled) {
        this
    } else {
        composed {
            val shown = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                shown.animateTo(1f, tween(SheetMotion.CASCADE_MS, delayMillis = cascadeDelayMs(index), easing = SheetMotion.MarkEasing))
            }
            graphicsLayer {
                alpha = shown.value
                translationY = SheetMotion.CASCADE_RISE.toPx() * (1f - shown.value)
            }
        }
    }
