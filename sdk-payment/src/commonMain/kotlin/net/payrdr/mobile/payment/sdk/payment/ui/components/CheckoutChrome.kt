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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import net.payrdr.mobile.payment.sdk.payment.ui.theme.primarySoft
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.action_back
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.action_close
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.sheet_order_number
import org.jetbrains.compose.resources.stringResource

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
    AnnounceScreenChange()
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

/** Amount and order number at the top of the checkout steps; off for now, the Pay button carries the amount. */
private const val SHOW_ORDER_HEADER = false

/**
 * Top of the payment sheet and of the steps that take a payment: big amount with the order number
 * under it (read as the screen heading). A close cross sits on the right, in line with the top of the
 * amount; a back arrow gets its own row above the amount, as a large title does. With [SHOW_ORDER_HEADER] off
 * only «‹ Back» and the cross are left, on one row.
 *
 * @param amount formatted amount; null leaves only the navigation action
 * @param onClose shows the close cross
 * @param onBack shows the back arrow (used instead of [onClose] on pushed steps)
 */
@Composable
internal fun AmountTopBar(
    amount: String?,
    orderNumber: String?,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    val colors = LocalPaymentColors.current
    if (!SHOW_ORDER_HEADER) {
        // Only the navigation: back on the left, close on the right, nothing when neither is set.
        if (onBack == null && onClose == null) return
        Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                // Labelled: with no heading on the step, a bare arrow does not say where it leads.
                TextAction(
                    text = stringResource(Res.string.action_back),
                    icon = PaymentIcons.Back,
                    onClick = onBack,
                    // The arrow lines up with the content's left edge; the action carries its own padding.
                    modifier = Modifier.offset(x = (-10).dp),
                )
            }
            Spacer(Modifier.weight(1f))
            if (onClose != null) {
                IconAction(
                    icon = PaymentIcons.Close,
                    contentDescription = stringResource(Res.string.action_close),
                    onClick = onClose,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.offset(x = 8.dp),
                )
            }
        }
        return
    }
    Column(modifier = modifier.fillMaxWidth()) {
        if (onBack != null) {
            // Lined up with the content's left edge: the button carries its own padding around the glyph.
            IconAction(
                icon = PaymentIcons.Back,
                contentDescription = stringResource(Res.string.action_back),
                onClick = onBack,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.offset(x = (-10).dp),
            )
        }
        Row(verticalAlignment = Alignment.Top) {
            AmountTitle(amount, orderNumber, Modifier.weight(1f))
            if (onClose != null) {
                IconAction(
                    icon = PaymentIcons.Close,
                    contentDescription = stringResource(Res.string.action_close),
                    onClick = onClose,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.offset(x = 8.dp, y = ICON_TO_AMOUNT_TOP),
                )
            }
        }
    }
}

@Composable
private fun AmountTitle(
    amount: String?,
    orderNumber: String?,
    modifier: Modifier,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        if (amount != null) {
            Text(amount, style = typography.amount.copy(color = colors.onBackground), modifier = Modifier.semantics { heading() })
        }
        orderNumber?.let {
            Text(stringResource(Res.string.sheet_order_number, it), style = typography.caption.copy(color = colors.onSurfaceVariant))
        }
    }
}

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

/** Lifts a 44dp icon button so the glyph's top meets the top of the amount digits. */
private val ICON_TO_AMOUNT_TOP = (-12).dp
