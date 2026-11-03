package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

@Serializable
internal data class OrderPayerData(
    val mobilePhone: String? = null,
    val email: String? = null,
)
