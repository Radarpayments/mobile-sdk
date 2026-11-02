package net.payrdr.mobile.payment.sdk.core.crypto

import net.payrdr.mobile.payment.sdk.core.model.Key

/**
 * Encrypts the payment-string into a token.
 */
public interface CryptogramCipher {
    public fun encode(
        data: String,
        key: Key,
    ): String
}
