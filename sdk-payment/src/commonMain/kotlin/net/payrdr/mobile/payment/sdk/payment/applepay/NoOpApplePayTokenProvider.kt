package net.payrdr.mobile.payment.sdk.payment.applepay

import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig

/**
 * Default [ApplePayTokenProvider] for platforms that cannot present the Apple Pay sheet.
 * Always returns [ApplePayTokenResult.NotAvailable] — used as the default on Android/common wiring.
 */
public object NoOpApplePayTokenProvider : ApplePayTokenProvider {
    override suspend fun requestPaymentToken(
        config: ApplePayConfig,
        amountMinorUnits: Long,
        currencyCode: String,
        countryCode: String,
        merchantDisplayName: String,
    ): ApplePayTokenResult = ApplePayTokenResult.NotAvailable
}
