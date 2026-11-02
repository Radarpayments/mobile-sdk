package net.payrdr.mobile.payment.sdk.payment.http

import io.ktor.client.HttpClient
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.ssl.SslPinningConfig

/**
 * HTTP clients shared by every `SdkPayment` of the process. A client depends only on the factory and the SSL pinning
 * (the gateway address goes into each request), so one per pair is enough; they live as long as the process, which
 * bounds them — a client per checkout would leak one `URLSession` per payment on iOS.
 */
internal object SharedHttpClients {
    private val lock = PlatformLock()
    private val clients = HashMap<Pair<HttpClientFactory, Set<SslPinningConfig.PinEntry>?>, HttpClient>()

    fun get(
        factory: HttpClientFactory,
        config: SdkPaymentConfig,
    ): HttpClient = lock.withLock { clients.getOrPut(factory to config.sslPinning?.entries) { factory.create(config) } }
}

/** A plain mutual-exclusion lock: the cache above is reached from whichever thread opens a checkout. */
internal expect class PlatformLock() {
    fun <T> withLock(block: () -> T): T
}
