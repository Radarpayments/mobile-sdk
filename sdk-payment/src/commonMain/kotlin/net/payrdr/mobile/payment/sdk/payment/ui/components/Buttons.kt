package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.theme.primarySoft
import net.payrdr.mobile.payment.sdk.payment.ui.theme.secondaryFill
import net.payrdr.mobile.payment.sdk.payment.ui.theme.tabular

/** Visual weight of a [CheckoutButton]. */
internal enum class ButtonKind { Primary, Secondary, Danger }

/**
 * 54dp full-width button of the checkout. [Primary] uses the merchant accent, [Secondary] a neutral
 * tint of the text colour, [Danger] the error colour. Its label changes word by word ([CascadeLabel]). The step's
 * [primary] button, in the sheet, is drawn by its [PrimaryButtonLayer]: it stays in place from step to step.
 */
@Composable
internal fun CheckoutButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    primary: Boolean = false,
) {
    if (LocalPlaceholder.current) return PlaceholderBlock(modifier.fillMaxWidth().height(54.dp), LocalPaymentShapes.current.button)
    val face = PrimaryButtonFace(text, icon, kind, enabled, loading)
    val layer = LocalPrimaryButtonLayer.current?.takeIf { primary }
    if (layer == null) return ButtonSurface(face, onClick, LabelMotion.InPlace, modifier = modifier.fillMaxWidth())

    val alpha = LocalStepAlpha.current
    val settled = LocalStepSettled.current
    val slot = remember(layer) { PrimaryButtonSlot(face, alpha, settled) }
    slot.onClick = onClick
    SideEffect { slot.face = face }
    DisposableEffect(layer, slot) {
        layer.slots += slot
        onDispose { layer.slots -= slot }
    }
    Spacer(
        modifier.fillMaxWidth().height(54.dp).onPlaced {
            slot.coordinates = it
            slot.placements++
        },
    )
}

/** What a [CheckoutButton] draws, in its step or in the sheet's [PrimaryButtonLayer]; [motion] as for [CascadeLabel]. */
@Composable
internal fun ButtonSurface(
    face: PrimaryButtonFace,
    onClick: () -> Unit,
    motion: LabelMotion,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPaymentColors.current
    val shapes = LocalPaymentShapes.current
    val typography = LocalPaymentTypography.current
    val (container: Color, content: Color) =
        when (face.kind) {
            ButtonKind.Primary -> colors.primary to colors.onPrimary
            ButtonKind.Secondary -> colors.secondaryFill to colors.onBackground
            ButtonKind.Danger -> colors.error to colors.onError
        }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = face.enabled && !face.loading
    val parts = remember(face.text, face.icon, face.loading) { labelParts(face.text, face.icon, face.loading) }
    val textStyle = typography.button.tabular().copy(color = content)

    Box(
        modifier =
            modifier
                .height(54.dp)
                .scale(if (pressed) 0.985f else 1f)
                .alpha(if (face.enabled) 1f else 0.45f)
                .clip(shapes.button)
                .background(container)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = active,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CascadeLabel(parts, textStyle, content, motion) {
            if (face.loading) {
                CircularProgressIndicator(color = content, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ICON_GAP),
                ) {
                    if (face.icon != null) {
                        Icon(face.icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = face.text,
                        style = textStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Accent-coloured text action — "All cards", "Edit", "Done", "Scan". */
@Composable
internal fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = LocalPaymentColors.current.primary,
) {
    val typography = LocalPaymentTypography.current
    val placeholder = LocalPlaceholder.current
    Row(
        modifier =
            modifier
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .then(if (placeholder) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick))
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = textColor(color), modifier = Modifier.size(18.dp).placeholderText())
        Text(
            text = text,
            style = typography.label.copy(color = textColor(color), fontWeight = FontWeight.SemiBold),
            modifier = Modifier.placeholderText(),
        )
    }
}

/** 44dp square icon button with a soft pressed background. */
@Composable
internal fun IconAction(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalPaymentColors.current.onBackground,
) {
    val colors = LocalPaymentColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier =
            modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (pressed) colors.primarySoft else Color.Transparent)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = contentDescription,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}
