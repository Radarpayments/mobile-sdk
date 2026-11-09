package net.payrdr.mobile.payment.sdk.payment.http

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import net.payrdr.mobile.payment.sdk.payment.ssl.SslPinningConfig
import net.payrdr.mobile.payment.sdk.payment.ssl.sha256
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFRelease
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust
import platform.Security.SecCertificateCopyKey
import platform.Security.SecCertificateRef
import platform.Security.SecKeyCopyExternalRepresentation
import platform.Security.SecTrustEvaluateWithError
import platform.Security.SecTrustGetCertificateAtIndex
import platform.Security.SecTrustGetCertificateCount
import platform.Security.SecTrustRef
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Darwin TLS-challenge handler. On each TLS handshake: Apple's chain validation runs first;
 * then, for every certificate of the chain, we extract its public-key bytes (SecKey external representation),
 * prepend the appropriate ASN.1 SubjectPublicKeyInfo header to reconstruct full SPKI DER, SHA-256 hash,
 * base64-encode, and compare to the host's pins. Matches `"sha256/<base64>"` pin strings
 * (same format as OkHttp CertificatePinner accepts on Android and the documented openssl recipe).
 *
 * SPKI reconstruction is required because `SecKeyCopyExternalRepresentation` returns the bare
 * key bytes (PKCS#1 RSAPublicKey for RSA, uncompressed `0x04||X||Y` point for EC), not the full
 * SPKI DER that OkHttp / openssl hash. ASN.1 prefixes are detected from the raw key byte shape
 * (RSA: SEQUENCE marker `0x30 0x82` + modulus length range; EC: `0x04` marker + fixed length).
 * See TrustKit `TSKSPKIHashCache.m` for the canonical prefix table.
 */
@OptIn(ExperimentalForeignApi::class)
internal class DarwinPinningDelegate(
    private val config: SslPinningConfig,
) {
    fun handle(challenge: NSURLAuthenticationChallenge): Pair<NSURLSessionAuthChallengeDisposition, NSURLCredential?> {
        val protectionSpace = challenge.protectionSpace
        if (protectionSpace.authenticationMethod != NSURLAuthenticationMethodServerTrust) {
            return NSURLSessionAuthChallengePerformDefaultHandling to null
        }
        return decide(protectionSpace.host, protectionSpace.serverTrust)
    }

    /** What to answer a server-trust challenge from [host] presenting [serverTrust]. */
    internal fun decide(
        host: String,
        serverTrust: SecTrustRef?,
    ): Pair<NSURLSessionAuthChallengeDisposition, NSURLCredential?> {
        // If no pins configured for this host, fall through to default validation
        // — aligns with OkHttp's CertificatePinner semantics (only pinned hosts are enforced).
        val pins = config.pinsFor(host)
        if (pins.isEmpty()) return NSURLSessionAuthChallengePerformDefaultHandling to null

        val trust: SecTrustRef = serverTrust ?: return defaultReject()
        // 1. Let Apple run its standard chain validation first.
        if (!SecTrustEvaluateWithError(trust, null)) return defaultReject()

        val chain = (0 until SecTrustGetCertificateCount(trust).toInt()).mapNotNull { SecTrustGetCertificateAtIndex(trust, it.convert()) }
        return if (hasPinnedKey(chain, pins)) {
            NSURLSessionAuthChallengeUseCredential to NSURLCredential.credentialForTrust(trust)
        } else {
            defaultReject()
        }
    }

    /**
     * 2. As OkHttp does, the chain passes when any of its certificates — leaf, intermediate or root — has a
     *    pinned key, so a pin on the issuing CA survives the leaf's renewal.
     */
    internal fun hasPinnedKey(
        chain: List<SecCertificateRef>,
        pins: Set<String>,
    ): Boolean = chain.any { spkiPin(it) in pins }

    /** `"sha256/<base64>"` of [cert]'s SubjectPublicKeyInfo, or null for a key this delegate cannot frame. */
    internal fun spkiPin(cert: SecCertificateRef): String? {
        val key = SecCertificateCopyKey(cert) ?: return null
        val keyData = SecKeyCopyExternalRepresentation(key, null)
        CFRelease(key)
        if (keyData == null) return null

        // Raw key bytes (RSA PKCS#1 or EC uncompressed point); `prefix || keyBytes` reproduces the SubjectPublicKeyInfo
        // DER that OkHttp CertificatePinner and the documented `openssl ... | openssl dgst -sha256` recipe hash.
        val publicKeyBytes = keyData.toKotlinByteArray() // consumes +1 ref, releases
        val asn1Prefix = asn1SpkiPrefixFromBytes(publicKeyBytes) ?: return null

        @OptIn(ExperimentalEncodingApi::class)
        val b64 = Base64.encode(sha256(asn1Prefix + publicKeyBytes))
        return "sha256/$b64"
    }

    private fun defaultReject(): Pair<NSURLSessionAuthChallengeDisposition, NSURLCredential?> =
        NSURLSessionAuthChallengeCancelAuthenticationChallenge to null

    /**
     * Detect algorithm + size from the raw public-key bytes emitted by
     * `SecKeyCopyExternalRepresentation` and return the matching ASN.1 SPKI prefix.
     *
     * RSA keys are PKCS#1 `RSAPublicKey` DER — outer SEQUENCE long-form `0x30 0x82 <len_hi> <len_lo>`;
     * size is inferred from total byte length (RSA-2048 ≈ 270 bytes, 3072 ≈ 398, 4096 ≈ 526).
     * EC keys are an uncompressed point `0x04 || X || Y` — 65 bytes for P-256, 97 bytes for P-384.
     *
     * Returns null for unsupported keys; merchants must pin a cert with RSA-2048/3072/4096 or EC-P256/P384.
     */
    private fun asn1SpkiPrefixFromBytes(keyBytes: ByteArray): ByteArray? {
        if (keyBytes.isEmpty()) return null
        return when {
            keyBytes[0] == 0x30.toByte() && keyBytes.size >= 2 && keyBytes[1] == 0x82.toByte() -> {
                // RSA PKCS#1 RSAPublicKey — map by byte length to 2048/3072/4096.
                when (keyBytes.size) {
                    in 260..280 -> RSA_2048_PREFIX
                    in 390..410 -> RSA_3072_PREFIX
                    in 515..535 -> RSA_4096_PREFIX
                    else -> null
                }
            }
            keyBytes[0] == 0x04.toByte() && keyBytes.size == 65 -> EC_P256_PREFIX
            keyBytes[0] == 0x04.toByte() && keyBytes.size == 97 -> EC_P384_PREFIX
            else -> null
        }
    }

    private companion object {
        // TrustKit TSKSPKIHashCache.m — ASN.1 SubjectPublicKeyInfo headers.
        // Each header is the outer `SEQUENCE { AlgorithmIdentifier, BIT STRING { ... } }` framing
        // minus the payload bytes; concatenated with SecKey's bare public-key output to reconstruct
        // full SPKI DER that OkHttp / openssl hash.
        private val RSA_2048_PREFIX =
            byteArrayOf(
                0x30,
                0x82.toByte(),
                0x01,
                0x22,
                0x30,
                0x0d,
                0x06,
                0x09,
                0x2a,
                0x86.toByte(),
                0x48,
                0x86.toByte(),
                0xf7.toByte(),
                0x0d,
                0x01,
                0x01,
                0x01,
                0x05,
                0x00,
                0x03,
                0x82.toByte(),
                0x01,
                0x0f,
                0x00,
            )
        private val RSA_3072_PREFIX =
            byteArrayOf(
                0x30,
                0x82.toByte(),
                0x01,
                0xa2.toByte(),
                0x30,
                0x0d,
                0x06,
                0x09,
                0x2a,
                0x86.toByte(),
                0x48,
                0x86.toByte(),
                0xf7.toByte(),
                0x0d,
                0x01,
                0x01,
                0x01,
                0x05,
                0x00,
                0x03,
                0x82.toByte(),
                0x01,
                0x8f.toByte(),
                0x00,
            )
        private val RSA_4096_PREFIX =
            byteArrayOf(
                0x30,
                0x82.toByte(),
                0x02,
                0x22,
                0x30,
                0x0d,
                0x06,
                0x09,
                0x2a,
                0x86.toByte(),
                0x48,
                0x86.toByte(),
                0xf7.toByte(),
                0x0d,
                0x01,
                0x01,
                0x01,
                0x05,
                0x00,
                0x03,
                0x82.toByte(),
                0x02,
                0x0f,
                0x00,
            )
        private val EC_P256_PREFIX =
            byteArrayOf(
                0x30,
                0x59,
                0x30,
                0x13,
                0x06,
                0x07,
                0x2a,
                0x86.toByte(),
                0x48,
                0xce.toByte(),
                0x3d,
                0x02,
                0x01,
                0x06,
                0x08,
                0x2a,
                0x86.toByte(),
                0x48,
                0xce.toByte(),
                0x3d,
                0x03,
                0x01,
                0x07,
                0x03,
                0x42,
                0x00,
            )
        private val EC_P384_PREFIX =
            byteArrayOf(
                0x30,
                0x76,
                0x30,
                0x10,
                0x06,
                0x07,
                0x2a,
                0x86.toByte(),
                0x48,
                0xce.toByte(),
                0x3d,
                0x02,
                0x01,
                0x06,
                0x05,
                0x2b,
                0x81.toByte(),
                0x04,
                0x00,
                0x22,
                0x03,
                0x62,
                0x00,
            )
    }
}

/**
 * Convert a `+1 CFDataRef` (owner of a retain) into a Kotlin [ByteArray], releasing the CF ref.
 * Caller transfers ownership on call; must NOT touch the ref after this returns.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun CFDataRef.toKotlinByteArray(): ByteArray =
    try {
        val length = CFDataGetLength(this).toInt()
        val bytes = ByteArray(length)
        if (length > 0) {
            val ptr = checkNotNull(CFDataGetBytePtr(this)) { "CFData of $length bytes has no storage" }.reinterpret<ByteVar>()
            for (i in 0 until length) {
                bytes[i] = ptr[i]
            }
        }
        bytes
    } finally {
        CFRelease(this)
    }
