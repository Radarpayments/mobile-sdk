package net.payrdr.mobile.payment.sdk.core.model

/**
 * Public key used to encrypt the payment cryptogram.
 *
 * @property value PEM-encoded RSA public key
 * @property protocol must be `"RSA"` — other algorithms rejected at cipher time
 * @property expiration epoch-ms expiration timestamp; `Long.MAX_VALUE` means never expires
 */
public data class Key(
    val value: String,
    val protocol: String,
    val expiration: Long,
)
