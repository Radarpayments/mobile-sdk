package net.payrdr.mobile.payment.sdk.payment.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig

/**
 * Unpinned baseline. Used whenever [SdkPaymentConfig.sslPinning] is `null` and for tests that
 * supply their own engine override.
 *
 * Installs:
 *  - `ContentNegotiation` with a lenient JSON codec (matches Plan 2 defaults).
 *  - `HttpTimeout` — connect 15s, request 60s, socket 60s.
 *  - `defaultRequest { Accept: application/json }`.
 */
public object DefaultHttpClientFactory : HttpClientFactory {
    override fun create(
        config: SdkPaymentConfig,
        engineOverride: HttpClientEngine?,
    ): HttpClient {
        val json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
                explicitNulls = false
            }
        val configureBlock: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
            defaultRequest {
                if (!headers.contains(HttpHeaders.Accept)) {
                    headers.append(HttpHeaders.Accept, "application/json")
                }
            }
        }
        return if (engineOverride != null) {
            HttpClient(engineOverride, configureBlock)
        } else {
            HttpClient(configureBlock)
        }
    }
}
