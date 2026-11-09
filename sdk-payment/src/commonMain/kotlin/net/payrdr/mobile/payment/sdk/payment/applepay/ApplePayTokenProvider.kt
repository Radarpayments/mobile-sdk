package net.payrdr.mobile.payment.sdk.payment.applepay

import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig

/**
 * Abstracts Apple Pay sheet presentation. Implemented on iOS via [PKPaymentAuthorizationController];
 * on Android this provider always returns [ApplePayTokenResult.NotAvailable].
 *
 * Implemented in Swift: SKIE names a `suspend` function of a Kotlin interface with a `__` prefix and a completion
 * handler there (e.g. `__performChallenge(challenge:completionHandler:)`).
 */
internal interface ApplePayTokenProvider {
    /**
     * @param config merchant config — supplies merchant-id, supported networks.
     * @param amountMinorUnits the amount to charge, in minor currency units.
     * @param currencyCode ISO 4217 alpha-3: the order's currency.
     * @param countryCode ISO 3166-1 alpha-2; the merchant's country unless [ApplePayConfig.countryCode] overrides it.
     * @param merchantDisplayName human-readable name used as the first summary-item label.
     */
    suspend fun requestPaymentToken(
        config: ApplePayConfig,
        amountMinorUnits: Long,
        currencyCode: String,
        countryCode: String,
        merchantDisplayName: String,
    ): ApplePayTokenResult
}

/** Outcome of an Apple Pay sheet presentation. */
internal sealed interface ApplePayTokenResult {
    /** Encrypted payment token — a base64-encoded JSON blob from `PKPayment.token.paymentData`. */
    data class Success(
        val paymentToken: String,
    ) : ApplePayTokenResult

    /** User dismissed the Apple Pay sheet without authorizing. */
    data object Cancelled : ApplePayTokenResult

    /** Apple Pay is not available on this device (non-iOS platform or device without provisioning). */
    data object NotAvailable : ApplePayTokenResult

    /** Apple Pay sheet presentation or authorization failed; [reason] describes the cause. */
    data class Failure(
        val reason: String,
        val cause: Throwable? = null,
    ) : ApplePayTokenResult
}
