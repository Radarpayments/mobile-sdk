package net.payrdr.mobile.payment.sdk.payment.support

import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayTokenResult

/**
 * Programmable fake. Default: always returns [GooglePayTokenResult.Success] with a fixed token.
 */
internal class FakeGooglePayTokenProvider(
    var nextResult: GooglePayTokenResult = GooglePayTokenResult.Success(paymentToken = "FAKE_JWE_TOKEN"),
) : GooglePayTokenProvider {
    val calls: MutableList<Request> = mutableListOf()

    override suspend fun requestPaymentToken(
        config: GooglePayConfig,
        amountMinorUnits: Long,
        currencyCode: String,
        countryCode: String,
    ): GooglePayTokenResult {
        calls += Request(config, amountMinorUnits, currencyCode, countryCode)
        return nextResult
    }

    data class Request(
        val config: GooglePayConfig,
        val amountMinorUnits: Long,
        val currencyCode: String,
        val countryCode: String,
    )
}
