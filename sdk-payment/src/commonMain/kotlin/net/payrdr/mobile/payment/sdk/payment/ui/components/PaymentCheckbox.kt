package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import kotlin.math.roundToInt

/** 22dp checkbox on the left, centred on the first line of the title + description next to it; the whole row toggles. */
@Composable
internal fun CheckboxRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                // The box filling in is the feedback; no ripple over the row.
                .toggleable(
                    value = checked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange,
                ).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val titleStyle = typography.body.copy(color = colors.onBackground, fontWeight = typography.label.fontWeight)
        // The box's centre goes to the middle of the title's capitals, measured from its first baseline: centring on
        // the line box leaves it low, since the line keeps room for descenders.
        val capHalf =
            with(LocalDensity.current) {
                titleStyle.fontSize
                    .takeIf { it.isSp }
                    ?.toPx()
                    ?.times(CAP_HALF)
            } ?: 0f
        CheckboxBox(checked = checked, modifier = Modifier.alignBy { it.measuredHeight / 2 + capHalf.roundToInt() })
        Column(
            modifier = Modifier.weight(1f).alignBy(FirstBaseline),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = titleStyle)
            if (description != null) {
                Text(description, style = typography.caption.copy(color = colors.onSurfaceVariant))
            }
        }
    }
}

@Composable
private fun CheckboxBox(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPaymentColors.current
    val shape = RoundedCornerShape(6.dp)
    val fill by animateColorAsState(if (checked) colors.primary else Color.Transparent, label = "checkbox-fill")
    val border by animateColorAsState(if (checked) colors.primary else colors.outlineStrong, label = "checkbox-border")
    Box(
        modifier = modifier.size(BOX_SIZE).background(fill, shape).border(1.5.dp, border, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(PaymentIcons.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
    }
}

private val BOX_SIZE = 22.dp

/** Half the cap height of the usual UI fonts (Roboto, SF), in ems. */
private const val CAP_HALF = 0.355f
