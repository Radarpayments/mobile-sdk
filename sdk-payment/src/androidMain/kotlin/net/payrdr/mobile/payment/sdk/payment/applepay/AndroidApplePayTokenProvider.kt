package net.payrdr.mobile.payment.sdk.payment.applepay

import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig

/** Apple Pay is iOS-only. This provider always returns [ApplePayTokenResult.NotAvailable]. */
public class AndroidApplePayTokenProvider : ApplePayTokenProvider {
    override suspend fun requestPaymentToken(
        config: ApplePayConfig,
        amountMinorUnits: Long,
        currencyCode: String,
        countryCode: String,
        merchantDisplayName: String,
    ): ApplePayTokenResult = ApplePayTokenResult.NotAvailable
}
