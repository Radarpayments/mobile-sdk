package net.payrdr.mobile.payment.sdk.payment.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig

/**
 * Creates preconfigured Ktor [HttpClient]s for [net.payrdr.mobile.payment.sdk.payment.api.PaymentApi] /
 * [net.payrdr.mobile.payment.sdk.payment.keyprovider.RemoteKeyProvider]. Each platform resolves a
 * different default engine (OkHttp on Android, Darwin on iOS), which makes SSL pinning viable.
 *
 * Callers who need full control over the engine can pass [engineOverride] — useful in tests.
 */
public interface HttpClientFactory {
    /**
     * Build a Ktor [HttpClient] for [config]. When [engineOverride] is supplied it replaces the
     * platform-default engine (OkHttp/Darwin) — test doubles typically pass a MockEngine here.
     */
    public fun create(
        config: SdkPaymentConfig,
        engineOverride: HttpClientEngine? = null,
    ): HttpClient
}

/** Factory resolved for the current platform. Platform source sets provide the actual. */
public expect fun defaultHttpClientFactory(): HttpClientFactory
