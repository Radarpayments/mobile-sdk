package net.payrdr.mobile.payment.sdk.payment.flow

import net.payrdr.mobile.payment.sdk.payment.api.dto.response.FinishedPaymentInfoResponse
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.exception.SdkDeclinedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkOrderNotFoundException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionExpiredException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionNotFoundException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkTransactionException
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PaymentReceipt
import net.payrdr.mobile.payment.sdk.payment.utils.MoneyFormatter
import net.payrdr.mobile.payment.sdk.payment.utils.ORDER_STATUS_FAILED
import net.payrdr.mobile.payment.sdk.payment.utils.ORDER_STATUS_PAID
import net.payrdr.mobile.payment.sdk.payment.utils.OrderStatus
import net.payrdr.mobile.payment.sdk.payment.utils.containsAnyOfKeywordIgnoreCase

/** Paid, then taken back: the payment did not go through for the buyer. */
private val ORDER_STATUS_UNDONE = listOf(OrderStatus.REVERSED, OrderStatus.REFUNDED)

/** `actionCode` of an order with no payment attempt recorded while its session is alive ("Waiting for payment attempt"). */
private const val ACTION_CODE_NO_ATTEMPT_YET = -100

/**
 * Publicly documented `actionCode`s for an outcome the gateway does not know yet, so not a decline: -30001 the status
 * inquiry to the bank's processing has started, 90 response status unknown, 998 the request timed out, 151019 the
 * transfer was sent but the bank did not answer.
 */
private val ACTION_CODES_OUTCOME_UNKNOWN = setOf(-30001, 90, 998, 151019)

/** Order feature of an order that only saves a card: a zero payment the gateway takes back. */
private const val FEATURE_VERIFY = "VERIFY"

/**
 * How the gateway's order status (`getFinishedPaymentInfo.do`) reads for one checkout: whether it settles the order,
 * and as what.
 *
 * @param orderId the id the checkout reports its result under
 * @param sessionFeatures the order features of the payment session (`VERIFY`: the order only saves a card)
 */
internal class OrderOutcome(
    private val orderId: String,
    private val mdOrder: String,
    private val sessionFeatures: List<String>,
) {
    /**
     * The outcome [info] settles, or null while the gateway has not settled the order: still CREATED with no code,
     * with -100 (no payment attempt recorded yet and the session is alive — not a decline), or with a code saying the
     * gateway itself does not know the outcome yet ([ACTION_CODES_OUTCOME_UNKNOWN]).
     */
    fun settled(info: FinishedPaymentInfoResponse): CheckoutResult? {
        val status = info.status
        val declineReason = declineReason(info)
        return when {
            // The gateway sends nothing but this flag for an order its session expired on (declined, never paid).
            info.sessionExpired -> CheckoutResult.Failed(orderId, SdkSessionExpiredException(mdOrder))
            isPaid(info) || cardSaved(info) -> CheckoutResult.Completed(orderId)
            awaitsAttempt(info) || info.actionCode in ACTION_CODES_OUTCOME_UNKNOWN -> null
            declineReason != null ->
                CheckoutResult.Failed(
                    orderId,
                    SdkDeclinedException(declineCode = info.errorTypeName ?: info.actionCode.toString(), message = declineReason),
                )
            status != null && status.containsAnyOfKeywordIgnoreCase(ORDER_STATUS_FAILED) ->
                CheckoutResult.Failed(orderId, SdkDeclinedException(message = "Order is declined"))
            status != null && status.containsAnyOfKeywordIgnoreCase(ORDER_STATUS_UNDONE) ->
                CheckoutResult.Failed(orderId, SdkTransactionException("Payment finished with status $status"))
            else -> null
        }
    }

    /**
     * The outcome for a session without `remainingSecs`, from the order's final status [info]: an order paid earlier
     * is [CheckoutResult.Completed], anything else cannot be paid any more.
     */
    fun ofClosedSession(
        info: FinishedPaymentInfoResponse,
        checkout: CheckoutConfig,
    ): CheckoutResult {
        val status = info.status
        val error =
            when {
                info.sessionExpired -> SdkSessionExpiredException(mdOrder)
                status == null ->
                    when (checkout) {
                        is CheckoutConfig.SessionId -> SdkSessionNotFoundException(checkout.value)
                        is CheckoutConfig.MdOrder -> SdkOrderNotFoundException(mdOrder)
                    }
                status.containsAnyOfKeywordIgnoreCase(ORDER_STATUS_PAID) || cardSaved(info) -> return CheckoutResult.Completed(orderId)
                // The gateway's code and wording when it has them (e.g. DECLINED_BY_MERCHANT: an API V2 session expired
                // by the merchant).
                status.containsAnyOfKeywordIgnoreCase(ORDER_STATUS_FAILED) ->
                    SdkDeclinedException(
                        declineCode = info.errorTypeName.orNullIfBlank(),
                        message = declineReason(info) ?: "Order is declined",
                    )
                else -> SdkTransactionException("Order cannot be paid, status $status")
            }
        return CheckoutResult.Failed(orderId, error)
    }

    /**
     * The receipt of a settled [info]. Its amount is the gateway's "3.90 EUR", written like the order amount before
     * it (the gateway may leave the code out, "3.90", hence [currency]); [orderAmount] when it cannot be read.
     */
    fun receipt(
        info: FinishedPaymentInfoResponse,
        currency: String?,
        language: String?,
        orderAmount: String?,
    ): PaymentReceipt =
        PaymentReceipt(
            formattedAmount =
                if (savesCardOnly(info)) {
                    null
                } else {
                    MoneyFormatter.format(info.formattedAmount, currency, language)
                        ?: orderAmount
                },
            paymentDate = info.paymentDate,
            panMasked = info.panMasked,
            approvalCode = info.approvalCode.orNullIfBlank(),
            refNum = info.refNum.orNullIfBlank(),
            actionCodeDescription = declineReason(info) ?: info.actionCodeDescription,
        )

    fun isPaid(info: FinishedPaymentInfoResponse): Boolean = info.status?.containsAnyOfKeywordIgnoreCase(ORDER_STATUS_PAID) == true

    /** No payment attempt is recorded and the session is alive (-100): nothing was charged, the order can still be paid. */
    fun awaitsAttempt(info: FinishedPaymentInfoResponse): Boolean = !isPaid(info) && info.actionCode == ACTION_CODE_NO_ATTEMPT_YET

    /**
     * The gateway's explanation of a decline for the payer; set only when an action code reports one (not -100 or a
     * code for an outcome the gateway does not know yet, whose texts read like "pending" but decline nothing).
     */
    fun declineReason(info: FinishedPaymentInfoResponse): String? {
        val code = info.actionCode ?: 0
        val declines = !isPaid(info) && code != 0 && code != ACTION_CODE_NO_ATTEMPT_YET && code !in ACTION_CODES_OUTCOME_UNKNOWN
        return (info.displayErrorMessage.orNullIfBlank() ?: info.actionCodeDescription.orNullIfBlank()).takeIf { declines }
    }

    /**
     * An order that only saves a card ended with the card saved: the gateway makes a zero payment and takes it back, so
     * `REVERSED` with action code 0 is its success, as on the gateway's own page.
     */
    private fun cardSaved(info: FinishedPaymentInfoResponse): Boolean =
        savesCardOnly(info) && info.actionCode == 0 && info.status?.containsAnyOfKeywordIgnoreCase(listOf(OrderStatus.REVERSED)) == true

    /** The order only saves a card (`VERIFY`): as the order status or the payment session tells. */
    private fun savesCardOnly(info: FinishedPaymentInfoResponse): Boolean =
        FEATURE_VERIFY in info.orderFeatures || FEATURE_VERIFY in sessionFeatures

    /** The gateway writes absent values as blank or as the string `null`. */
    private fun String?.orNullIfBlank(): String? = this?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
}
