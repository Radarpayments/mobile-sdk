package net.payrdr.mobile.payment.sdk.payment.flow

import net.payrdr.mobile.payment.sdk.payment.api.dto.response.SessionStatusResponse
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.result.PayerDataRequest
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardFormatters
import net.payrdr.mobile.payment.sdk.payment.utils.MoneyFormatter

/** Amount and order number of [session], the amount written the way [language] writes it. */
internal fun orderSummary(
    session: SessionStatusResponse,
    transfer: Transfer?,
    language: String?,
): OrderSummary {
    // An order that only saves a card pays nothing: its zero is not shown anywhere.
    val amount = if (transfer?.saveCardOnly == true) null else MoneyFormatter.format(session.amount, session.currencyAlphaCode, language)
    return OrderSummary(formattedAmount = amount, orderNumber = session.orderNumber)
}

/**
 * The card form of [session]: its saved cards, what it asks of the buyer, and the wallets on offer. A [transfer] pays
 * from the buyer's debit cards and saves a card by its own rules.
 */
internal fun cardForm(
    session: SessionStatusResponse,
    transfer: Transfer?,
    payerData: PayerDataRequest,
    googlePay: GooglePayConfig?,
    applePay: ApplePayConfig?,
): PaymentState.CardFormShown =
    PaymentState.CardFormShown(
        savedCards =
            (if (transfer != null) Transfer.debitCards(session) else session.bindingItems).map { item ->
                val (maskedPan, expiry) = CardFormatters.splitBindingLabel(item.label)
                SavedCardInfo(
                    bindingId = item.id,
                    maskedPan = maskedPan,
                    paymentSystem = item.paymentSystem,
                    cardHolder = item.cardHolder,
                    canDelete = session.bindingDeactivationEnabled,
                    expiry = expiry,
                    payerPrefill = PayerDataMapper.bindingPrefill(item),
                )
            },
        allowNewCard = true,
        cvcRequired = if (transfer != null) !transfer.cvcBindingNotRequired else !session.cvcNotRequired,
        canSaveCard = if (transfer != null) transfer.saveCard.shown else session.bindingEnabled,
        payerData = payerData,
        googlePay = googlePay,
        applePay = applePay,
        transfer = transfer != null,
        saveCardByDefault = transfer == null || transfer.saveCard == Transfer.SaveCard.LOCKED,
        saveCardLocked = transfer?.saveCard == Transfer.SaveCard.LOCKED,
        saveCardOnly = transfer?.saveCardOnly == true,
    )
