package net.payrdr.mobile.payment.sdk.payment.keyprovider.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ActiveKeysResponse(
    val keys: List<ActiveKeyDto>,
)
