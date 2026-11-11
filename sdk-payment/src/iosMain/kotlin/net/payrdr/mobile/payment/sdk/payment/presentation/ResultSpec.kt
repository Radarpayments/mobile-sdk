package net.payrdr.mobile.payment.sdk.payment.presentation

import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionExpiredException
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.result.PaymentReceipt
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.ui.screens.failureChip
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardFormatters
import net.payrdr.mobile.payment.sdk.payment.ui.util.GatewayDateTime
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import net.payrdr.mobile.payment.sdk.payment.ui.util.receiptDateText

/**
 * What the result screen says, worded as the Compose `ResultContent` words it.
 *
 * @property success the payment went through.
 * @property canRetry a failure the buyer may answer with another card.
 * @property title the headline.
 * @property amount the amount, isolated for a right-to-left line; null when neither the receipt nor the order has it.
 * @property message under the amount of a failure: the gateway's reason, or a general hint when it gave none.
 * @property chip the gateway's code or decline reason for support; null when there is none.
 * @property lines the receipt of a success, top to bottom.
 */
@PayrdrUiApi
public class ResultSpec internal constructor(
    public val success: Boolean,
    public val canRetry: Boolean,
    public val title: String,
    public val amount: String?,
    public val message: String?,
    public val chip: String?,
    public val lines: List<ReceiptLineSpec>,
) {
    /** Builds the result screen of an order. */
    public companion object {
        /** The result screen of [completed]; [dark] picks the card logo's colours for a dark sheet. */
        public fun of(
            completed: PaymentState.Completed,
            ui: CheckoutUi,
            strings: PaymentStrings,
            dark: Boolean,
        ): ResultSpec {
            val result = completed.result
            val receipt = completed.receipt
            val success = result is CheckoutResult.Completed
            val error = (result as? CheckoutResult.Failed)?.error
            val description = receipt?.actionCodeDescription?.takeIf { it.isNotBlank() }
            val hint = if (error is SdkSessionExpiredException) "result_session_expired" else "result_failure_hint"
            val title =
                when {
                    success && ui.saveCardOnly -> "result_card_saved_title"
                    success -> "result_success_title"
                    else -> "result_failure_title"
                }
            return ResultSpec(
                success = success,
                canRetry = result is CheckoutResult.Failed && ui.canReturnToForm,
                title = strings.text(title),
                amount = (receipt?.formattedAmount ?: ui.order?.formattedAmount)?.let(::ltr),
                message = if (success) null else description ?: strings.text(hint),
                chip = if (success) null else failureChip(description = description, error = error),
                lines = if (success && receipt != null) receiptLines(receipt, ui.order, strings, dark) else emptyList(),
            )
        }

        private fun receiptLines(
            receipt: PaymentReceipt,
            order: OrderSummary?,
            strings: PaymentStrings,
            dark: Boolean,
        ): List<ReceiptLineSpec> {
            val pan = receipt.panMasked?.takeIf { it.any(Char::isDigit) }
            val date =
                receipt.paymentDate?.let { raw ->
                    GatewayDateTime.parse(raw)?.let { receiptDateText(it, strings.language) } ?: raw
                }
            return buildList {
                if (pan != null) {
                    add(
                        ReceiptLineSpec(
                            label = strings.text("receipt_card"),
                            value = ltr(CardFormatters.shortMask(pan)),
                            card = true,
                            logo = NetworkLogoSpec.ofMaskedPan(pan, dark),
                        ),
                    )
                }
                if (date != null) add(ReceiptLineSpec(strings.text("receipt_date"), ltr(date)))
                order?.orderNumber?.let { number ->
                    add(ReceiptLineSpec(strings.text("receipt_order"), ltr(strings.format("receipt_order_number", number))))
                }
            }
        }
    }
}

/**
 * One line of the receipt: [label] at the start, [value] at the end. A [card] line keeps the slot of the network's
 * [logo] before the value even when the network is not recognised.
 */
@PayrdrUiApi
public class ReceiptLineSpec internal constructor(
    public val label: String,
    public val value: String,
    public val card: Boolean = false,
    public val logo: NetworkLogoSpec? = null,
)
