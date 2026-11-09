package net.payrdr.mobile.payment.samples.android.merchant

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.math.BigInteger
import java.util.UUID

/**
 * "Your server": registers an order at the gateway and hands back its mdOrder (API V1) or its session (API V2).
 *
 * In a real integration this code lives on the merchant's backend, which keeps the merchant
 * login and password or API key; the app only receives the order. The demo registers orders from the app
 * so that it works without a backend — never ship merchant credentials inside an app.
 */
fun interface OrderRegistrar {
    /** Registers an order for [amountMinor] euro cents; [clientId] links it to a buyer's saved cards, null = guest. */
    suspend fun register(
        amountMinor: Long,
        clientId: String?,
    ): OrderRegistration
}

/** Outcome of [OrderRegistrar.register]. */
sealed interface OrderRegistration {
    /**
     * The gateway accepted the order: pass [sessionId] to the SDK when there is one (`presentSession`, API V2),
     * [mdOrder] otherwise (`present`, API V1). An API V2 session has an mdOrder too, the one its id encodes.
     */
    data class Registered(
        val mdOrder: String,
        val sessionId: String? = null,
    ) : OrderRegistration

    /** Login or password is not set in the developer settings; nothing was sent. */
    data object MissingCredentials : OrderRegistration

    /** The API V2 key is not set in the developer settings; nothing was sent. */
    data object MissingApiKey : OrderRegistration

    /** The gateway refused the order or could not be reached. */
    data class Failed(
        val message: String,
    ) : OrderRegistration
}

/**
 * Registers orders through API V1 of the gateway at [baseUrl], in euros: a payment with `register.do`, an AFT transfer
 * with `registerP2P.do` ([orderType]); gives up after [timeoutMillis].
 */
@Suppress("LongParameterList") // one per developer setting
class GatewayOrderRegistrar(
    private val baseUrl: String,
    private val login: String,
    private val password: String,
    private val http: HttpClient,
    private val timeoutMillis: Long = TIMEOUT_MS,
    val orderType: OrderType = OrderType.PURCHASE,
    val prefill: TransferPrefill? = null,
) : OrderRegistrar {
    override suspend fun register(
        amountMinor: Long,
        clientId: String?,
    ): OrderRegistration {
        if (login.isBlank() || password.isBlank()) return OrderRegistration.MissingCredentials
        if (orderType == OrderType.AFT) return registerTransfer(amountMinor, clientId)
        return callGateway(timeoutMillis, ::parseRegisterResponse) {
            http.submitForm(
                url = "${baseUrl.trimEnd('/')}/rest/register.do",
                formParameters =
                    parameters {
                        append("userName", login)
                        append("password", password)
                        append("amount", amountMinor.toString())
                        append("currency", EUR)
                        append("orderNumber", newOrderNumber())
                        append("returnUrl", RETURN_URL)
                        if (clientId != null) append("clientId", clientId)
                    },
            )
        }
    }

    /**
     * A one-leg AFT transfer (`WITHOUT_TO_CARD`, transaction type `A`): the buyer's card is debited, the money goes to
     * the recipient; the sheet asks for the sender and recipient details the gateway needs.
     *
     * A transfer of 0 only saves the buyer's card for [clientId]: the gateway checks the card and reverses (`VERIFY`),
     * and refuses `VERIFY` without `FORCE_CREATE_BINDING`.
     */
    private suspend fun registerTransfer(
        amountMinor: Long,
        clientId: String?,
    ): OrderRegistration {
        val features = if (amountMinor == 0L) listOf(WITHOUT_TO_CARD, VERIFY, FORCE_CREATE_BINDING) else listOf(WITHOUT_TO_CARD)
        val order =
            buildJsonObject {
                put("username", login)
                put("password", password)
                put("amount", amountMinor)
                put("currency", EUR)
                put("orderNumber", newOrderNumber())
                put("returnUrl", RETURN_URL)
                put("transactionTypeIndicator", "A")
                put("features", buildJsonObject { put("feature", JsonArray(features.map(::JsonPrimitive))) })
                if (clientId != null) put("clientId", clientId)
                prefill?.let { p ->
                    p.email?.let { put("email", it) }
                    if (p.billingPayerData.isNotEmpty()) {
                        put(
                            "billingPayerData",
                            JsonObject(p.billingPayerData.mapValues { JsonPrimitive(it.value) }),
                        )
                    }
                    if (p.billingRecipientData.isNotEmpty()) {
                        put("billingRecipientData", JsonObject(p.billingRecipientData.mapValues { JsonPrimitive(it.value) }))
                    }
                }
            }
        return callGateway(timeoutMillis, ::parseRegisterResponse) {
            http.post("${baseUrl.trimEnd('/')}/rest/api/p2p/registerP2P.do") {
                contentType(ContentType.Application.Json)
                setBody(order.toString())
            }
        }
    }

    override fun toString(): String = "GatewayOrderRegistrar(baseUrl=$baseUrl, orderType=$orderType)"

    // Gateways limit orderNumber to 32 characters; a full UUID with the prefix is 41.
    private fun newOrderNumber() =
        "demo-" +
            UUID
                .randomUUID()
                .toString()
                .replace("-", "")
                .take(ORDER_NUMBER_RANDOM_CHARS)

    private companion object {
        const val EUR = "978"
        const val ORDER_NUMBER_RANDOM_CHARS = 20
        const val WITHOUT_TO_CARD = "WITHOUT_TO_CARD"
        const val VERIFY = "VERIFY"
        const val FORCE_CREATE_BINDING = "FORCE_CREATE_BINDING"
    }
}

/**
 * Creates API V2 sessions (`POST /api2/sessions`) at the gateway of [baseUrl] with the merchant's [apiKey], in euros;
 * gives up after [timeoutMillis]. The buyer comes back from the bank page to `sdk://done`, which the SDK expects
 * (the merchant's own pages work too).
 */
class ApiV2SessionRegistrar(
    private val baseUrl: String,
    private val apiKey: String,
    private val http: HttpClient,
    private val timeoutMillis: Long = TIMEOUT_MS,
) : OrderRegistrar {
    override suspend fun register(
        amountMinor: Long,
        clientId: String?,
    ): OrderRegistration {
        if (apiKey.isBlank()) return OrderRegistration.MissingApiKey
        val session =
            buildJsonObject {
                put("amount", amountMinor)
                put("currency", "EUR")
                put("successUrl", RETURN_URL)
                put("failureUrl", RETURN_URL)
                // API V2's name for the buyer whose cards the gateway keeps (API V1's clientId).
                if (clientId != null) put("merchantCustomerId", clientId)
            }
        return callGateway(timeoutMillis, ::parseSessionResponse) {
            http.post("${api2Url(baseUrl)}/sessions") {
                header("X-API-KEY", apiKey)
                contentType(ContentType.Application.Json)
                setBody(session.toString())
            }
        }
    }

    override fun toString(): String = "ApiV2SessionRegistrar(baseUrl=$baseUrl)"
}

/**
 * The registrar for the API version and order type chosen in the developer settings; transfers go through API V1 and
 * carry [prefill] when there is one.
 */
@Suppress("LongParameterList") // one per developer setting
fun merchantRegistrar(
    apiVersion: ApiVersion,
    baseUrl: String,
    login: String,
    password: String,
    apiKey: String,
    http: HttpClient,
    orderType: OrderType = OrderType.PURCHASE,
    prefill: TransferPrefill? = null,
): OrderRegistrar =
    when (apiVersion) {
        ApiVersion.V1 -> GatewayOrderRegistrar(baseUrl, login, password, http, orderType = orderType, prefill = prefill)
        ApiVersion.V2 -> ApiV2SessionRegistrar(baseUrl, apiKey, http)
    }

/** The merchant API a demo order is registered through. */
enum class ApiVersion { V1, V2 }

/** What a demo order is: a payment, or a one-leg AFT transfer (API V1 `registerP2P.do`). */
enum class OrderType { PURCHASE, AFT }

/**
 * Details a transfer is registered with so that the sheet does not ask for them: the buyer's [email], and
 * `billingPayerData` / `billingRecipientData` as `registerP2P.do` takes them (gateway field name → value).
 */
data class TransferPrefill(
    val email: String?,
    val billingPayerData: Map<String, String>,
    val billingRecipientData: Map<String, String>,
)

/** API V2 is served next to the payment page: `https://host/payment` → `https://host/api2`. */
internal fun api2Url(baseUrl: String): String {
    val url = Url(baseUrl.trim())
    val port = if (url.specifiedPort == url.protocol.defaultPort || url.specifiedPort == 0) "" else ":${url.specifiedPort}"
    return "${url.protocol.name}://${url.host}$port/api2"
}

/** Reads a `POST /api2/sessions` response: the session on success, `error.errorMessage` otherwise. */
internal fun parseSessionResponse(body: String): OrderRegistration {
    val json = runCatching { Json.parseToJsonElement(body) as? JsonObject }.getOrNull()
    json ?: return OrderRegistration.Failed("Unexpected gateway response")
    json["id"]?.jsonPrimitive?.content?.let { id ->
        val mdOrder = mdOrderOfSession(id) ?: return OrderRegistration.Failed("Unexpected session id $id")
        return OrderRegistration.Registered(mdOrder, sessionId = id)
    }
    val error = json["error"] as? JsonObject
    val message = error?.get("errorMessage")?.jsonPrimitive?.content
    return OrderRegistration.Failed(message ?: "Gateway error ${error?.get("errorCode")?.jsonPrimitive?.content.orEmpty()}")
}

/** The mdOrder an API V2 session id encodes: the id is `ps_` + Base58 of `ps:` + mdOrder. Null for anything else. */
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

private const val SESSION_PREFIX = "ps_"
private const val DECODED_PREFIX = "ps:"
private const val BASE58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
private val BASE58_RADIX = BigInteger.valueOf(58)

/** One gateway call of "your server": real time on the IO dispatcher, so a stalled DNS lookup cannot keep "Pay" busy. */
private suspend fun callGateway(
    timeoutMillis: Long,
    parse: (String) -> OrderRegistration,
    request: suspend () -> HttpResponse,
): OrderRegistration =
    try {
        val response =
            withContext(Dispatchers.IO) { withTimeoutOrNull(timeoutMillis) { request() } }
                ?: return OrderRegistration.Failed("The gateway did not answer in ${timeoutMillis / MS_IN_S} s")
        parse(response.bodyAsText())
    } catch (e: CancellationException) {
        throw e
    } catch (
        @Suppress("TooGenericExceptionCaught") e: Exception,
    ) {
        OrderRegistration.Failed(e.message ?: e::class.simpleName.orEmpty())
    }

private const val RETURN_URL = "sdk://done"
private const val TIMEOUT_MS = 20_000L
private const val MS_IN_S = 1000

/** Reads a `register.do` / `registerP2P.do` response: `orderId` on success, `errorMessage` otherwise. */
internal fun parseRegisterResponse(body: String): OrderRegistration {
    val json = runCatching { Json.parseToJsonElement(body) as? JsonObject }.getOrNull()
    json ?: return OrderRegistration.Failed("Unexpected gateway response")
    json["orderId"]?.jsonPrimitive?.content?.let { return OrderRegistration.Registered(it) }
    val message = json["errorMessage"]?.jsonPrimitive?.content
    return OrderRegistration.Failed(message ?: "Gateway error ${json["errorCode"]?.jsonPrimitive?.content.orEmpty()}")
}
