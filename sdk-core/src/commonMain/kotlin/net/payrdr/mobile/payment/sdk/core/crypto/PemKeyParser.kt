package net.payrdr.mobile.payment.sdk.core.crypto

import net.payrdr.mobile.payment.sdk.core.utils.pemKeyContent
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Decodes a PEM-armored public key into raw DER-encoded SubjectPublicKeyInfo bytes.
 */
@OptIn(ExperimentalEncodingApi::class)
internal object PemKeyParser {
    fun parsePublicKeyDer(pem: String): ByteArray = Base64.decode(pem.pemKeyContent())
}
