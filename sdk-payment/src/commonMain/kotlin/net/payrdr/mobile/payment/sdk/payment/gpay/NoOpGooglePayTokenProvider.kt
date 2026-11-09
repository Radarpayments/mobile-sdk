package net.payrdr.mobile.payment.sdk.payment.gpay

import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig

/** Default / placeholder provider. Tapping Google Pay while this is wired → `NotAvailable`. */
internal object NoOpGooglePayTokenProvider : GooglePayTokenProvider {
    override suspend fun requestPaymentToken(
        config: GooglePayConfig,
        amountMinorUnits: Long,
        currencyCode: String,
        countryCode: String,
    ): GooglePayTokenResult = GooglePayTokenResult.NotAvailable
}
