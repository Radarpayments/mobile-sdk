package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.theme.tabular
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr

/**
 * One payment method: leading plate, title (+ subtitle), trailing control. Selected rows get an
 * accent outline and a soft accent fill.
 *
 * @param numericTitle render the title with tabular figures (card masks)
 * @param role semantics role of the click — [Role.RadioButton] for selectable rows
 * @param framed false inside a frame of its own (a field group): no border or fill
 */
@Composable
internal fun MethodRow(
    title: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    numericTitle: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    role: Role = Role.Button,
    framed: Boolean = true,
    trailing: @Composable () -> Unit = {},
) {
    val colors = LocalPaymentColors.current
    val shapes = LocalPaymentShapes.current
    val typography = LocalPaymentTypography.current
    val border by animateColorAsState(if (selected) colors.primary else colors.outline, label = "method-border")
    val fill by animateColorAsState(if (selected) colors.primarySoft else colors.background, label = "method-fill")

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .then(if (framed) Modifier.clip(shapes.card).background(fill).border(1.5.dp, border, shapes.card) else Modifier)
                .then(
                    when {
                        onClick == null || LocalPlaceholder.current -> Modifier
                        // A radio row reads as selected or not, as its ring shows it.
                        role == Role.RadioButton -> Modifier.selectable(selected = selected, role = role, onClick = onClick)
                        else -> Modifier.clickable(role = role, onClick = onClick)
                    },
                ).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading()
        Column(modifier = Modifier.weight(1f)) {
            val titleStyle = typography.body.copy(color = textColor(colors.onBackground), fontWeight = typography.title.fontWeight)
            Text(
                text = if (numericTitle) ltr(title) else title,
                style = if (numericTitle) titleStyle.tabular() else titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.placeholderText(),
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = typography.caption.copy(color = textColor(colors.onSurfaceVariant)),
                    maxLines = 1,
                    modifier = Modifier.placeholderText(),
                )
            }
        }
        trailing()
    }
}

/** 22dp radio ring; filled accent dot when [selected]. */
@Composable
internal fun RadioMark(selected: Boolean) {
    if (LocalPlaceholder.current) return PlaceholderBlock(Modifier.size(22.dp), CircleShape)
    val colors = LocalPaymentColors.current
    val ring by animateColorAsState(if (selected) colors.primary else colors.outlineStrong, label = "radio")
    Box(
        modifier = Modifier.size(22.dp).border(2.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(10.dp).background(colors.primary, CircleShape))
    }
}

/** Tertiary chevron for rows that open another step. */
@Composable
internal fun ChevronMark() {
    if (LocalPlaceholder.current) {
        return Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { PlaceholderBlock(Modifier.size(8.dp, 14.dp)) }
    }
    Icon(PaymentIcons.Chevron, contentDescription = null, tint = LocalPaymentColors.current.placeholder, modifier = Modifier.size(20.dp))
}

/** Dashed 40×26 plate with a plus — leading mark of the "New card" row. */
@Composable
internal fun AddCardPlate() {
    if (LocalPlaceholder.current) return PlaceholderBlock(Modifier.size(width = 40.dp, height = 26.dp))
    val colors = LocalPaymentColors.current
    Box(
        modifier =
            Modifier
                .size(width = 40.dp, height = 26.dp)
                .drawBehind {
                    drawRoundRect(
                        color = colors.outlineStrong,
                        cornerRadius = CornerRadius(6.dp.toPx()),
                        style =
                            Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                            ),
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(PaymentIcons.Plus, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}
