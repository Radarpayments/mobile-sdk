package net.payrdr.mobile.payment.sdk.payment.support

import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayTokenResult
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig

internal class FakeApplePayTokenProvider(
    var nextResult: ApplePayTokenResult = ApplePayTokenResult.Success(paymentToken = "FAKE_APPLE_PAY_TOKEN_B64"),
) : ApplePayTokenProvider {
    val calls: MutableList<Request> = mutableListOf()

    override suspend fun requestPaymentToken(
        config: ApplePayConfig,
        amountMinorUnits: Long,
        currencyCode: String,
        countryCode: String,
        merchantDisplayName: String,
    ): ApplePayTokenResult {
        calls += Request(config, amountMinorUnits, currencyCode, countryCode, merchantDisplayName)
        return nextResult
    }

    data class Request(
        val config: ApplePayConfig,
        val amountMinorUnits: Long,
        val currencyCode: String,
        val countryCode: String,
        val merchantDisplayName: String,
    )
}
