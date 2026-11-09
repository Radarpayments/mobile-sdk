package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

/** Response from `google/paymentOrder.do`. */
@Serializable
internal data class ProcessFormGPayResponse(
    @Serializable(with = WalletSuccessSerializer::class)
    val success: Boolean,
    val data: GPayData? = null,
    val error: WalletError? = null,
)
