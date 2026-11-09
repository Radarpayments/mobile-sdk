package net.payrdr.mobile.payment.sdk.payment.gpay

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The token the gateway takes from Google Pay's `PaymentData` JSON — `paymentMethodData.tokenizationData.token`, itself
 * a JSON document passed on as the string it is — or null when the answer has none.
 */
internal fun googlePayToken(paymentDataJson: String): String? {
    val root = runCatching { Json.parseToJsonElement(paymentDataJson) }.getOrNull() as? JsonObject ?: return null
    val methodData = root["paymentMethodData"] as? JsonObject
    val tokenization = methodData?.get("tokenizationData") as? JsonObject
    val token = tokenization?.get("token") as? JsonPrimitive
    return token?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
}
