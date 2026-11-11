package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.action_back
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.action_close
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.stringResource

/**
 * Header of every checkout step: a round back button on the left, the step's title in the middle and a round
 * close button on the right — same size and colour, so the two read as one pair. A missing action leaves its
 * slot empty and the title stays centred.
 *
 * @param onBack one step back; null on the first step
 * @param onClose leaves the whole checkout
 */
@Composable
internal fun StepHeader(
    title: String,
    onBack: (() -> Unit)?,
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    Box(modifier = modifier.fillMaxWidth().height(STEP_HEADER_HEIGHT), contentAlignment = Alignment.Center) {
        Text(
            text = title,
            style = typography.title.copy(color = colors.onBackground),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // The pane title makes TalkBack announce the step as it comes; VoiceOver is told in AnnounceScreenChange.
            modifier =
                Modifier.padding(horizontal = STEP_HEADER_HEIGHT + 8.dp).semantics {
                    heading()
                    paneTitle = title
                },
        )
        if (onBack != null) {
            RoundIconAction(
                icon = PaymentIcons.Back,
                contentDescription = stringResource(Res.string.action_back),
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).offset(x = -ROUND_ACTION_INSET),
            )
        }
        if (onClose != null) {
            RoundIconAction(
                icon = PaymentIcons.Close,
                contentDescription = stringResource(Res.string.action_close),
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterEnd).offset(x = ROUND_ACTION_INSET),
            )
        }
    }
}

/** 44dp target with a 32dp soft circle; the circle, not the target, lines up with the content's edge. */
@Composable
private fun RoundIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPaymentColors.current
    Box(
        modifier =
            modifier
                .size(STEP_HEADER_HEIGHT)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(ROUND_ACTION_SIZE).background(colors.surface, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = colors.onBackground, modifier = Modifier.size(18.dp))
        }
    }
}

private val STEP_HEADER_HEIGHT = 44.dp
private val ROUND_ACTION_SIZE = 32.dp
private val ROUND_ACTION_INSET = (STEP_HEADER_HEIGHT - ROUND_ACTION_SIZE) / 2

/** Hairline — label — hairline, e.g. "or pay by card". */
@Composable
internal fun LabeledDivider(text: String) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.outline)
        Text(
            text = text,
            style = typography.caption.copy(color = textColor(colors.onSurfaceVariant)),
            modifier = Modifier.placeholderText(),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.outline)
    }
}

/** Drag handle at the top of bottom sheets. */
@Composable
internal fun SheetGrabber(modifier: Modifier = Modifier) {
    val colors = LocalPaymentColors.current
    Box(modifier = modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 38.dp, height = 4.dp)
                .background(colors.outlineStrong, RoundedCornerShape(2.dp)),
        )
    }
}

/** Spinning accent arc on a soft track with [icon] in the middle — processing and 3DS waits. */
@Composable
internal fun ProgressRing(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
) {
    val colors = LocalPaymentColors.current
    val transition = rememberInfiniteTransition(label = "ring")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "ring-angle",
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size).rotate(angle)) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2
            val arcSize =
                androidx.compose.ui.geometry
                    .Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft =
                androidx.compose.ui.geometry
                    .Offset(inset, inset)
            drawArc(colors.primarySoft, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(colors.primary, -90f, 90f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(30.dp))
    }
}
