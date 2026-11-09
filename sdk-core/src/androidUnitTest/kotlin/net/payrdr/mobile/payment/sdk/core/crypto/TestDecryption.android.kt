package net.payrdr.mobile.payment.sdk.core.crypto

import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
internal actual fun decryptWithTestKey(ciphertext: ByteArray): ByteArray {
    val der = Base64.decode(TestRsaKeys.PRIVATE_KEY_PKCS8_BASE64)
    val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(der))
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    cipher.init(Cipher.DECRYPT_MODE, key)
    return cipher.doFinal(ciphertext)
}
