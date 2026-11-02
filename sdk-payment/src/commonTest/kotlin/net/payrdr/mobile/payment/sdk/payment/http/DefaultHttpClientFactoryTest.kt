package net.payrdr.mobile.payment.sdk.payment.http

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import kotlin.test.Test

class DefaultHttpClientFactoryTest {
    @Test fun `creates client with ContentNegotiation plugin installed`() {
        val factory = DefaultHttpClientFactory
        val client =
            factory.create(
                SdkPaymentConfig(baseUrl = "https://example.test", sslPinning = null),
                engineOverride = TestEngines.mockEngine("ok"),
            )

        // Plugin accessor throws if plugin is not installed.
        client.plugin(ContentNegotiation)
    }

    @Test fun `created client can perform a GET through the mock engine`() =
        runTest {
            val factory = DefaultHttpClientFactory
            val client =
                factory.create(
                    SdkPaymentConfig(baseUrl = "https://example.test"),
                    engineOverride = TestEngines.mockEngine("hello"),
                )

            val response = client.get("https://example.test/ping")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe "hello"
        }
}

private object TestEngines {
    fun mockEngine(body: String): io.ktor.client.engine.mock.MockEngine =
        io.ktor.client.engine.mock.MockEngine { _ ->
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "text/plain"),
            )
        }
}
