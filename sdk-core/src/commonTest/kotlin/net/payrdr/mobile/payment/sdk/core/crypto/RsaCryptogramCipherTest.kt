package net.payrdr.mobile.payment.sdk.core.crypto

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotBeBlank
import net.payrdr.mobile.payment.sdk.core.model.Key
import kotlin.test.Test

class RsaCryptogramCipherTest {
    private val cipher = RsaCryptogramCipher()

    @Test
    fun `produces non-empty Base64 ciphertext under valid RSA key`() {
        val key = Key(value = TEST_PUBLIC_KEY_PEM, protocol = "RSA", expiration = Long.MAX_VALUE)
        val token = cipher.encode("hello-world", key)
        token.shouldNotBeBlank()
        token shouldMatch Regex("[A-Za-z0-9+/]+=*")
    }

    @Test
    fun `different calls produce different ciphertexts because RSA PKCS1 is randomized`() {
        val key = Key(value = TEST_PUBLIC_KEY_PEM, protocol = "RSA", expiration = Long.MAX_VALUE)
        val t1 = cipher.encode("same-input", key)
        val t2 = cipher.encode("same-input", key)
        (t1 != t2) shouldBe true
    }

    @Test
    fun `rejects non-RSA protocol`() {
        val key = Key(value = TEST_PUBLIC_KEY_PEM, protocol = "AES", expiration = Long.MAX_VALUE)
        shouldThrow<IllegalStateException> {
            cipher.encode("data", key)
        }
    }

    @Test
    fun `throws on malformed PEM key`() {
        val key =
            Key(value = "-----BEGIN PUBLIC KEY-----\nnot-valid\n-----END PUBLIC KEY-----", protocol = "RSA", expiration = Long.MAX_VALUE)
        shouldThrow<IllegalArgumentException> {
            cipher.encode("data", key)
        }
    }

    private companion object {
        // Test RSA-2048 public key — fixture only, not used in production.
        // Generated via: openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out /tmp/test_rsa.pem
        //               openssl rsa -pubout -in /tmp/test_rsa.pem -out /tmp/test_rsa_pub.pem
        const val TEST_PUBLIC_KEY_PEM = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvjG7qd50WLuBmw7rUpQo
xN44zA/XUddLxYAN22fJE9GbLMwKcT0fVE//KjAUDDy3s8p4QuNuxLfUsINM4DlV
IbQZx3ZOXUaQILYCqmqqvbibEk4ulITdMS6QjEsHXdcKXpr+1ZyVG90RI7oxyQf8
YKTxqOQF7lYqicEb6ucPJwYxLA1IoPu9K5PAysLrFiEwuq+z6dNFrvVMXSDAefry
Hc/MnjXhGLc3qcfH9sJczKQ8we6I7zIDgRcIjsZ8xI8ME+X36UJpYPvrU41Xssi1
6MCObHbZpZTEjShPnfmwTIDMbjnqwitXKaC0C+vJ+DvnL5+iqsHN+HT+z8NcQXco
xQIDAQAB
-----END PUBLIC KEY-----"""
    }
}
