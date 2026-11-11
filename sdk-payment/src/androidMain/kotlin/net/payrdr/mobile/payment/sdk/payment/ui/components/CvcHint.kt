package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardFormatters
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetwork
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.cvv_hint_back
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.cvv_hint_front
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.cvv_hint_open
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * «i» at the end of a CVV input: a tap shows a dark bubble under it saying where the code is printed
 * ([network] tells the back of the card from the front), a tap anywhere hides it. The popup does not take focus,
 * so the keyboard stays up.
 */
@Composable
internal fun CvcHintButton(network: CardNetwork) {
    val colors = LocalPaymentColors.current
    var shown by remember { mutableStateOf(false) }
    // A tap on the icon while the bubble is up first dismisses it as an outside tap; without this the same tap
    // would then open it again.
    var dismissedAt by remember { mutableStateOf<TimeMark?>(null) }
    val description = stringResource(Res.string.cvv_hint_open)
    val density = LocalDensity.current
    val position = remember(density) { with(density) { BelowAnchorEnd(ARROW_INSET.roundToPx(), ANCHOR_GAP.roundToPx()) } }
    Box(
        modifier =
            Modifier
                .size(HINT_TARGET)
                .semantics { contentDescription = description }
                .toggleable(
                    value = shown,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onValueChange = { show ->
                        val reopening = dismissedAt?.let { it.elapsedNow() < REOPEN_GUARD } == true
                        shown = show && !reopening
                        dismissedAt = null
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            PaymentIcons.Info,
            contentDescription = null,
            tint = if (shown) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        if (shown) {
            Popup(
                popupPositionProvider = position,
                onDismissRequest = {
                    shown = false
                    dismissedAt = TimeSource.Monotonic.markNow()
                },
                properties = PopupProperties(focusable = false),
            ) {
                CvcHintBubble(frontSide = CardFormatters.cvvLength(network) == FRONT_CVV_LENGTH)
            }
        }
    }
}

@Composable
private fun CvcHintBubble(frontSide: Boolean) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    val bubble = colors.onBackground
    val shape = RoundedCornerShape(10.dp)
    Column(horizontalAlignment = Alignment.End) {
        // The arrow sits under the icon: the bubble's end is ARROW_INSET past the icon's centre.
        Box(
            Modifier
                .padding(end = ARROW_INSET - ARROW_WIDTH / 2)
                .size(ARROW_WIDTH, ARROW_HEIGHT)
                .background(bubble, ArrowUp),
        )
        Text(
            text = stringResource(if (frontSide) Res.string.cvv_hint_front else Res.string.cvv_hint_back),
            style = typography.caption.copy(color = colors.background),
            modifier =
                Modifier
                    .widthIn(max = BUBBLE_MAX_WIDTH)
                    .shadow(8.dp, shape)
                    .background(bubble, shape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

/** [gap] under the anchor, end-aligned so the arrow points at the anchor's centre; kept inside the window. */
private class BelowAnchorEnd(
    private val arrowInset: Int,
    private val gap: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val end = anchorBounds.center.x + arrowInset
        val x = (end - popupContentSize.width).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        return IntOffset(x, anchorBounds.bottom + gap)
    }
}

private val ArrowUp =
    GenericShape { size, _ ->
        moveTo(size.width / 2, 0f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }

private val HINT_TARGET = 32.dp
private val BUBBLE_MAX_WIDTH = 240.dp
private val ARROW_WIDTH = 12.dp
private val ARROW_HEIGHT = 6.dp
private val ARROW_INSET = 22.dp

// From the icon's target to just past the cell's frame, so the arrow does not cover it.
private val ANCHOR_GAP = 8.dp
private const val FRONT_CVV_LENGTH = 4
private val REOPEN_GUARD = 300.milliseconds
