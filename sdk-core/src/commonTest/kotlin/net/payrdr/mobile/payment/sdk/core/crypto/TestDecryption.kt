package net.payrdr.mobile.payment.sdk.core.crypto

/** Opens a token encrypted under [TestRsaKeys.PUBLIC_KEY_PEM] with the platform's own RSA, as the gateway would. */
internal expect fun decryptWithTestKey(ciphertext: ByteArray): ByteArray
