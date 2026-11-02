package net.payrdr.mobile.payment.sdk.core.utils

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class Base58CoderTest {
    @Test
    fun `encode then decode round-trips`() {
        val input = "Hello, Base58 world!"
        val encoded = Base58Coder.encode(input)
        Base58Coder.decode(encoded) shouldBe input
    }

    @Test
    fun `isValidBase58String accepts alphabet chars`() {
        Base58Coder.isValidBase58String("6FYvALoiRsRvmw") shouldBe true
    }

    @Test
    fun `isValidBase58String rejects out-of-alphabet chars`() {
        Base58Coder.isValidBase58String("0OIl") shouldBe false // excluded from Base58 alphabet
    }
}
