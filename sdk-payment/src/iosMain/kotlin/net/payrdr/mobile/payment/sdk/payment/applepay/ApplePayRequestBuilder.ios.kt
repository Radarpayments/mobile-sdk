package net.payrdr.mobile.payment.sdk.payment.applepay

import kotlinx.cinterop.ExperimentalForeignApi
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import platform.Foundation.NSDecimalNumber
import platform.PassKit.PKPaymentRequest
import platform.PassKit.PKPaymentSummaryItem

/**
 * Pure helper that builds a [PKPaymentRequest] from an [ApplePayConfig] + runtime parameters.
 *
 * Factored out of [IosApplePayTokenProvider] so it can be unit-tested from iosTest without
 * touching PKPaymentAuthorizationController (which requires a running UI).
 */
@OptIn(ExperimentalForeignApi::class)
public object ApplePayRequestBuilder {
    public fun build(
        config: ApplePayConfig,
        amountMinorUnits: Long,
        currencyCode: String,
        countryCode: String,
        merchantDisplayName: String,
    ): PKPaymentRequest {
        require(config.supportedNetworks.isNotEmpty()) {
            "ApplePayConfig.supportedNetworks must contain at least one network"
        }
        require(config.merchantCapabilities.isNotEmpty()) {
            "ApplePayConfig.merchantCapabilities must contain at least one capability"
        }
        require(merchantDisplayName.isNotBlank()) { "merchantDisplayName must not be blank" }
        require(amountMinorUnits >= 0L) {
            "amountMinorUnits must be non-negative. Got: $amountMinorUnits"
        }
        val effectiveCountry = config.countryCode ?: countryCode
        require(currencyCode.length == 3) {
            "currencyCode must be ISO 4217 alpha-3 (3 chars). Got: '$currencyCode'"
        }
        require(effectiveCountry.length == 2) {
            "countryCode must be ISO 3166-1 alpha-2 (2 chars). Got: '$effectiveCountry'"
        }

        val request = PKPaymentRequest()
        request.merchantIdentifier = config.merchantId
        request.currencyCode = currencyCode
        request.countryCode = effectiveCountry
        request.supportedNetworks = config.supportedNetworks.map { it.toPkPaymentNetwork() }
        request.merchantCapabilities = config.merchantCapabilities.toPkMerchantCapabilityMask()

        val majorString = majorString(amountMinorUnits)
        val item = PKPaymentSummaryItem()
        item.label = merchantDisplayName
        item.amount = NSDecimalNumber(string = majorString)
        request.paymentSummaryItems = listOf(item)

        return request
    }

    private fun majorString(minor: Long): String {
        val whole = minor / 100
        val fraction = (minor % 100).let { if (it < 0) -it else it }
        val frac = fraction.toString().padStart(2, '0')
        return "$whole.$frac"
    }
}
