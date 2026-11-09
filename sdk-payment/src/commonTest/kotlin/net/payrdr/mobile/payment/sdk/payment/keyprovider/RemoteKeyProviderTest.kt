package net.payrdr.mobile.payment.sdk.payment.keyprovider

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test

class RemoteKeyProviderTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun client(engine: MockEngine) =
        HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }

    private val successBody =
        """
        {
          "keys": [
            {
              "keyValue": "-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----",
              "protocolVersion": "RSA",
              "keyExpiration": 1999999999999
            }
          ]
        }
        """.trimIndent()

    @Test fun `returns first key on success`() =
        runTest {
            val engine =
                MockEngine {
                    respond(
                        successBody,
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val provider = RemoteKeyProvider(url = "https://example.com/se/keys.do", httpClient = client(engine))

            val key = provider.provideKey()

            key.protocol shouldBe "RSA"
            key.value.contains("AAAA") shouldBe true
        }

    @Test fun `a cancelled request stays a cancellation and does not become a key failure`() =
        runTest {
            val engine = MockEngine { throw CancellationException("checkout closed") }
            val provider = RemoteKeyProvider(url = "https://example.com/se/keys.do", httpClient = client(engine))

            shouldThrow<CancellationException> { provider.provideKey() }
        }

    @Test fun `throws KeyProviderException on HTTP 500`() =
        runTest {
            val engine = MockEngine { respond("boom", HttpStatusCode.InternalServerError) }
            val provider = RemoteKeyProvider(url = "https://example.com/se/keys.do", httpClient = client(engine))

            shouldThrow<KeyProviderException> { provider.provideKey() }
        }

    @Test fun `throws KeyProviderException on empty keys array`() =
        runTest {
            val engine =
                MockEngine {
                    respond(
                        """{"keys":[]}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val provider = RemoteKeyProvider(url = "https://example.com/se/keys.do", httpClient = client(engine))

            shouldThrow<KeyProviderException> { provider.provideKey() }
        }
}
