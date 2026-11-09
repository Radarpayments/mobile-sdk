package net.payrdr.mobile.payment.sdk.payment.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.convert
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

        return HttpClient(engineOverride ?: Darwin.create { configureDarwin(config) }) { gatewayDefaults() }
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
