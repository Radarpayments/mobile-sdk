package net.payrdr.mobile.payment.sdk.payment.http

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.ssl.SslPinningConfig
import net.payrdr.mobile.payment.sdk.payment.ssl.TestCertificate
import net.payrdr.mobile.payment.sdk.payment.ssl.TestCertificates
import okhttp3.CertificatePinner
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLPeerUnverifiedException
import kotlin.test.Test

/** The pins of [SslPinningConfig] as OkHttp enforces them on Android, checked against the pins `openssl` computes. */
class CertificatePinningTest {
    private val chain = listOf(x509(TestCertificates.LEAF), x509(TestCertificates.CA))

    @Test
    fun `okhttp pins every supported key type as openssl does`() {
        listOf(
            TestCertificates.CA,
            TestCertificates.LEAF,
            TestCertificates.RSA_3072,
            TestCertificates.RSA_4096,
            TestCertificates.EC_P384,
        ).forEach { CertificatePinner.pin(x509(it)) shouldBe it.pin }
    }

    @Test
    fun `a chain with the pinned leaf passes`() {
        pinner(TestCertificates.LEAF).check(TestCertificates.LEAF_HOST, chain)
    }

    @Test
    fun `a pin on the issuer lets the chain pass so it survives the leaf renewal`() {
        pinner(TestCertificates.CA).check(TestCertificates.LEAF_HOST, chain)
    }

    @Test
    fun `a chain without the pinned key is refused`() {
        shouldThrow<SSLPeerUnverifiedException> { pinner(TestCertificates.RSA_4096).check(TestCertificates.LEAF_HOST, chain) }
    }

    @Test
    fun `a host without pins is not checked`() {
        pinner(TestCertificates.RSA_4096).check("other.example", chain)
    }

    @Test
    fun `a wildcard pin covers one label below the domain`() {
        val pinner = pinner(TestCertificates.RSA_4096, host = "*.pinned.example")

        shouldThrow<SSLPeerUnverifiedException> { pinner.check("api.pinned.example", chain) }
        pinner.check("pinned.example", chain)
    }

    @Test
    fun `without a pinning config there is no pinner`() {
        OkHttpClientFactory.buildCertificatePinner(null).shouldBeNull()
    }

    private fun pinner(
        certificate: TestCertificate,
        host: String = TestCertificates.LEAF_HOST,
    ): CertificatePinner {
        val config = SslPinningConfig.builder().addPin(host, certificate.pin.removePrefix("sha256/")).build()
        return checkNotNull(OkHttpClientFactory.buildCertificatePinner(config))
    }

    private fun x509(certificate: TestCertificate): X509Certificate =
        CertificateFactory.getInstance("X.509").generateCertificate(certificate.der.inputStream()) as X509Certificate
}
