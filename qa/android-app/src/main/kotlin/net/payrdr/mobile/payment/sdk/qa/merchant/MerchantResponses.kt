package net.payrdr.mobile.payment.sdk.qa.merchant

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigInteger

/** What registering an order gave: the ids the SDK opens the checkout with, or why there are none. */
internal sealed interface Registration {
    /** `register.do` and `registerP2P.do` give [mdOrder]; a session gives [sessionId] and, when it decodes, [mdOrder]. */
    data class Registered(
        val mdOrder: String?,
        val sessionId: String?,
    ) : Registration

    /** [httpStatus] is null when no answer came. [message] carries no secret and is bounded in length. */
    data class Failed(
        val httpStatus: Int?,
        val message: String,
    ) : Registration
}

/** The order's state as the merchant's backend sees it after the checkout. */
internal sealed interface OrderStatus {
    data class Known(
        val orderStatus: Int?,
        val actionCode: Int?,
        val actionCodeDescription: String?,
    ) : OrderStatus

    /** [httpStatus] is null when no answer came. [message] carries no secret and is bounded in length. */
    data class Failed(
        val httpStatus: Int?,
        val message: String,
    ) : OrderStatus

    /** API v2 has no status call for the merchant, or there were no credentials or no order to ask about. */
    data object NotQueried : OrderStatus
}

private const val MESSAGE_LIMIT = 300
private const val SESSION_PREFIX = "ps_"
private const val DECODED_PREFIX = "ps:"
private const val BASE58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
private val BASE58_RADIX = BigInteger.valueOf(58)

/** `register.do` / `registerP2P.do`: the order id on success, the gateway's error otherwise. */
internal fun registerAnswer(
    status: Int,
    body: String,
    secrets: GatewaySecrets,
): Registration {
    val json = jsonObjectOf(body)
    val orderId = json?.text("orderId")
    if (status.isSuccess() && orderId != null) return Registration.Registered(orderId, null)
    return Registration.Failed(status, json?.gatewayError(secrets) ?: httpFailure(status, body, secrets))
}

/** `POST {api2}/sessions`: the session id on success, `error.errorMessage` otherwise. */
internal fun sessionAnswer(
    status: Int,
    body: String,
    secrets: GatewaySecrets,
): Registration {
    val json = jsonObjectOf(body)
    val id = json?.text("id")
    if (status.isSuccess() && id != null) return Registration.Registered(mdOrderOfSession(id), id)
    val error = json?.get("error") as? JsonObject
    return Registration.Failed(status, error?.gatewayError(secrets) ?: httpFailure(status, body, secrets))
}

/** `getOrderStatusExtended.do`. An answer with none of the three fields is not a status, whatever its HTTP code. */
internal fun statusAnswer(
    status: Int,
    body: String,
    secrets: GatewaySecrets,
): OrderStatus {
    val json = jsonObjectOf(body)
    json?.gatewayError(secrets)?.let { return OrderStatus.Failed(status, it) }
    if (json != null && status.isSuccess()) {
        val orderStatus = json.text("orderStatus")?.toIntOrNull()
        val actionCode = json.text("actionCode")?.toIntOrNull()
        val description = json.text("actionCodeDescription")?.let { secrets.bounded(it, MESSAGE_LIMIT) }
        if (orderStatus != null || actionCode != null || description != null) {
            return OrderStatus.Known(orderStatus, actionCode, description)
        }
    }
    return OrderStatus.Failed(status, httpFailure(status, body, secrets))
}

/** The mdOrder inside a session id: `ps_` + Base58 of `ps:<mdOrder>`. Null when the id is not of that form. */
internal fun mdOrderOfSession(sessionId: String): String? {
    if (!sessionId.startsWith(SESSION_PREFIX)) return null
    val encoded = sessionId.removePrefix(SESSION_PREFIX)
    var number = BigInteger.ZERO
    for (char in encoded) {
        val digit = BASE58.indexOf(char).takeIf { it >= 0 } ?: return null
        number = number * BASE58_RADIX + digit.toBigInteger()
    }
    // toByteArray() adds a sign byte; each leading "1" stands for a zero byte.
    val bytes = number.toByteArray().dropWhile { it == 0.toByte() }
    val zeros = List(encoded.takeWhile { it == BASE58[0] }.length) { 0.toByte() }
    val decoded = (zeros + bytes).toByteArray().decodeToString()
    return decoded.removePrefix(DECODED_PREFIX).takeIf { decoded.startsWith(DECODED_PREFIX) && it.isNotEmpty() }
}

private fun Int.isSuccess(): Boolean = this in 200..299

private fun jsonObjectOf(body: String): JsonObject? =
    try {
        Json.parseToJsonElement(body) as? JsonObject
    } catch (_: SerializationException) {
        null
    }

// A blank or null field is as good as absent: the gateway sends "orderId": "" with some errors.
private fun JsonObject.text(name: String): String? =
    (get(name) as? JsonPrimitive)
        ?.takeUnless { it is JsonNull }
        ?.content
        ?.takeIf { it.isNotBlank() }

// errorCode "0" is the gateway's "no error"; a message beside it is not a failure reason.
private fun JsonObject.gatewayError(secrets: GatewaySecrets): String? {
    val code = text("errorCode")
    val message = text("errorMessage")
    val error =
        when {
            code == "0" -> null
            message != null -> message
            code != null -> "Gateway error $code"
            else -> null
        }
    return error?.let { secrets.bounded(it, MESSAGE_LIMIT) }
}

// Whatever came instead of an answer — an HTML error page, a proxy's text, nothing — so QA sees which it was.
private fun httpFailure(
    status: Int,
    body: String,
    secrets: GatewaySecrets,
): String = "HTTP $status: " + if (body.isBlank()) "(empty body)" else secrets.bounded(body, MESSAGE_LIMIT)
