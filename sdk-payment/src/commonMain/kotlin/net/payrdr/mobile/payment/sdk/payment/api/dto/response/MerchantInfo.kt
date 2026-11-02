package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

/**
 * The merchant of the order, as `getSessionStatus.do` describes it.
 *
 * @property merchantLogin the merchant's login at the gateway; Google Pay's `gatewayMerchantId`
 * @property merchantCountry the merchant's country; the country of a wallet payment
 */
@Serializable
public data class MerchantInfo(
    val merchantLogin: String? = null,
    val merchantFullName: String? = null,
    val merchantCountry: MerchantCountry? = null,
)

/** A country as the gateway sends it: ISO 3166-1 numeric ([countryCode]) and alpha-2 ([countryCodeAlpha2]). */
@Serializable
public data class MerchantCountry(
    val countryCode: String? = null,
    val countryCodeAlpha2: String? = null,
)
