package net.payrdr.mobile.payment.sdk.core.crypto

import net.payrdr.mobile.payment.sdk.core.model.Key
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * [CryptogramCipher] implementation that encrypts the payment string with RSA/PKCS1 and
 * returns a Base64-encoded token. Matches the wire format used by Radar PAYRDR SDK 3.x.
 */
@OptIn(ExperimentalEncodingApi::class)
public class RsaCryptogramCipher(
    private val rsa: Rsa = Rsa(),
) : CryptogramCipher {
    override fun encode(
        data: String,
        key: Key,
    ): String {
        check(key.protocol == "RSA") { "Only RSA keys are supported, got ${key.protocol}" }
        val publicKeyDer = PemKeyParser.parsePublicKeyDer(key.value)
        val encrypted = rsa.encryptPkcs1(data.encodeToByteArray(), publicKeyDer)
        return Base64.encode(encrypted)
    }
}
