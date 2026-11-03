package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

/** Response from `unbindcardanon.do`. `errorCode == 0` means the card was detached. */
@Serializable
internal data class UnbindCardResponse(
    val errorCode: Int,
) {
    val isSuccess: Boolean get() = errorCode == SUCCESS_CODE

    companion object {
        const val SUCCESS_CODE: Int = 0
    }
}
