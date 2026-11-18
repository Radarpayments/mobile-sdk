package net.payrdr.mobile.payment.sdk.qa.config

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class DocumentLimitsTest {
    // The root object is level 1, so depth n is n objects nested in each other.
    private fun nested(depth: Int): String = "{\"a\":".repeat(depth - 1) + "{}" + "}".repeat(depth - 1)

    // 6 bytes of `{"a":"`, the filler, 2 bytes of `"}`; 'é' is two bytes of UTF-8 and one char.
    private fun sized(bytes: Int): String {
        val filler = bytes - 8
        return "{\"a\":\"" + "é".repeat(filler / 2) + "x".repeat(filler % 2) + "\"}"
    }

    private fun problem(decoded: Decoded<*>): Problem = (decoded as Decoded.Invalid).problem

    @Test
    fun `a document 32 levels deep is read`() {
        QaConfigJson.decode(nested(32)) shouldBe Decoded.Ok(QaConfig())
        QaConfigJson.decode("{\"a\":" + "[".repeat(30) + "{}" + "]".repeat(30) + "}") shouldBe
            Decoded.Ok(QaConfig())
    }

    @Test
    fun `a document 33 levels deep is rejected at the root`() {
        val problem = problem(QaConfigJson.decode(nested(33)))
        problem.field shouldBe "$"
        problem.message shouldContain "32"
        problem(QaConfigJson.decode("{\"a\":" + "[".repeat(32) + "1" + "]".repeat(32) + "}")).field shouldBe "$"
    }

    @Test
    fun `a very deep document is rejected without overflowing the stack`() {
        problem(QaConfigJson.decode(nested(100_000))).field shouldBe "$"
        problem(QaConfigJson.decode("[".repeat(100_000))).field shouldBe "$"
        problem(StandProfileJson.decode(nested(100_000))).field shouldBe "$"
    }

    @Test
    fun `brackets inside strings do not count`() {
        QaConfigJson.decode("{\"a\":\"" + "[{".repeat(100) + "\"}") shouldBe Decoded.Ok(QaConfig())
        QaConfigJson.decode("{\"a\":\"\\\"" + "[{".repeat(100) + "\"}") shouldBe Decoded.Ok(QaConfig())
    }

    @Test
    fun `an escaped backslash ends before the quote, so the brackets after the string count`() {
        val text = "{\"a\":\"\\\\\",\"b\":" + nested(32) + "}"
        problem(QaConfigJson.decode(text)).field shouldBe "$"
    }

    @Test
    fun `a document of 65536 bytes is read and one of 65537 is rejected`() {
        sized(65536).length shouldBe 32772
        QaConfigJson.decode(sized(65536)) shouldBe Decoded.Ok(QaConfig())
        val problem = problem(QaConfigJson.decode(sized(65537)))
        problem.field shouldBe "$"
        problem.message shouldContain "65536"
        problem(StandProfileJson.decode(sized(65537))).field shouldBe "$"
    }

    @Test
    fun `a limit problem does not echo the text`() {
        problem(QaConfigJson.decode(sized(65537))).message.contains("é") shouldBe false
    }

    @Test
    fun `the form's extra counts its depth from its place in the document`() {
        // `extra` is level 3 of a config: the root, `order`, then `extra`.
        val document = QaConfigJson.decode("{\"order\":{\"extra\":${nested(30)}}}") as Decoded.Ok
        extraFromText(nested(30)) shouldBe Decoded.Ok(document.value.order.extra)
        QaConfigJson.decode("{\"order\":{\"extra\":${nested(31)}}}").let { problem(it).field shouldBe "$" }
        val problem = (extraFromText(nested(31)) as Decoded.Invalid).problem
        problem.field shouldBe "order.extra"
        problem.message shouldContain "30"
        (extraFromText(nested(100_000)) as Decoded.Invalid).problem.field shouldBe "order.extra"
        (extraFromText(sized(65537)) as Decoded.Invalid).problem.field shouldBe "order.extra"
    }
}
