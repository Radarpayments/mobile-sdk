package net.payrdr.mobile.payment.sdk.payment.result

import net.payrdr.mobile.payment.sdk.payment.exception.SdkApplePayException
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

/**
 * Flattens a [CheckoutResult] into string pairs and back, so it can cross an Android activity result (an `Intent`
 * carries no objects). The error keeps its type and fields; its `cause` is not carried.
 */
internal object CheckoutResultCodec {
    private const val PREFIX = "net.payrdr.sdk.payment.result."
    private const val KIND = PREFIX + "KIND"
    private const val ORDER_ID = PREFIX + "ORDER_ID"
    private const val ERROR_TYPE = PREFIX + "ERROR_TYPE"
    private const val ERROR_MESSAGE = PREFIX + "ERROR_MESSAGE"
    private const val ERROR_CODE = PREFIX + "ERROR_CODE"
    private const val ERROR_HTTP_STATUS = PREFIX + "ERROR_HTTP_STATUS"
    private const val ERROR_ID = PREFIX + "ERROR_ID"

    fun encode(result: CheckoutResult): Map<String, String> =
        buildMap {
            put(ORDER_ID, result.orderId)
            when (result) {
                is CheckoutResult.Completed -> put(KIND, "completed")
                is CheckoutResult.Canceled -> put(KIND, "canceled")
                is CheckoutResult.Failed -> {
                    put(KIND, "failed")
                    putError(result.error)
                }
                is CheckoutResult.Unknown -> {
                    put(KIND, "unknown")
                    result.cause?.let { putError(it) }
                }
            }
        }

    fun decode(values: Map<String, String>): CheckoutResult {
        val orderId = values[ORDER_ID].orEmpty()
        return when (values[KIND]) {
            "completed" -> CheckoutResult.Completed(orderId)
            "canceled" -> CheckoutResult.Canceled(orderId)
            "failed" -> CheckoutResult.Failed(orderId, readError(values) ?: SdkException("Unknown error"))
            else -> CheckoutResult.Unknown(orderId, readError(values))
        }
    }

    /** The checkout closed without a result (the system finished or recreated it): the outcome is not known. */
    fun closedWithoutResult(orderId: String = ""): CheckoutResult = CheckoutResult.Unknown(orderId)

    private fun MutableMap<String, String>.putError(error: SdkException) {
        put(ERROR_TYPE, typeOf(error))
        error.message?.let { put(ERROR_MESSAGE, it) }
        when (error) {
            is SdkDeclinedException -> error.declineCode?.let { put(ERROR_CODE, it) }
            is SdkPaymentApiException -> {
                error.errorCode?.let { put(ERROR_CODE, it) }
                error.httpStatus?.let { put(ERROR_HTTP_STATUS, it.toString()) }
            }
            is SdkSessionNotFoundException -> put(ERROR_ID, error.sessionId)
            is SdkOrderNotFoundException -> put(ERROR_ID, error.mdOrder)
            is SdkSessionExpiredException -> put(ERROR_ID, error.mdOrder)
            is SdkInvalidSessionIdException -> put(ERROR_ID, error.sessionId)
            is SdkThreeDsChallengeFailedException -> put(ERROR_ID, error.reason)
            else -> Unit
        }
    }

    private fun typeOf(error: SdkException): String =
        when (error) {
            is SdkDeclinedException -> "declined"
            is SdkPaymentApiException -> "api"
            is SdkNetworkException -> "network"
            is SdkSessionNotFoundException -> "session_not_found"
            is SdkOrderNotFoundException -> "order_not_found"
            is SdkSessionExpiredException -> "session_expired"
            is SdkInvalidSessionIdException -> "invalid_session_id"
            is SdkCryptogramException -> "cryptogram"
            is SdkInvalidPublicKeyException -> "invalid_public_key"
            is SdkNotConfiguredException -> "not_configured"
            is SdkTransactionException -> "transaction"
            is SdkThreeDsChallengeFailedException -> "three_ds_failed"
            is SdkThreeDsTimeoutException -> "three_ds_timeout"
            is SdkGooglePayException -> "google_pay"
            is SdkApplePayException -> "apple_pay"
            else -> "other"
        }

    @Suppress("CyclomaticComplexMethod") // one branch per exception type
    private fun readError(values: Map<String, String>): SdkException? {
        val type = values[ERROR_TYPE] ?: return null
        val message = values[ERROR_MESSAGE]
        val code = values[ERROR_CODE]
        val id = values[ERROR_ID].orEmpty()
        return when (type) {
            "declined" -> SdkDeclinedException(declineCode = code, message = message)
            "api" -> SdkPaymentApiException(httpStatus = values[ERROR_HTTP_STATUS]?.toIntOrNull(), errorCode = code, message = message)
            "network" -> SdkNetworkException(message)
            "session_not_found" -> SdkSessionNotFoundException(id)
            "order_not_found" -> SdkOrderNotFoundException(id)
            "session_expired" -> SdkSessionExpiredException(id)
            "invalid_session_id" -> SdkInvalidSessionIdException(id)
            "cryptogram" -> SdkCryptogramException(message)
            "invalid_public_key" -> SdkInvalidPublicKeyException(message)
            "not_configured" -> SdkNotConfiguredException(message.orEmpty())
            "transaction" -> SdkTransactionException(message)
            "three_ds_failed" -> SdkThreeDsChallengeFailedException(id)
            "three_ds_timeout" -> SdkThreeDsTimeoutException()
            "google_pay" -> SdkGooglePayException(message)
            "apple_pay" -> SdkApplePayException(message)
            else -> SdkException(message)
        }
    }
}
