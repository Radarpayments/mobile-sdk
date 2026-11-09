package net.payrdr.mobile.payment.sdk.payment.support

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private val lenientJson = Json { ignoreUnknownKeys = true }

internal fun mockJsonClient(handle: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): Pair<HttpClient, MockEngine> {
    val engine = MockEngine(handle)
    val client =
        HttpClient(engine) {
            install(ContentNegotiation) { json(lenientJson) }
            expectSuccess = false
        }
    return client to engine
}

internal fun MockRequestHandleScope.json(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
) = respond(
    content = body,
    status = status,
    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
)

internal fun HttpRequestData.methodAndPath(): Pair<HttpMethod, String> = method to url.encodedPath
