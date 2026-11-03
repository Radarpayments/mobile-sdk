package net.payrdr.mobile.payment.sdk.payment.gpay

import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig

/**
 * Abstracts Google Pay sheet presentation. Implemented on Android via [PaymentsClient];
 * on iOS this provider always returns [GooglePayTokenResult.NotAvailable] (the button is
 * not rendered on iOS anyway).
 *
 * The provider is responsible for:
 *  1. Building a `PaymentDataRequest` JSON from [GooglePayConfig] + runtime amount/currency.
 *  2. Presenting the Google Pay sheet.
 *  3. Returning the encrypted [paymentToken] (a JWE signed by the Google Pay side).
 *
 * The `PaymentFlowController` encodes the token in base64, as the gateway reads it, and sends it to
 * `google/paymentOrder.do`, or for a transfer to `p2p/perform.do`.
 */
internal interface GooglePayTokenProvider {
    /**
     * @param config the complete config for this order — merchant id, gateway, gateway merchant id, environment,
     *   and country set (the merchant's config completed from the gateway and the order).
     * @param amountMinorUnits the amount to charge, in minor currency units (e.g. 10055 = 100.55 USD).
     * @param currencyCode ISO 4217 alpha-3 currency code: the order's.
     * @param countryCode ISO 3166-1 alpha-2 country code; the same as [GooglePayConfig.countryCode].
     */
    suspend fun requestPaymentToken(
        config: GooglePayConfig,
        amountMinorUnits: Long,
        currencyCode: String,
        countryCode: String,
    ): GooglePayTokenResult
}

/** Outcome of a Google Pay token request. */
internal sealed interface GooglePayTokenResult {
    /** User authorized payment; [paymentToken] is the encrypted JWE blob to hand to the backend. */
    data class Success(
        val paymentToken: String,
    ) : GooglePayTokenResult

    /** User dismissed the GPay sheet without completing. */
    data object Cancelled : GooglePayTokenResult

    /** Google Pay is not available on this device (no Play Services, or iOS). */
    data object NotAvailable : GooglePayTokenResult

    /** Underlying failure from Play Services or token parsing. */
    data class Failure(
        val reason: String,
        val cause: Throwable? = null,
    ) : GooglePayTokenResult
}
