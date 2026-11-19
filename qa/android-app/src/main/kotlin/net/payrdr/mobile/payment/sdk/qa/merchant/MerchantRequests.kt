package net.payrdr.mobile.payment.sdk.qa.merchant

import io.ktor.http.Parameters
import io.ktor.http.Url
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import net.payrdr.mobile.payment.sdk.qa.config.Currencies
import net.payrdr.mobile.payment.sdk.qa.config.OrderConfig
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import java.util.UUID

// The SDK is the app here: the gateway's redirects after 3DS land on a URL nobody opens.
private const val DONE_URL = "sdk://done"
private const val WITHOUT_TO_CARD = "WITHOUT_TO_CARD"
private const val ORDER_NUMBER_DIGITS = 20

/** A number the gateway has not seen: it rejects a second order with the same number for the merchant. */
internal fun newOrderNumber(): String =
    "qa-" +
        UUID
            .randomUUID()
            .toString()
            .replace("-", "")
            .take(ORDER_NUMBER_DIGITS)

internal fun gatewayBase(profile: StandProfile): String = profile.gatewayUrl.trim().trimEnd('/')

/** The API v2 root sits on the gateway's host, not under its path: `https://host/payment` → `https://host/api2`. */
internal fun api2Url(baseUrl: String): String {
    val url = Url(baseUrl.trim())
    val port = if (url.specifiedPort == url.protocol.defaultPort || url.specifiedPort == 0) "" else ":${url.specifiedPort}"
    return "${url.protocol.name}://${url.host}$port/api2"
}

internal fun registerUrl(profile: StandProfile): String = "${gatewayBase(profile)}/rest/register.do"

internal fun transferUrl(profile: StandProfile): String = "${gatewayBase(profile)}/rest/api/p2p/registerP2P.do"

internal fun sessionUrl(profile: StandProfile): String = "${api2Url(profile.gatewayUrl)}/sessions"

internal fun statusUrl(profile: StandProfile): String = "${gatewayBase(profile)}/rest/getOrderStatusExtended.do"

/** `register.do`. The caller checked that the currency has a numeric code. */
internal fun registerForm(
    order: OrderConfig,
    profile: StandProfile,
    orderNumber: String,
): Parameters {
    val fields =
        linkedMapOf(
            "userName" to profile.login,
            "password" to profile.password,
            "amount" to order.amount.toString(),
            "currency" to numericCurrency(order),
            "orderNumber" to orderNumber,
            "returnUrl" to DONE_URL,
        )
    order.clientId?.takeIf { it.isNotBlank() }?.let { fields["clientId"] = it }
    if (order.features.isNotEmpty()) fields["features"] = order.features.joinToString(",")
    order.extra.forEach { (name, value) -> if (value is JsonNull) fields.remove(name) else fields[name] = formText(value) }
    return Parameters.build { fields.forEach { (name, value) -> append(name, value) } }
}

/** `registerP2P.do`, a one-leg transfer: the gateway requires WITHOUT_TO_CARD and the "A" indicator for it. */
internal fun transferJson(
    order: OrderConfig,
    profile: StandProfile,
    orderNumber: String,
): JsonObject {
    val body =
        buildJsonObject {
            put("username", profile.login)
            put("password", profile.password)
            put("amount", order.amount)
            put("currency", numericCurrency(order))
            put("orderNumber", orderNumber)
            put("returnUrl", DONE_URL)
            put("transactionTypeIndicator", "A")
            putJsonObject("features") {
                putJsonArray("feature") {
                    (listOf(WITHOUT_TO_CARD) + order.features).distinct().forEach { add(JsonPrimitive(it)) }
                }
            }
            order.clientId?.takeIf { it.isNotBlank() }?.let { put("clientId", it) }
        }
    return withExtra(body, order.extra)
}

/** `POST {api2}/sessions`; the API key goes in a header, not here. */
internal fun sessionJson(order: OrderConfig): JsonObject {
    val body =
        buildJsonObject {
            put("amount", order.amount)
            put("currency", order.currency)
            put("successUrl", DONE_URL)
            put("failureUrl", DONE_URL)
            order.clientId?.takeIf { it.isNotBlank() }?.let { put("merchantCustomerId", it) }
            // ⚠ Unconfirmed on a stand (no API key on the sandbox yet): sessions are assumed to take a plain array of names.
            if (order.features.isNotEmpty()) put("features", JsonArray(order.features.map(::JsonPrimitive)))
        }
    return withExtra(body, order.extra)
}

/** `getOrderStatusExtended.do`. */
internal fun statusForm(
    orderId: String,
    profile: StandProfile,
): Parameters =
    Parameters.build {
        append("userName", profile.login)
        append("password", profile.password)
        append("orderId", orderId)
    }

private fun numericCurrency(order: OrderConfig): String =
    requireNotNull(Currencies.numeric(order.currency)) { "Unknown currency ${order.currency}" }

// A form has only text: a JSON string goes without its quotes, any other value as its JSON text.
private fun formText(value: JsonElement): String = if (value is JsonPrimitive) value.content else value.toString()

// Map addition keeps the position of a replaced key, so the journal shows the request in its usual order. A null in
// extra is how QA leaves a parameter out.
private fun withExtra(
    body: JsonObject,
    extra: JsonObject,
): JsonObject = JsonObject((body + extra).filterValues { it !is JsonNull })
