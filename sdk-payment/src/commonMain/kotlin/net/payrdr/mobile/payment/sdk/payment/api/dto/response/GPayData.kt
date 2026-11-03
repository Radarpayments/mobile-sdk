package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

/** The `data` of a wallet payment answer: a 3DS challenge when the bank asks for one; a refusal carries only [redirectUrl]. */
@Serializable
internal data class GPayData(
    val orderId: String? = null,
    val acsUrl: String? = null,
    val paReq: String? = null,
    val termUrl: String? = null,
    val errorTypeName: String? = null,
    val redirectUrl: String? = null,
)
