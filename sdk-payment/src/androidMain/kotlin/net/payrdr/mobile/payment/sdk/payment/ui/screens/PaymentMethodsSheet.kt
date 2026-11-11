package net.payrdr.mobile.payment.sdk.payment.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.presentation.SHEET_CARD_LIMIT
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.components.AddCardPlate
import net.payrdr.mobile.payment.sdk.payment.ui.components.ApplePayButton
import net.payrdr.mobile.payment.sdk.payment.ui.components.CardNetworkIcon
import net.payrdr.mobile.payment.sdk.payment.ui.components.CheckoutButton
import net.payrdr.mobile.payment.sdk.payment.ui.components.ChevronMark
import net.payrdr.mobile.payment.sdk.payment.ui.components.GooglePayButton
import net.payrdr.mobile.payment.sdk.payment.ui.components.LabeledDivider
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalPlaceholder
import net.payrdr.mobile.payment.sdk.payment.ui.components.MethodRow
import net.payrdr.mobile.payment.sdk.payment.ui.components.PayButton
import net.payrdr.mobile.payment.sdk.payment.ui.components.PlaceholderBlock
import net.payrdr.mobile.payment.sdk.payment.ui.components.RadioMark
import net.payrdr.mobile.payment.sdk.payment.ui.components.StepHeader
import net.payrdr.mobile.payment.sdk.payment.ui.components.TextAction
import net.payrdr.mobile.payment.sdk.payment.ui.components.WalletAvailability
import net.payrdr.mobile.payment.sdk.payment.ui.components.payLabel
import net.payrdr.mobile.payment.sdk.payment.ui.components.placeholderText
import net.payrdr.mobile.payment.sdk.payment.ui.components.textColor
import net.payrdr.mobile.payment.sdk.payment.ui.motion.LocalStepBodyModifier
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardFormatters
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetworkResolver
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_list_add_new
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.checkout_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.sheet_all_cards
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.sheet_new_card_hint
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.sheet_or_pay_by_card
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.sheet_pay_by_card
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.sheet_pay_with_card
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.sheet_saved_cards
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.stringResource

/**
 * Content of the first step — the payment sheet. Header with merchant, amount and order number;
 * wallet buttons; saved cards as a radio list (first one preselected) plus a "New card" row; Pay.
 * Without saved cards the primary button becomes "Pay by card".
 */
@Composable
internal fun ColumnScope.PaymentMethodsContent(
    wallets: WalletAvailability,
    order: OrderSummary?,
    savedCards: List<SavedCardInfo>,
    onClose: () -> Unit,
    onGooglePay: () -> Unit,
    onApplePay: () -> Unit,
    onPaySaved: (SavedCardInfo) -> Unit,
    onNewCard: () -> Unit,
    onAllCards: () -> Unit,
    opensCardStep: (SavedCardInfo) -> Boolean,
) {
    StepHeader(title = stringResource(Res.string.checkout_title), onBack = null, onClose = onClose)
    Column(LocalStepBodyModifier.current, verticalArrangement = Arrangement.spacedBy(SHEET_ITEM_SPACING)) {
        PaymentMethodsBody(wallets, order, savedCards, onGooglePay, onApplePay, onPaySaved, onNewCard, onAllCards, opensCardStep)
    }
}

/** Between the items of the payment sheet: the header, the wallets, the cards, the button. */
internal val SHEET_ITEM_SPACING = 14.dp

/** Everything under the header; in a skeleton ([LocalPlaceholder]) the same layout drawn as blocks. */
@Composable
internal fun ColumnScope.PaymentMethodsBody(
    wallets: WalletAvailability,
    order: OrderSummary?,
    savedCards: List<SavedCardInfo>,
    onGooglePay: () -> Unit,
    onApplePay: () -> Unit,
    onPaySaved: (SavedCardInfo) -> Unit,
    onNewCard: () -> Unit,
    onAllCards: () -> Unit,
    opensCardStep: (SavedCardInfo) -> Boolean = { false },
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    val placeholder = LocalPlaceholder.current
    var selectedId by remember(savedCards) { mutableStateOf(savedCards.firstOrNull()?.bindingId) }
    val selected = savedCards.firstOrNull { it.bindingId == selectedId }

    val walletModifier = Modifier.fillMaxWidth().height(52.dp)
    when {
        // A wallet button is a native view: the skeleton draws only its block.
        placeholder -> if (wallets.any) PlaceholderBlock(walletModifier, LocalPaymentShapes.current.button)
        else -> {
            if (wallets.googlePay) GooglePayButton(onClick = onGooglePay, modifier = walletModifier)
            if (wallets.applePay) ApplePayButton(onClick = onApplePay, modifier = walletModifier)
        }
    }
    if (wallets.any) LabeledDivider(stringResource(Res.string.sheet_or_pay_by_card))

    if (savedCards.isNotEmpty()) {
        // As tall as its text: the 40dp "All cards" target would otherwise push the row away from the divider above.
        Row(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.sheet_saved_cards),
                    style = typography.label.copy(color = textColor(colors.onSurfaceVariant), fontWeight = typography.title.fontWeight),
                    modifier = Modifier.placeholderText(),
                )
            }
            if (savedCards.size > SHEET_CARD_LIMIT || savedCards.any { it.canDelete }) {
                TextAction(
                    text = stringResource(Res.string.sheet_all_cards),
                    onClick = onAllCards,
                    modifier = Modifier.offset(x = 8.dp).wrapContentHeight(unbounded = true),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            savedCards.take(SHEET_CARD_LIMIT).forEach { card ->
                SavedCardRow(
                    card = card,
                    selected = card.bindingId == selectedId,
                    onClick = { selectedId = card.bindingId },
                )
            }
            NewCardRow(onClick = onNewCard)
        }
        val toCardStep = selected?.takeIf(opensCardStep)
        val label =
            toCardStep?.let { stringResource(Res.string.sheet_pay_with_card, ltr(CardFormatters.shortMask(it.maskedPan))) }
                ?: payLabel(order)
        PayButton(label = label, onClick = { selected?.let(onPaySaved) }, enabled = selected != null, showLock = toCardStep == null)
    } else {
        CheckoutButton(text = stringResource(Res.string.sheet_pay_by_card), onClick = onNewCard, primary = true)
    }
}

@Composable
internal fun SavedCardRow(
    card: SavedCardInfo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val network = CardNetworkResolver.resolveSaved(card.paymentSystem, card.maskedPan)
    MethodRow(
        title = CardFormatters.shortMask(card.maskedPan),
        subtitle = card.rowSubtitle(),
        numericTitle = true,
        selected = selected,
        onClick = onClick,
        role = Role.RadioButton,
        leading = { CardNetworkIcon(network) },
        trailing = { RadioMark(selected) },
    )
}

@Composable
internal fun NewCardRow(onClick: () -> Unit) {
    MethodRow(
        title = stringResource(Res.string.card_list_add_new),
        subtitle = stringResource(Res.string.sheet_new_card_hint),
        onClick = onClick,
        leading = { AddCardPlate() },
        trailing = { ChevronMark() },
    )
}
