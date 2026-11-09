package net.payrdr.mobile.payment.sdk.payment.utils

import net.payrdr.mobile.payment.sdk.core.utils.Base58Coder
import net.payrdr.mobile.payment.sdk.payment.exception.SdkInvalidSessionIdException

private const val OUTSIDE_BASE58_PREFIX = "ps_"
private const val INSIDE_BASE58_PREFIX = "ps:"

/**
 * Converts between V1 order numbers (`mdOrder`) and V2 session ids (`ps_...`).
 *
 * A session id is the Base58 encoding of `ps:{mdOrder}`, prefixed with `ps_`.
 */
internal object SessionIdConverter {
    fun mdOrderToSessionId(mdOrder: String): String {
        val encoded = Base58Coder.encode(INSIDE_BASE58_PREFIX + mdOrder)
        return OUTSIDE_BASE58_PREFIX + encoded.trim()
    }

    fun sessionIdToMdOrder(sessionId: String): String {
        if (sessionId.isBlank()) {
            throw SdkInvalidSessionIdException(sessionId)
        }
        if (!sessionId.startsWith(OUTSIDE_BASE58_PREFIX)) {
            throw SdkInvalidSessionIdException(sessionId)
        }
        val base58Body = sessionId.substring(OUTSIDE_BASE58_PREFIX.length)
        if (!Base58Coder.isValidBase58String(base58Body)) {
            throw SdkInvalidSessionIdException(sessionId)
        }
        val decoded = Base58Coder.decode(base58Body)
        if (!decoded.startsWith(INSIDE_BASE58_PREFIX)) {
            throw SdkInvalidSessionIdException(sessionId)
        }
        return decoded.substring(INSIDE_BASE58_PREFIX.length)
    }
}
