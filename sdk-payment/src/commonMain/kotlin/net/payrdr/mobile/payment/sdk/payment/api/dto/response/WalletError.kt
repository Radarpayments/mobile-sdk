package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

/** Why the gateway refused a wallet payment (`"success": false`); [displayErrorMessage] is meant for the buyer. */
@Serializable
internal data class WalletError(
    val code: String? = null,
    val description: String? = null,
    val message: String? = null,
    val displayErrorMessage: String? = null,
)
