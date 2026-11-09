package net.payrdr.mobile.payment.sdk.payment.keyprovider.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ActiveKeyDto(
    val keyValue: String,
    val protocolVersion: String,
    val keyExpiration: Long,
)
