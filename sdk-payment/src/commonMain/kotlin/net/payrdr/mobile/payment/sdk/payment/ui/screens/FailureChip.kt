package net.payrdr.mobile.payment.sdk.payment.ui.screens

import net.payrdr.mobile.payment.sdk.payment.exception.SdkDeclinedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkPaymentApiException

/**
 * The chip for [error]: only what came from the gateway — a decline in its words, which are the buyer's, or the code of
 * an error it answered with. A failure of the SDK's own (no network, a session not found) gets none: the buyer can do
 * nothing with it, the text above already says what to do, and the app gets its type in the result. The SDK's messages
 * are for developers: English, with ids and hosts in them.
 */
internal fun failureChip(
    description: String?,
    error: SdkException?,
): String? =
    when (error) {
        is SdkDeclinedException -> failureChipText(description = description, error = error.message)
        is SdkPaymentApiException -> error.errorCode?.takeIf { it.isNotBlank() } ?: error.httpStatus?.let { "HTTP $it" }
        else -> null
    }

/**
 * The gateway's decline reason for the chip under the description, or null when there is none or it only repeats
 * [description]: a bank's decline comes as both, and saying it twice reads as two different problems.
 */
internal fun failureChipText(
    description: String?,
    error: String?,
): String? {
    val code = error?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return code.takeUnless { description != null && it.equals(description.trim(), ignoreCase = true) }
}
