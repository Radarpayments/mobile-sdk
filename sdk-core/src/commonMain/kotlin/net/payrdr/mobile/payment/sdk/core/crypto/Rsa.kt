package net.payrdr.mobile.payment.sdk.core.crypto

/**
 * Platform-specific RSA/PKCS1 encryptor.
 *
 * Android actual: `javax.crypto.Cipher.getInstance("RSA/NONE/PKCS1Padding")` (Android Keystore BC provider).
 * iOS actual: `SecKeyCreateEncryptedData` with algorithm `.rsaEncryptionPKCS1`.
 */
public expect class Rsa() {
    /**
     * Encrypts [data] with PKCS#1 v1.5 padding under the RSA public key given as DER-encoded
     * SubjectPublicKeyInfo bytes.
     *
     * @throws IllegalArgumentException if the key bytes cannot be parsed
     * @throws IllegalStateException    if the encryption operation fails
     */
    public fun encryptPkcs1(
        data: ByteArray,
        publicKeyDer: ByteArray,
    ): ByteArray
}
