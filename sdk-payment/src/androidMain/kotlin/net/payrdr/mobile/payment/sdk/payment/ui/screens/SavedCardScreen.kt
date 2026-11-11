package net.payrdr.mobile.payment.sdk.payment.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.result.PayerDataRequest
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.components.CardNetworkIcon
import net.payrdr.mobile.payment.sdk.payment.ui.components.CvcHintButton
import net.payrdr.mobile.payment.sdk.payment.ui.components.FieldGroupFrame
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupCellDivider
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupCellEdges
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupFieldStatus
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupInfoCell
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupRow
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupTextCell
import net.payrdr.mobile.payment.sdk.payment.ui.components.MethodRow
import net.payrdr.mobile.payment.sdk.payment.ui.components.PayButton
import net.payrdr.mobile.payment.sdk.payment.ui.components.SIDE_PADDING
import net.payrdr.mobile.payment.sdk.payment.ui.components.StepHeader
import net.payrdr.mobile.payment.sdk.payment.ui.components.TextFieldContentType
import net.payrdr.mobile.payment.sdk.payment.ui.components.keepVisibleWhileFocused
import net.payrdr.mobile.payment.sdk.payment.ui.components.payLabel
import net.payrdr.mobile.payment.sdk.payment.ui.payer.CountryPickerSheet
import net.payrdr.mobile.payment.sdk.payment.ui.payer.PayerSummarySection
import net.payrdr.mobile.payment.sdk.payment.ui.payer.rememberPayerFieldsState
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.theme.tabular
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardFormatters
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardInputValidation
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetwork
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetworkResolver
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_cvv_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.saved_card_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.stringResource

/**
 * Confirm a saved card: the card as its row in the payment methods with the CVV under it in one frame, and the payer
 * fields the gateway asks for this card's payment system. Skipped when neither is needed, see [needsSavedCardScreen].
 *
 * @param cvcRequired false when the gateway reports `cvcNotRequired` — no CVV field
 * @param onClose when non-null, the top bar gets a close action that leaves the whole checkout
 */
@Composable
internal fun SavedCardScreen(
    card: SavedCardInfo,
    order: OrderSummary?,
    onBack: () -> Unit,
    onSubmit: (PaymentIntent.SubmitSavedCard) -> Unit,
    modifier: Modifier = Modifier,
    cvcRequired: Boolean = true,
    payerData: PayerDataRequest = PayerDataRequest(),
    onClose: (() -> Unit)? = null,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    val network = CardNetworkResolver.resolveSaved(card.paymentSystem, card.maskedPan)
    var cvc by remember(card.bindingId) { mutableStateOf("") }
    var touched by remember(card.bindingId) { mutableStateOf(false) }
    var cvvFocused by remember(card.bindingId) { mutableStateOf(false) }
    val error = if (cvcRequired) CardInputValidation.cvv(cvc, network) else null
    val payer = rememberPayerFieldsState(payerData, card.payerPrefill)
    val paymentSystems = savedCardPaymentSystems(card)
    val payLabel = payLabel(order)

    PageScaffold(
        modifier = modifier.testTag("SavedCardScreen"),
        topBar = {
            StepHeader(
                title = stringResource(Res.string.saved_card_title),
                onBack = onBack,
                onClose = onClose,
                modifier = Modifier.padding(start = SIDE_PADDING, end = SIDE_PADDING, top = 2.dp, bottom = 8.dp),
            )
        },
        footer = {
            PayButton(
                label = payLabel,
                onClick = {
                    touched = true
                    val payerSpecs = payer.specsFor(paymentSystems)
                    val payerValid = payer.validateAll(payerSpecs)
                    if (error == null && payerValid) {
                        onSubmit(
                            PaymentIntent.SubmitSavedCard(
                                bindingId = card.bindingId,
                                cvc = cvc.takeIf { cvcRequired },
                                payerData = payer.submitValues(payerSpecs),
                            ),
                        )
                    }
                },
            )
        },
        overlay = { CountryPickerSheet(payer) },
        // The CVV takes the focus with the screen: Pay stays in reach above the keys.
        footerOverKeyboard = true,
    ) {
        val cvvError = error?.takeIf { touched }
        val cvvHighlight =
            when {
                cvvError != null -> colors.error
                cvvFocused -> colors.primary
                else -> null
            }
        // One frame. With a CVV to type: the card and the CVV side by side, like the new card's date and CVV; without one,
        // the card as the row it was picked from (logo, last digits, network and expiry).
        Column(
            modifier = Modifier.fillMaxWidth().keepVisibleWhileFocused(cvvFocused),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FieldGroupFrame {
                if (!cvcRequired) {
                    MethodRow(
                        title = CardFormatters.shortMask(card.maskedPan),
                        subtitle = card.rowSubtitle(),
                        numericTitle = true,
                        framed = false,
                        leading = { CardNetworkIcon(network) },
                    )
                } else {
                    GroupRow(highlighted = cvvHighlight != null) {
                        // A missing or short CVV outlines the whole frame, the hairline between the cells included.
                        SavedCardCell(
                            card = card,
                            network = network,
                            highlight = colors.error.takeIf { cvvError != null },
                            modifier = Modifier.weight(3f),
                        )
                        GroupCellDivider()
                        val maxCvc = CardFormatters.cvvLength(network)
                        GroupTextCell(
                            value = cvc,
                            onValueChange = { text ->
                                cvc = CardFormatters.digitsOnly(text).take(maxCvc)
                                if (cvc.length == maxCvc) touched = true
                            },
                            placeholder = stringResource(Res.string.card_form_cvv_label),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            contentType = TextFieldContentType.CREDIT_CARD_SECURITY_CODE,
                            status = GroupFieldStatus(isError = cvvError != null, isValid = touched && error == null),
                            highlight = cvvHighlight,
                            edges = GroupCellEdges.of(row = 0, rows = 1, column = 1, columns = 2),
                            onFocusChange = { cvvFocused = it },
                            // The CVV is the only thing to type here: the keyboard opens with the screen.
                            autoFocus = true,
                            modifier = Modifier.weight(2f),
                            trailing = { CvcHintButton(network) },
                        )
                    }
                }
            }
            if (cvvError != null) {
                Text(text = stringResource(cvvError.message()), style = typography.caption.copy(color = colors.error))
            }
        }
        PayerSummarySection(state = payer, paymentSystems = paymentSystems)
    }
}

/** The saved card as the left cell of the card and CVV row: logo, last digits and, under them, the expiry. */
@Composable
private fun SavedCardCell(
    card: SavedCardInfo,
    network: CardNetwork,
    highlight: Color?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    GroupInfoCell(highlight = highlight, edges = GroupCellEdges.of(row = 0, rows = 1, column = 0, columns = 2), modifier = modifier) {
        CardNetworkIcon(network)
        Spacer(Modifier.width(6.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = ltr(CardFormatters.shortMask(card.maskedPan)),
                style = typography.body.copy(color = colors.onBackground, fontWeight = typography.title.fontWeight).tabular(),
                maxLines = 1,
            )
            card.expiry?.let { expiry ->
                Text(
                    text = ltr(expiry),
                    style = typography.caption.tabular().copy(color = colors.onSurfaceVariant),
                    maxLines = 1,
                )
            }
        }
    }
}
