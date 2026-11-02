package net.payrdr.mobile.payment.sdk.payment.gpay

import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig

/**
 * iOS has no Google Pay. This provider always resolves to
 * [GooglePayTokenResult.NotAvailable]. Consumers should not render the Google Pay button on
 * iOS in the first place (the Compose actual for `GooglePayButton.ios.kt` is a `Spacer`).
 */
public class IosGooglePayTokenProvider : GooglePayTokenProvider {
    override suspend fun requestPaymentToken(
        config: GooglePayConfig,
        amountMinorUnits: Long,
        currencyCode: String,
        countryCode: String,
    ): GooglePayTokenResult = GooglePayTokenResult.NotAvailable
}
