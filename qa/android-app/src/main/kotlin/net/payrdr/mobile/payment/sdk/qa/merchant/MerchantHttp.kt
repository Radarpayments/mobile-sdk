package net.payrdr.mobile.payment.sdk.qa.merchant

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.http.Parameters
import io.ktor.util.flattenEntries
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** The client [Merchant] talks to the gateway with: the app's own, not the SDK's, as a merchant's server would be. */
internal fun merchantHttpClient(): HttpClient =
    HttpClient(OkHttp) {
        engine {
            // OkHttp's own 10 s connect, read and write timeouts would fire before the merchant's 20 s and end a stalled
            // exchange with "SocketTimeoutException: timeout", where iOS reports the contract's "did not answer": with
            // them off (0 is "none"), the merchant's coroutine timeout is the only one on both platforms.
            preconfigured =
                OkHttpClient
                    .Builder()
                    .connectTimeout(0, TimeUnit.MILLISECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .writeTimeout(0, TimeUnit.MILLISECONDS)
                    .callTimeout(0, TimeUnit.MILLISECONDS)
                    .build()
        }
    }

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
        override fun printedHeaders(): String = if (apiKey != null) " $API_KEY_HEADER: ${GatewaySecrets.MASK}" else ""

        override fun printedBody(): String =
            JsonObject(body.mapValues { (name, value) -> if (name == PASSWORD) JsonPrimitive(GatewaySecrets.MASK) else value })
                .toString()
    }

    // Printed unencoded, so QA reads the values as sent; the password is replaced by name and then by value.
    protected abstract fun printedBody(): String

    protected open fun printedHeaders(): String = ""

    fun printed(secrets: GatewaySecrets): String =
        secrets.bounded("→ POST $url${printedHeaders()} ${printedBody()}", JOURNAL_LIMIT, CUT_MARKER)

    override fun toString(): String = "${this::class.simpleName}($url)"

    companion object {
        const val JOURNAL_LIMIT = 4_000
        const val CUT_MARKER = "…"
        const val API_KEY_HEADER = "X-API-KEY"
        private const val PASSWORD = "password"
    }
}
