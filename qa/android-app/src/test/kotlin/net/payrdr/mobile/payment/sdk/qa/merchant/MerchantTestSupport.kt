package net.payrdr.mobile.payment.sdk.qa.merchant

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import io.ktor.http.parseQueryString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import net.payrdr.mobile.payment.sdk.qa.config.ApiVersion
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.journal.Journal
import java.util.concurrent.CopyOnWriteArrayList

internal val V1 = StandProfile("v1", "https://dev.bpcbt.com/payment", login = "login-1", password = "password-1")
internal val V2 = StandProfile("v2", "https://dev.bpcbt.com/payment", api = ApiVersion.V2, apiKey = "key-1")

internal class SentRequest(
    val method: String,
    val url: String,
    val headers: Headers,
    val contentType: ContentType?,
    val body: String,
) {
    fun form(): Parameters = parseQueryString(body)

    fun json(): JsonObject = Json.parseToJsonElement(body).jsonObject
}

/** A gateway that records what it was sent and answers with [answer]. */
internal class FakeGateway(
    private val answer: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { ok("""{"orderId":"abc"}""") },
) {
    val requests: MutableList<SentRequest> = CopyOnWriteArrayList()
    val journal = Journal()

    val client =
        HttpClient(
            MockEngine { request ->
                requests +=
                    SentRequest(
                        request.method.value,
                        request.url.toString(),
                        request.headers,
                        request.body.contentType,
                        request.body.toByteArray().decodeToString(),
                    )
                answer(request)
            },
        )

    fun merchant(timeoutMillis: Long = 20_000): Merchant = Merchant(lazyOf(client), journal, timeoutMillis) { "qa-1" }

    fun journalText(): String = journal.entries.value.joinToString("\n") { it.message }

    fun single(): SentRequest = requests.single()
}

internal fun MockRequestHandleScope.ok(body: String): HttpResponseData = reply(HttpStatusCode.OK, body)

internal fun MockRequestHandleScope.reply(
    status: HttpStatusCode,
    body: String,
    contentType: ContentType = ContentType.Application.Json,
): HttpResponseData = respond(body, status, headersOf(HttpHeaders.ContentType, contentType.toString()))
