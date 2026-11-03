package net.payrdr.mobile.payment.sdk.payment.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.cinterop.convert
import kotlinx.serialization.json.Json
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig

/**
 * iOS actual. When [SdkPaymentConfig.sslPinning] is non-null, installs a Darwin engine whose
 * `NSURLSessionConfiguration` carries a pinning-aware challenge handler.
 */
internal object DarwinHttpClientFactory : HttpClientFactory {
    override fun create(
        config: SdkPaymentConfig,
        engineOverride: HttpClientEngine?,
    ): HttpClient {
        if (config.sslPinning == null && engineOverride == null) {
            return DefaultHttpClientFactory.create(config, engineOverride)
        }

        val json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
                explicitNulls = false
            }

        return HttpClient(engineOverride ?: Darwin.create { configureDarwin(config) }) {
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
    }

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    private fun io.ktor.client.engine.darwin.DarwinClientEngineConfig.configureDarwin(config: SdkPaymentConfig) {
        val ssl = config.sslPinning ?: return
        val delegate = DarwinPinningDelegate(ssl)
        handleChallenge { _, _, challenge, completionHandler ->
            val (disposition, credential) = delegate.handle(challenge)
            completionHandler(disposition.convert(), credential)
        }
    }
}

internal actual fun defaultHttpClientFactory(): HttpClientFactory = DarwinHttpClientFactory
