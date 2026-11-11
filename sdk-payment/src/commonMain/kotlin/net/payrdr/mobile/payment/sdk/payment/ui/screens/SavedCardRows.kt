package net.payrdr.mobile.payment.sdk.payment.ui.screens

import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetworkResolver
import net.payrdr.mobile.payment.sdk.payment.ui.util.displayName
import net.payrdr.mobile.payment.sdk.payment.ui.util.gatewayPaymentSystems

/** Gateway payment-system names to look up payer fields for a binding: its own name first, then the network's. */
internal fun savedCardPaymentSystems(card: SavedCardInfo): List<String> =
    (listOf(card.paymentSystem) + CardNetworkResolver.resolveSaved(card.paymentSystem, card.maskedPan).gatewayPaymentSystems)
        .map { it.trim().uppercase() }
        .filter { it.isNotEmpty() }
        .distinct()

/** The confirm step is needed when the gateway wants a CVV or payer fields for this card. */
internal fun needsSavedCardScreen(
    form: PaymentState.CardFormShown,
    card: SavedCardInfo,
): Boolean = form.cvcRequired || savedCardPaymentSystems(card).any { form.payerData.fieldsFor(it).isNotEmpty() }

/** Second line of a saved-card row: network and expiry, e.g. `Visa · 12/26`. */
internal fun SavedCardInfo.rowSubtitle(): String? {
    val network = CardNetworkResolver.resolveSaved(paymentSystem, maskedPan)
    return listOfNotNull(network.displayName.ifEmpty { paymentSystem }.ifEmpty { null }, expiry)
        .joinToString(" · ")
        .ifEmpty { null }
}
