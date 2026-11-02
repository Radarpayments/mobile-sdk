package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

/**
 * Response from `getFinishedPaymentInfo.do` — the "what ultimately happened to this order" record.
 *
 * @property status order status, e.g. `DEPOSITED`; null when the gateway does not know the order
 * @property actionCode gateway result of the last attempt; non-zero when it was refused
 * @property displayErrorMessage why the last attempt was refused, worded for the payer
 * @property errorTypeName refusal category, e.g. `DATA_INPUT_ERROR`
 * @property sessionExpired the order's session ran out before a payment: the gateway declined it and sends nothing else
 * @property orderFeatures order features, e.g. `VERIFY` for an order that only saves a card
 */
@Serializable
public data class FinishedPaymentInfoResponse(
    val status: String? = null,
    val paymentDate: String? = null,
    val approvalCode: String? = null,
    val terminalId: String? = null,
    val refNum: String? = null,
    val panMasked: String? = null,
    val formattedAmount: String? = null,
    val actionCodeDescription: String? = null,
    val actionCode: Int? = null,
    val displayErrorMessage: String? = null,
    val errorTypeName: String? = null,
    val sessionExpired: Boolean = false,
    val orderFeatures: List<String> = emptyList(),
)
