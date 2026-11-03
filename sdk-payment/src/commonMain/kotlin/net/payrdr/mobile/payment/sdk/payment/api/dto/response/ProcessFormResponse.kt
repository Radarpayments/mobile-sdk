package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from `processform.do` / `processBindingForm.do`.
 *
 * If [threeDSMethodURL] is non-null the merchant is configured for the EMV 3DS2 native SDK,
 * which this SDK no longer supports. [PaymentFlowController] surfaces this as
 * `SdkNotConfiguredException`.
 *
 * If [acsUrl] is non-null (and [threeDSMethodURL] is null) — 3DS v1 web challenge is required.
 * If [error] is set the gateway refused the payment outright, still with `errorCode` 0
 * (e.g. `DATA_INPUT_ERROR`). Otherwise the payment is frictionless and will be finalized by polling
 * `getFinishedPaymentInfo`.
 *
 * @property error refusal reason in the gateway's words
 * @property displayErrorMessage the same reason worded for the payer, when the gateway provides it
 * @property errorTypeName refusal category, e.g. `DATA_INPUT_ERROR`
 */
@Serializable
internal data class ProcessFormResponse(
    val errorCode: Int,
    val acsUrl: String? = null,
    val paReq: String? = null,
    val termUrl: String? = null,
    @SerialName("threeDSMethodURL")
    val threeDSMethodURL: String? = null,
    val error: String? = null,
    val displayErrorMessage: String? = null,
    val errorTypeName: String? = null,
)
