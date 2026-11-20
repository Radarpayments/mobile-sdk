package net.payrdr.mobile.payment.sdk.payment.gpay

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The token the gateway takes from Google Pay's `PaymentData` JSON — `paymentMethodData.tokenizationData.token`, itself
 * a JSON document passed on as the string it is — or null when the answer has none.
 */
internal fun googlePayToken(paymentDataJson: String): String? {
    val methodData = paymentData(paymentDataJson)?.get("paymentMethodData") as? JsonObject
    val tokenization = methodData?.get("tokenizationData") as? JsonObject
    return tokenization.text("token")
}

/**
 * The buyer's email and phone from Google Pay's `PaymentData` JSON — `email` and
 * `paymentMethodData.info.billingAddress.phoneNumber` — each null when the answer has none.
 */
internal fun googlePayPayer(paymentDataJson: String): GooglePayPayer {
    val root = paymentData(paymentDataJson)
    val info = (root?.get("paymentMethodData") as? JsonObject)?.get("info") as? JsonObject
    return GooglePayPayer(email = root.text("email"), phone = (info?.get("billingAddress") as? JsonObject).text("phoneNumber"))
}

private fun paymentData(json: String): JsonObject? = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject

private fun JsonObject?.text(name: String): String? =
    (this?.get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
