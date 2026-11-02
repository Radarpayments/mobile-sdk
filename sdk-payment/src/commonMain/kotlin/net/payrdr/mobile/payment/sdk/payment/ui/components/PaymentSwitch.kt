package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography

/** Title + description on the left, 46×28 switch on the right; the whole row toggles. */
@Composable
internal fun SwitchRow(
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
                // The sliding thumb is the feedback; no ripple over the row.
                .toggleable(
                    value = checked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        // A lone title sits in the middle of the switch; with a description it lines up with the top.
        verticalAlignment = if (description == null) Alignment.CenterVertically else Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = typography.body.copy(color = colors.onBackground, fontWeight = typography.label.fontWeight))
            if (description != null) {
                Text(description, style = typography.caption.copy(color = colors.onSurfaceVariant))
            }
        }
        SwitchTrack(checked = checked, modifier = if (description == null) Modifier else Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun SwitchTrack(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPaymentColors.current
    val track by animateColorAsState(if (checked) colors.primary else colors.outlineStrong, label = "switch-track")
    val thumbX by animateDpAsState(if (checked) 18.dp else 0.dp, label = "switch-thumb")
    Box(
        modifier = modifier.size(width = 46.dp, height = 28.dp).background(track, CircleShape).padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = thumbX)
                .size(22.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}
