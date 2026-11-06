package net.payrdr.mobile.payment.sdk.payment.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.ssl.SslPinningConfig
import okhttp3.CertificatePinner

/**
 * Android actual — when [SdkPaymentConfig.sslPinning] is non-null, installs the OkHttp engine
 * with a [CertificatePinner] configured from the pin entries. When null, falls back to
 * [DefaultHttpClientFactory].
 */
internal object OkHttpClientFactory : HttpClientFactory {
    override fun create(
        config: SdkPaymentConfig,
        engineOverride: HttpClientEngine?,
    ): HttpClient {
        if (config.sslPinning == null && engineOverride == null) {
            return DefaultHttpClientFactory.create(config, engineOverride)
        }

        val pinner = buildCertificatePinner(config.sslPinning)
        return HttpClient(engineOverride ?: OkHttp.create { configureEngine(pinner) }) { gatewayDefaults() }
    }

    private fun io.ktor.client.engine.okhttp.OkHttpConfig.configureEngine(pinner: CertificatePinner?) {
        if (pinner != null) {
            config {
                certificatePinner(pinner)
            }
        }
    }

    internal fun buildCertificatePinner(ssl: SslPinningConfig?): CertificatePinner? {
        if (ssl == null || ssl.entries.isEmpty()) return null
        val b = CertificatePinner.Builder()
        ssl.entries.forEach { entry ->
            b.add(entry.hostname, entry.pinString)
        }
        return b.build()
    }
}

/** Public accessor — returns [OkHttpClientFactory] on Android. */
internal actual fun defaultHttpClientFactory(): HttpClientFactory = OkHttpClientFactory
