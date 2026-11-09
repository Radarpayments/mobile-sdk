package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

/**
 * Response from `applepay/paymentOrder.do`. Mirrors `ProcessFormGPayResponse` structure — Apple Pay
 * gateway may also return a 3DS challenge via nested [GPayData] (naming is historical; the struct
 * is shared between GPay and ApplePay backends).
 */
@Serializable
internal data class ApplePayProcessFormResponse(
    @Serializable(with = WalletSuccessSerializer::class)
    val success: Boolean,
    val data: GPayData? = null,
    val error: WalletError? = null,
)
