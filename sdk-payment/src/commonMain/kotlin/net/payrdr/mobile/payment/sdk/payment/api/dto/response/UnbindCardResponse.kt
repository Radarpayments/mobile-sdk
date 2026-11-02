package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

/** Response from `unbindcardanon.do`. `errorCode == 0` means the card was detached. */
@Serializable
public data class UnbindCardResponse(
    val errorCode: Int,
) {
    public val isSuccess: Boolean get() = errorCode == SUCCESS_CODE

    public companion object {
        public const val SUCCESS_CODE: Int = 0
    }
}
