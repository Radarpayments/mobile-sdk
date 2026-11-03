package net.payrdr.mobile.payment.sdk.payment.ssl

import net.payrdr.mobile.payment.sdk.core.InternalPayrdrApi

/**
 * Declarative SSL pinning configuration. Accepts pre-computed base64 SHA-256 SPKI pins.
 *
 * Normalized internal representation is a set of [PinEntry] — hostname (lowercased, trailing
 * dot stripped) paired with a `"sha256/<base64>"` pin string accepted by both OkHttp's
 * [okhttp3.CertificatePinner] and the iOS `SecTrustEvaluate` pinning delegate.
 *
 * Pre-compute pins with:
 * ```
 * openssl s_client -connect host:443 \
 *   | openssl x509 -pubkey -noout \
 *   | openssl pkey -pubin -outform DER \
 *   | openssl dgst -sha256 -binary \
 *   | openssl base64
 * ```
 *
 * Build via [SslPinningConfig.builder]:
 *
 * ```
 * val pinning = SslPinningConfig.builder()
 *     .addPin("api.payrdr.net", "sha256Base64==")
 *     .build()
 * ```
 */
public class SslPinningConfig private constructor(
    @InternalPayrdrApi public val entries: Set<PinEntry>,
) {
    /**
     * A single pinned host+pin pair. [pinString] is always `"sha256/<base64>"` — the same
     * format accepted by [okhttp3.CertificatePinner.Builder.add].
     */
    @InternalPayrdrApi
    public data class PinEntry(
        public val hostname: String,
        public val pinString: String,
    )

    /**
     * The pins enforced for [host] (any of them must match a certificate of the chain); empty when [host] is not
     * pinned. Hostnames follow OkHttp's [okhttp3.CertificatePinner], so a config acts the same on both platforms:
     * `*.example.com` covers one label below the domain, `**.example.com` the domain itself and any depth below.
     */
    internal fun pinsFor(host: String): Set<String> {
        val normalized = normalizeHost(host)
        return entries.filter { hostMatches(it.hostname, normalized) }.mapTo(linkedSetOf()) { it.pinString }
    }

    private fun hostMatches(
        pattern: String,
        host: String,
    ): Boolean =
        when {
            pattern.startsWith("**.") -> {
                val domain = pattern.removePrefix("**.")
                host == domain || host.endsWith(".$domain")
            }
            pattern.startsWith("*.") -> {
                val domain = pattern.removePrefix("*.")
                val label = host.removeSuffix(".$domain")
                host.endsWith(".$domain") && label.isNotEmpty() && '.' !in label
            }
            else -> host == pattern
        }

    /** Builder for [SslPinningConfig]. Accumulates entries; deduplicated on [build]. */
    public class Builder {
        private val collected: MutableSet<PinEntry> = linkedSetOf()

        /** Add a pre-computed SHA-256 pin (base64 of the 32-byte SPKI hash, no `sha256/` prefix). */
        public fun addPin(
            hostname: String,
            sha256Pin: String,
        ): Builder =
            apply {
                require(!sha256Pin.startsWith("sha256/")) {
                    "sha256Pin must be raw base64 (no 'sha256/' prefix). Got: $sha256Pin"
                }
                require(sha256Pin.length == 44 && sha256Pin.endsWith("=")) {
                    "sha256Pin must be 44 chars of base64 ending with '='. Got ${sha256Pin.length} chars: $sha256Pin"
                }
                val normalized = normalizeHost(hostname)
                require(normalized.isNotEmpty()) { "hostname must be non-empty after normalization. Got: '$hostname'" }
                collected += PinEntry(normalized, "sha256/$sha256Pin")
            }

        /**
         * Build the immutable [SslPinningConfig] from accumulated entries.
         * Requires at least one entry — pass `null` to [SdkPaymentConfig.sslPinning] to disable pinning.
         */
        public fun build(): SslPinningConfig {
            require(collected.isNotEmpty()) {
                "SslPinningConfig must contain at least one pin; pass null to SdkPaymentConfig.sslPinning to disable pinning."
            }
            return SslPinningConfig(collected.toSet())
        }
    }

    /** Factory + internal helpers for [SslPinningConfig]. */
    public companion object {
        /** Entry point for constructing an [SslPinningConfig] via its builder DSL. */
        public fun builder(): Builder = Builder()

        internal fun normalizeHost(raw: String): String = raw.lowercase().trimEnd('.')
    }
}

/** Platform SHA-256 bridge. JVM: `MessageDigest.getInstance("SHA-256")`. iOS: `CC_SHA256`. */
internal expect fun sha256(data: ByteArray): ByteArray
