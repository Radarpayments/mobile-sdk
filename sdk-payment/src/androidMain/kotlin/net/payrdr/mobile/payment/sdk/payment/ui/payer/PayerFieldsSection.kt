package net.payrdr.mobile.payment.sdk.payment.ui.payer

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.stringResource
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_recipient_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_sender_title

/**
 * Payer fields the gateway asks for a card of [paymentSystems] (gateway names, see
 * [PayerFieldsState.specsFor]): contacts continue the card fields, the billing address is a block behind
 * a hairline; a transfer adds a block for its sender and then one for its recipient after the address.
 * Renders nothing when nothing is asked. Pair with [CountryPickerSheet] in the screen's overlay.
 *
 * @param collapsibleBilling an all-optional billing block starts as a row to add it, see [CollapsibleBillingGroup]
 */
@Composable
internal fun PayerFieldsSection(
    state: PayerFieldsState,
    paymentSystems: List<String>,
    modifier: Modifier = Modifier,
    collapsibleBilling: Boolean = false,
) {
    val specs = state.specsFor(paymentSystems)
    Column(modifier = modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val contact = specs.filter { it.field == PayerField.EMAIL || it.field == PayerField.MOBILE_PHONE }
        val sender = specs.filter { it.field in TransferFields.SENDER }
        val recipient = specs.filter { it.field in TransferFields.RECIPIENT }
        val billing = specs - (contact + sender + recipient).toSet()
        contact.forEach { if (it.field == PayerField.MOBILE_PHONE) PhoneField(state, it) else PayerTextField(state, it) }
        if (billing.isNotEmpty()) {
            val canAddLine = state.canAddAddressLine(paymentSystems)
            if (collapsibleBilling && billing.none { it.mandatory }) {
                CollapsibleBillingGroup(state = state, billing = billing, canAddLine = canAddLine)
            } else {
                BillingGroup(state = state, billing = billing, canAddLine = canAddLine)
            }
        }
        if (sender.isNotEmpty()) TransferGroup(state, stringResource(Res.string.transfer_sender_title), sender, "transfer_sender")
        if (recipient.isNotEmpty()) {
            TransferGroup(
                state,
                stringResource(Res.string.transfer_recipient_title),
                recipient,
                "transfer_recipient",
            )
        }
    }
}
