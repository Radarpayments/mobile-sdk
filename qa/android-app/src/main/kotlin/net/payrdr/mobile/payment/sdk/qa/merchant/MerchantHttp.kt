package net.payrdr.mobile.payment.sdk.qa.merchant

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.http.Parameters
import io.ktor.util.flattenEntries
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** The client [Merchant] talks to the gateway with: the app's own, not the SDK's, as a merchant's server would be. */
internal fun merchantHttpClient(): HttpClient = HttpClient(OkHttp)

/** One POST to the gateway. Holds the credentials, so [toString] names only the URL. */
internal sealed class GatewayRequest(
    val url: String,
) {
    class Form(
        url: String,
        val parameters: Parameters,
    ) : GatewayRequest(url) {
        override fun printedBody(): String =
            parameters.flattenEntries().joinToString("&") { (name, value) ->
                "$name=${if (name == PASSWORD) GatewaySecrets.MASK else value}"
            }
    }

    /** [apiKey] goes in the `X-API-KEY` header and is never printed. */
    class Json(
        url: String,
        val body: JsonObject,
        val apiKey: String? = null,
    ) : GatewayRequest(url) {
        override fun printedBody(): String =
            JsonObject(body.mapValues { (name, value) -> if (name == PASSWORD) JsonPrimitive(GatewaySecrets.MASK) else value })
                .toString()
    }

    // Printed unencoded, so QA reads the values as sent; the password is replaced by name and then by value.
    protected abstract fun printedBody(): String

    fun printed(secrets: GatewaySecrets): String = secrets.bounded("→ POST $url ${printedBody()}", JOURNAL_LIMIT, CUT_MARKER)

    override fun toString(): String = "${this::class.simpleName}($url)"

    companion object {
        const val JOURNAL_LIMIT = 4_000
        const val CUT_MARKER = "…"
        private const val PASSWORD = "password"
    }
}
