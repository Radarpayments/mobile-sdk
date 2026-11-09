package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of `p2p/perform.do` / `p2p/performByBinding.do`; the SDK reads it as a [ProcessFormResponse]. */
@Serializable
internal data class P2PPerformResponse(
    val errorCode: Int = 0,
    val errorMessage: String? = null,
    val error: String? = null,
    val acsUrl: String? = null,
    val paReq: String? = null,
    val termUrl: String? = null,
    @SerialName("threeDSMethodURL")
    val threeDSMethodURL: String? = null,
)
