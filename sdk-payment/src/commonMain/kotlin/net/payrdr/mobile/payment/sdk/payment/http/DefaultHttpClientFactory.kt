package net.payrdr.mobile.payment.sdk.payment.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
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
 */
internal object DefaultHttpClientFactory : HttpClientFactory {
    override fun create(
        config: SdkPaymentConfig,
        engineOverride: HttpClientEngine?,
    ): HttpClient =
        if (engineOverride != null) {
            HttpClient(engineOverride) { gatewayDefaults() }
        } else {
            HttpClient { gatewayDefaults() }
        }
}

private val gatewayJson =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

/**
 * What every gateway client has, pinned or not: a lenient JSON codec, timeouts (connect 15 s, request and socket
 * 60 s) and `Accept: application/json`.
 */
internal fun HttpClientConfig<*>.gatewayDefaults() {
    install(ContentNegotiation) { json(gatewayJson) }
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
