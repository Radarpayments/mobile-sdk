package net.payrdr.mobile.payment.sdk.payment.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.components.ButtonKind
import net.payrdr.mobile.payment.sdk.payment.ui.components.CardNetworkIcon
import net.payrdr.mobile.payment.sdk.payment.ui.components.CheckoutButton
import net.payrdr.mobile.payment.sdk.payment.ui.components.ChevronMark
import net.payrdr.mobile.payment.sdk.payment.ui.components.IconAction
import net.payrdr.mobile.payment.sdk.payment.ui.components.MethodRow
import net.payrdr.mobile.payment.sdk.payment.ui.components.PaymentIcons
import net.payrdr.mobile.payment.sdk.payment.ui.components.SIDE_PADDING
import net.payrdr.mobile.payment.sdk.payment.ui.components.SheetOverlay
import net.payrdr.mobile.payment.sdk.payment.ui.components.StepHeader
import net.payrdr.mobile.payment.sdk.payment.ui.components.TextAction
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardFormatters
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetworkResolver
import net.payrdr.mobile.payment.sdk.payment.ui.util.displayName
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.action_done
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.action_edit
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_list_delete
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_list_edit_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_list_empty
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_list_keep
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_list_remove_body
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_list_remove_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_list_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.sheet_saved_cards
import org.jetbrains.compose.resources.stringResource

/**
 * All saved cards. Rows look the same as in the payment sheet; tapping one continues with that
 * card. "Edit" switches to a mode with a trash action on deletable cards (gateway
 * `bindingDeactivationEnabled`); removal is confirmed in a sheet that names the card.
 *
 * @param onCardClick continue the payment with this card
 * @param onAddNewCard open the new-card step
 * @param onUnbind remove the binding on the gateway
 * @param onBack when non-null, the top bar gets a back action
 * @param onClose when non-null, the top bar gets a close action that leaves the whole checkout
 */
@Composable
public fun CardListScreen(
    savedCards: List<SavedCardInfo>,
    onCardClick: (SavedCardInfo) -> Unit,
    onAddNewCard: () -> Unit,
    onUnbind: (PaymentIntent.UnbindSavedCard) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    val canEdit = savedCards.any { it.canDelete }
    var editing by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<SavedCardInfo?>(null) }
    // Keep the card being removed while the sheet animates out.
    var lastRemoving by remember { mutableStateOf<SavedCardInfo?>(null) }
    SideEffect { removing?.let { lastRemoving = it } }

    PageScaffold(
        modifier = modifier.testTag("CardListScreen"),
        topBar = {
            StepHeader(
                title = stringResource(if (editing) Res.string.card_list_edit_title else Res.string.card_list_title),
                onBack = onBack,
                onClose = onClose,
                modifier = Modifier.padding(start = SIDE_PADDING, end = SIDE_PADDING, top = 2.dp, bottom = 8.dp),
            )
        },
        overlay = {
            SheetOverlay(visible = removing != null, onDismiss = { removing = null }) {
                val card = removing ?: lastRemoving
                if (card != null) {
                    val name = cardName(card)
                    Text(
                        stringResource(Res.string.card_list_remove_title, name),
                        style = typography.headline.copy(color = colors.onBackground),
                    )
                    Text(
                        stringResource(Res.string.card_list_remove_body),
                        style = typography.body.copy(color = colors.onSurfaceVariant),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    CheckoutButton(
                        text = stringResource(Res.string.card_list_delete),
                        kind = ButtonKind.Danger,
                        onClick = {
                            onUnbind(PaymentIntent.UnbindSavedCard(card.bindingId))
                            removing = null
                        },
                    )
                    CheckoutButton(
                        text = stringResource(Res.string.card_list_keep),
                        kind = ButtonKind.Secondary,
                        onClick = { removing = null },
                    )
                }
            }
        },
    ) {
        // "Edit" sits with the list it changes, not in the header: the header stays back · title · close everywhere.
        Row(modifier = Modifier.fillMaxWidth().height(20.dp).offset(y = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.sheet_saved_cards),
                style = typography.label.copy(color = colors.onSurfaceVariant, fontWeight = typography.title.fontWeight),
                modifier = Modifier.weight(1f),
            )
            if (canEdit) {
                TextAction(
                    text = stringResource(if (editing) Res.string.action_done else Res.string.action_edit),
                    onClick = { editing = !editing },
                    modifier = Modifier.offset(x = 8.dp).wrapContentHeight(unbounded = true),
                )
            }
        }
        if (savedCards.isEmpty()) {
            Text(stringResource(Res.string.card_list_empty), style = typography.body.copy(color = colors.onSurfaceVariant))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            savedCards.forEach { card ->
                val network = CardNetworkResolver.resolveSaved(card.paymentSystem, card.maskedPan)
                MethodRow(
                    title = CardFormatters.shortMask(card.maskedPan),
                    subtitle = card.rowSubtitle(),
                    numericTitle = true,
                    onClick = if (editing) null else ({ onCardClick(card) }),
                    leading = { CardNetworkIcon(network) },
                    trailing = {
                        when {
                            // Same height as the delete button, so rows do not jump when editing starts.
                            !editing -> Box(Modifier.height(44.dp), contentAlignment = Alignment.Center) { ChevronMark() }
                            card.canDelete ->
                                IconAction(
                                    icon = PaymentIcons.Trash,
                                    contentDescription = stringResource(Res.string.card_list_delete) + " " + cardName(card),
                                    onClick = { removing = card },
                                    tint = colors.error,
                                    modifier = Modifier.offset(x = 10.dp).testTag("delete-${card.bindingId}"),
                                )
                        }
                    },
                )
            }
            if (!editing) NewCardRow(onClick = onAddNewCard)
        }
    }
}

private fun cardName(card: SavedCardInfo): String {
    val network = CardNetworkResolver.resolveSaved(card.paymentSystem, card.maskedPan)
    return listOf(network.displayName, ltr(CardFormatters.shortMask(card.maskedPan))).filter { it.isNotBlank() }.joinToString(" ")
}
