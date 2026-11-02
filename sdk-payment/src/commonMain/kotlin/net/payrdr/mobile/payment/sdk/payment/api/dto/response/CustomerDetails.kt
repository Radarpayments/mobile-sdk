package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

/**
 * Payer contacts the merchant passed at order registration (`email`, `phone`) and calling codes the
 * gateway suggests for the payer's phone.
 */
@Serializable
public data class CustomerDetails(
    val email: String? = null,
    val phone: String? = null,
    val suggestedCountryCodes: List<SuggestedCountryCode> = emptyList(),
)

/** Calling code with its country, e.g. `+44` / `GB`. */
@Serializable
public data class SuggestedCountryCode(
    val phoneCountryCode: String? = null,
    val alpha2: String? = null,
)
