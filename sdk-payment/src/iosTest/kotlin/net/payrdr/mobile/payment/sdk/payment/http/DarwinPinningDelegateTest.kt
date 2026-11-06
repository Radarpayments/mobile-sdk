@file:OptIn(ExperimentalForeignApi::class)

package net.payrdr.mobile.payment.sdk.payment.http

import io.kotest.matchers.shouldBe
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import net.payrdr.mobile.payment.sdk.payment.ssl.SslPinningConfig
import net.payrdr.mobile.payment.sdk.payment.ssl.TestCertificate
import net.payrdr.mobile.payment.sdk.payment.ssl.TestCertificates
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Security.SecCertificateCreateWithData
import platform.Security.SecCertificateRef
import kotlin.test.Test

/**
 * The pin is computed from the key bytes iOS gives out, with an ASN.1 header the SDK writes per key type; a wrong
 * header silently pins nothing. These pins are checked against the ones `openssl` computes from the certificates.
 *
 * The system's own chain check is not run here: a test process on the simulator cannot reach the trust daemon
 * (`SecTrustEvaluateWithError` fails with errSecInternal, -26276), so the chain is matched against the pins as given.
 */
class DarwinPinningDelegateTest {
    private val delegate =
        DarwinPinningDelegate(
            SslPinningConfig.builder().addPin(TestCertificates.LEAF_HOST, TestCertificates.LEAF.pin.removePrefix("sha256/")).build(),
        )
    private val chain = listOf(certificate(TestCertificates.LEAF), certificate(TestCertificates.CA))

    @Test
    fun every_supported_key_type_pins_as_openssl_does() {
        listOf(
            TestCertificates.CA,
            TestCertificates.LEAF,
            TestCertificates.RSA_3072,
            TestCertificates.RSA_4096,
            TestCertificates.EC_P384,
        ).forEach { delegate.spkiPin(certificate(it)) shouldBe it.pin }
    }

    @Test
    fun a_chain_with_the_pinned_leaf_matches() {
        delegate.hasPinnedKey(chain, setOf(TestCertificates.LEAF.pin)) shouldBe true
    }

    @Test
    fun a_pin_on_the_issuer_matches_the_chain_so_it_survives_the_leaf_renewal() {
        delegate.hasPinnedKey(chain, setOf(TestCertificates.CA.pin)) shouldBe true
    }

    @Test
    fun a_chain_without_the_pinned_key_does_not_match() {
        delegate.hasPinnedKey(chain, setOf(TestCertificates.RSA_4096.pin, TestCertificates.EC_P384.pin)) shouldBe false
    }

    @Test
    fun a_pinned_host_without_a_server_trust_is_refused() {
        delegate.decide(TestCertificates.LEAF_HOST, null).first shouldBe NSURLSessionAuthChallengeCancelAuthenticationChallenge
    }

    @Test
    fun a_host_without_pins_is_left_to_the_system_check() {
        delegate.decide("other.example", null).first shouldBe NSURLSessionAuthChallengePerformDefaultHandling
    }

    private fun certificate(certificate: TestCertificate): SecCertificateRef {
        val der = certificate.der
        val data = der.usePinned { CFDataCreate(kCFAllocatorDefault, it.addressOf(0).reinterpret(), der.size.convert()) }
        return checkNotNull(SecCertificateCreateWithData(kCFAllocatorDefault, data))
    }
}
