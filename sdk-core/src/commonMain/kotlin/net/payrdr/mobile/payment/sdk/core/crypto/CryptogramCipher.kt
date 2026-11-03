package net.payrdr.mobile.payment.sdk.core.crypto

import net.payrdr.mobile.payment.sdk.core.model.Key

/**
 * Encrypts the payment-string into a token.
 */
internal interface CryptogramCipher {
    fun encode(
        data: String,
        key: Key,
    ): String
}
