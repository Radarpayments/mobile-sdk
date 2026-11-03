package net.payrdr.mobile.payment.sdk.core.crypto

import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

internal actual class Rsa actual constructor() {
    actual fun encryptPkcs1(
        data: ByteArray,
        publicKeyDer: ByteArray,
    ): ByteArray {
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyDer))
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }
}
