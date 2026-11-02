package net.payrdr.mobile.payment.sdk.payment.keyprovider

import net.payrdr.mobile.payment.sdk.core.model.Key

/**
 * Source of the active RSA public key used to seal payment cryptograms.
 *
 * Implementations are expected to be thread-safe and may cache internally.
 *
 * Implemented in Swift: SKIE names a `suspend` function of a Kotlin interface with a `__` prefix and a completion
 * handler there (e.g. `__performChallenge(challenge:completionHandler:)`).
 */
public interface KeyProvider {
    /**
     * Returns the currently-active public key. Callers should not cache the result across
     * checkout sessions — keys rotate.
     *
     * @throws KeyProviderException if retrieval fails permanently
     */
    public suspend fun provideKey(): Key
}
