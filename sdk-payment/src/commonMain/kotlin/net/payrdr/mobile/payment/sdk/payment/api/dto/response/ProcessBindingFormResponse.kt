package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

/** Response from `processBindingForm.do` — a stripped `ProcessFormResponse`. */
@Serializable
public data class ProcessBindingFormResponse(
    val errorCode: Int,
)
