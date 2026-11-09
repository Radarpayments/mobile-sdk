package net.payrdr.mobile.payment.sdk.core.model

import net.payrdr.mobile.payment.sdk.core.InternalPayrdrApi

/**
 * Public key used to encrypt the payment cryptogram.
 *
 * @property value PEM-encoded RSA public key
 * @property protocol must be `"RSA"` — other algorithms rejected at cipher time
 * @property expiration epoch-ms expiration timestamp; `Long.MAX_VALUE` means never expires
 */
@InternalPayrdrApi
public data class Key(
    public val value: String,
    public val protocol: String,
    public val expiration: Long,
)
