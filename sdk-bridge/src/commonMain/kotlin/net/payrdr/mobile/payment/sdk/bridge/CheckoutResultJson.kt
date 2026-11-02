package net.payrdr.mobile.payment.sdk.bridge

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.payrdr.mobile.payment.sdk.payment.exception.SdkAlreadyPaidException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkApplePayException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkCancelledByUserException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkCryptogramException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkDeclinedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkGooglePayException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkInvalidPublicKeyException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkInvalidSessionIdException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNetworkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNotConfiguredException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkOrderNotFoundException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkPaymentApiException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionExpiredException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionNotFoundException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkThreeDsChallengeFailedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkThreeDsTimeoutException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkTransactionException
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult

/**
 * The result of a checkout as the plugins receive it:
 * `{"status": "completed" | "failed" | "canceled" | "unknown", "orderId": "...", "error": {"code": "...", "message": "..."}}`.
 * `error` comes with `failed`, and with `unknown` when the SDK saw an error while checking the order.
 */
internal object CheckoutResultJson {
    fun encode(result: CheckoutResult): String =
        when (result) {
            is CheckoutResult.Completed -> result("completed", result.orderId, error = null)
            is CheckoutResult.Canceled -> result("canceled", result.orderId, error = null)
            is CheckoutResult.Failed -> result("failed", result.orderId, errorOf(result.error))
            is CheckoutResult.Unknown -> result("unknown", result.orderId, result.cause?.let(::errorOf))
        }.toString()

    /** The checkout was not opened because the request broke the contract. */
    fun invalidRequest(message: String): String = failure(INVALID_REQUEST, message)

    /** The checkout was not opened: [code] says why (`busy`, `no_screen`, ...). */
    fun failure(
        code: String,
        message: String,
        orderId: String = "",
    ): String = result("failed", orderId, buildJsonObject { error(code, message) }).toString()

    private fun result(
        status: String,
        orderId: String,
        error: JsonObject?,
    ): JsonObject =
        buildJsonObject {
            put("status", status)
            put("orderId", orderId)
            error?.let { put("error", it) }
        }

    private fun errorOf(error: SdkException): JsonObject =
        buildJsonObject {
            error(codeOf(error), error.message)
            when (error) {
                is SdkDeclinedException -> error.declineCode?.let { put("declineCode", it) }
                is SdkPaymentApiException -> {
                    error.httpStatus?.let { put("httpStatus", it) }
                    error.errorCode?.let { put("gatewayErrorCode", it) }
                }
                else -> Unit
            }
        }

    private fun JsonObjectBuilder.error(
        code: String,
        message: String?,
    ) {
        put("code", code)
        message?.let { put("message", it) }
    }

    private fun codeOf(error: SdkException): String =
        when (error) {
            is SdkDeclinedException -> "declined"
            is SdkPaymentApiException -> "api"
            is SdkNetworkException -> "network"
            is SdkTransactionException -> "transaction"
            is SdkAlreadyPaidException -> "already_paid"
            is SdkSessionNotFoundException -> "session_not_found"
            is SdkOrderNotFoundException -> "order_not_found"
            is SdkSessionExpiredException -> "session_expired"
            is SdkInvalidSessionIdException -> "invalid_session_id"
            is SdkCryptogramException -> "cryptogram"
            is SdkInvalidPublicKeyException -> "invalid_public_key"
            is SdkNotConfiguredException -> "not_configured"
            is SdkCancelledByUserException -> "cancelled_by_user"
            is SdkThreeDsChallengeFailedException -> "three_ds_failed"
            is SdkThreeDsTimeoutException -> "three_ds_timeout"
            is SdkGooglePayException -> "google_pay"
            is SdkApplePayException -> "apple_pay"
            else -> "unknown"
        }

    const val INVALID_REQUEST = "invalid_request"
}
