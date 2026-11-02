package net.payrdr.mobile.payment.sdk.core.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StringExtensionsTest {
    @Test
    fun `pemKeyContent strips PEM headers and whitespace`() {
        val pem =
            """
            -----BEGIN PUBLIC KEY-----
            ABCDEF
            GHIJKL
            -----END PUBLIC KEY-----
            """.trimIndent()
        pem.pemKeyContent() shouldBe "ABCDEFGHIJKL"
    }

    @Test
    fun `digitsOnly removes non-digits`() {
        "4532 8967 0143 9077".digitsOnly() shouldBe "4532896701439077"
    }

    @Test
    fun `digitsOnly truncates to maxLength`() {
        "4532896701439077".digitsOnly(maxLength = 4) shouldBe "4532"
    }

    @Test
    fun `noSpaces removes whitespace`() {
        "4532 8967 0143 9077".noSpaces() shouldBe "4532896701439077"
    }

    @Test
    fun `toExpDate parses MM YY`() {
        val exp = "12/28".toExpDate()
        exp.expMonth shouldBe 12
        exp.expYear shouldBe 2028
    }

    @Test
    fun `toExpDate rejects wrong format`() {
        shouldThrow<IllegalArgumentException> { "1228".toExpDate() }
        shouldThrow<IllegalArgumentException> { "12-28".toExpDate() }
    }
}
