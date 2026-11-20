package net.payrdr.mobile.payment.sdk.qa.config

import io.kotest.matchers.shouldBe
import java.util.regex.Pattern
import kotlin.test.Test

internal class HttpUrlTest {
    @Test
    fun `whitespace is what Unicode calls White_Space on every platform`() {
        val unicodeSpace = Pattern.compile("\\s", Pattern.UNICODE_CHARACTER_CLASS)
        (0..0xFFFF).map(Int::toChar).forEach { char ->
            val spaced = unicodeSpace.matcher(char.toString()).matches()
            isHttpUrl("https://dev.bpcbt.com/a${char}b") shouldBe !spaced
        }
    }

    @Test
    fun `a host with a space ends before it`() {
        isHttpUrl("https://dev bpcbt.com") shouldBe false
        isHttpUrl("https:// dev.bpcbt.com") shouldBe false
        isHttpUrl("https://dev.bpcbt.com/payment") shouldBe true
    }
}
