package net.payrdr.mobile.payment.sdk.qa.config

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class StrictJsonTest {
    private fun problem(decoded: Decoded<*>): Problem = (decoded as Decoded.Invalid).problem

    @Test
    fun `json values are accepted as rfc 8259 spells them`() {
        listOf(
            "{}",
            " \t\r\n{}\r\n ",
            "{\"a\":[1,-2.5e+3,true,false,null,\"x\\u00e9\\ud83d\\ude00\\n\"],\"b\":{}}",
            "[-0,0,0.5,10,1e2,1E2,1e+2,1e-2,-0.0e0,123456789012345678901234567890]",
            "\"\\\"\\\\\\/\\b\\f\\n\\r\\t\\u0000\\uABCD\\uabcd\\uDBFF\\uDFFF\"",
            "\"\u007F\u2028\uFEFF\uD83D\uDE00é\"",
            "{\"\":{\"a\" : [ ] , \"b\" : { } }}",
            "1",
            "\"x\"",
            "null",
        ).forEach { isStrictJson(it) shouldBe true }
    }

    @Test
    fun `numbers that are not rfc 8259 numbers are rejected`() {
        listOf("01", "-01", "+1", "1.", ".5", "1e", "1e+", "-", "--1", "0x10", "1_000", "NaN", "Infinity", "-Infinity", "١", "１")
            .forEach { isStrictJson("{\"a\":$it}") shouldBe false }
    }

    @Test
    fun `bare words and misspelt literals are rejected`() {
        listOf("abc", "tru", "nul", "True", "NULL", "true1", "'a'", "undefined")
            .forEach { isStrictJson("{\"a\":$it}") shouldBe false }
    }

    @Test
    fun `a raw control character inside a string is rejected`() {
        listOf('\u0000', '\t', '\n', '\r', '\u001F').forEach { isStrictJson("{\"a\":\"x${it}y\"}") shouldBe false }
    }

    @Test
    fun `an escape that is not one of json's is rejected`() {
        listOf("\\x", "\\a", "\\'", "\\u12", "\\u12G4", "\\U0041", "\\u١٢٣٤", "\\")
            .forEach { isStrictJson("{\"a\":\"$it\"}") shouldBe false }
    }

    // RFC 8259 lets the grammar through and leaves the outcome to the parser; iOS refuses, and the string could not be
    // written back as UTF-8 anyway.
    @Test
    fun `a surrogate escape outside a pair is rejected`() {
        listOf("\\ud800", "\\udbff", "\\udc00", "\\udfff", "\\ud800x", "\\ud800\\u0041", "\\ud800\\ud800", "\\ude00\\ud83d")
            .forEach { isStrictJson("{\"a\":\"$it\"}") shouldBe false }
    }

    // Android text can hold a lone surrogate (an intent extra is a Java string); iOS text cannot.
    @Test
    fun `a raw lone surrogate is rejected`() {
        listOf("\uD800", "\uDC00", "\uD800x", "\uDE00\uD83D").forEach { isStrictJson("{\"a\":\"$it\"}") shouldBe false }
        isStrictJson("{\"a\":1}\uD800") shouldBe false
    }

    @Test
    fun `structure that is not json is rejected`() {
        listOf(
            "",
            " ",
            "{",
            "}",
            "{\"a\":1",
            "{\"a\":\"x",
            "[1,]",
            "{\"a\":1,}",
            "[,1]",
            "{,}",
            "{\"a\" 1}",
            "{\"a\":1 2}",
            "{\"a\":1 \"b\":2}",
            "{a:1}",
            "{'a':1}",
            "{1:1}",
            "{\"a\"}",
            "{\"a\":}",
            "[1}",
            "{\"a\":1]",
            "{\"a\":1}// note",
            "{} x",
            "{}{}",
            "{}]",
        ).forEach { isStrictJson(it) shouldBe false }
    }

    @Test
    fun `only space, tab, line feed and carriage return surround the value`() {
        listOf("\uFEFF{}", "\u00A0{}", "{}\u00A0", "\u000B{}", "\u000C{}", "\u0085{}", "\u2028{}", "\u3000{}")
            .forEach { isStrictJson(it) shouldBe false }
    }

    @Test
    fun `a very deep text is checked without recursion`() {
        isStrictJson("[".repeat(100_000) + "]".repeat(100_000)) shouldBe true
        isStrictJson("{\"a\":".repeat(100_000) + "{}" + "}".repeat(100_000)) shouldBe true
        isStrictJson("[".repeat(100_000)) shouldBe false
    }

    @Test
    fun `an amount that is not a json number is rejected at the root`() {
        listOf("01", "+1", "1.", ".5", "١٠٠", "1_000", "NaN", "abc").forEach {
            problem(QaConfigJson.decode("{\"order\":{\"amount\":$it}}")) shouldBe NOT_AN_OBJECT
        }
    }

    @Test
    fun `json number forms are still read as amounts`() {
        listOf("100", "100.0", "1e2", "1E+2", "10000e-2").forEach {
            (QaConfigJson.decode("{\"order\":{\"amount\":$it}}") as Decoded.Ok).value.order.amount shouldBe 100
        }
        (QaConfigJson.decode("{\"order\":{\"amount\":-0}}") as Decoded.Ok).value.order.amount shouldBe 0
    }

    @Test
    fun `a profile that is not json is rejected at the root`() {
        val text = "{\"name\":\"sandbox\",\"gatewayUrl\":\"https://dev.bpcbt.com/pay\tment\"}"
        problem(StandProfileJson.decode(text)) shouldBe NOT_AN_OBJECT
        val loneSurrogate = "{\"name\":\"sandbox\",\"gatewayUrl\":\"https://dev.bpcbt.com/payment\",\"login\":\"\\ud800\"}"
        problem(StandProfileJson.decode(loneSurrogate)) shouldBe NOT_AN_OBJECT
    }

    @Test
    fun `extra text that is not json names the extra field`() {
        listOf("{\"a\":abc}", "{\"a\":01}", "{\"a\":\"\\ud800\"}", "{\"a\":1} x").forEach {
            problem(extraFromText(it)) shouldBe Problem("order.extra", "must be a JSON object")
        }
    }

    @Test
    fun `a duplicate key keeps its first place and its last value`() {
        val extra = (extraFromText("{\"a\":1,\"b\":2,\"a\":3}") as Decoded.Ok).value
        extra.keys.toList() shouldBe listOf("a", "b")
        extra["a"] shouldBe JsonPrimitive(3)
        val config = (QaConfigJson.decode("{\"order\":{\"extra\":{\"a\":1,\"b\":2,\"a\":3}}}") as Decoded.Ok).value
        config.order.extra
            .toList() shouldBe listOf("a" to JsonPrimitive(3), "b" to JsonPrimitive(2))
        val later = (QaConfigJson.decode("{\"order\":{\"amount\":1,\"extra\":{\"a\":1}},\"order\":{\"amount\":5}}") as Decoded.Ok).value
        later.order.amount shouldBe 5
        later.order.extra shouldBe JsonObject(emptyMap())
    }
}
