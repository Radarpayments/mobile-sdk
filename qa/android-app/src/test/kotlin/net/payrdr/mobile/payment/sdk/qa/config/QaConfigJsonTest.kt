package net.payrdr.mobile.payment.sdk.qa.config

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class QaConfigJsonTest {
    private fun field(text: String): String? = (QaConfigJson.decode(text) as? Decoded.Invalid)?.problem?.field

    private fun ok(text: String): QaConfig = (QaConfigJson.decode(text) as Decoded.Ok).value

    @Test
    fun `the default config is what an empty object decodes to`() {
        ok("{}") shouldBe QaConfig()
    }

    @Test
    fun `the default config survives a round trip`() {
        QaConfigJson.decode(QaConfigJson.encode(QaConfig())) shouldBe Decoded.Ok(QaConfig())
    }

    @Test
    fun `an integral number is read by value whatever its spelling`() {
        listOf("100", "100.0", "1e2", "1E2", "1.00e2").forEach {
            ok("""{"order":{"amount":$it}}""").order.amount shouldBe 100L
        }
    }

    @Test
    fun `a boolean a string or a fraction is not an amount`() {
        listOf("true", "\"100\"", "100.5", "[]", "{}").forEach {
            field("""{"order":{"amount":$it}}""") shouldBe "order.amount"
        }
    }

    @Test
    fun `an absurd exponent is rejected without exhausting memory`() {
        field("""{"order":{"amount":1e999999999}}""") shouldBe "order.amount"
        field("""{"order":{"amount":1e-999999999}}""") shouldBe "order.amount"
        ok("""{"order":{"amount":0e999999999}}""").order.amount shouldBe 0L
    }

    @Test
    fun `an exponent at the top of the int range is a wrong value and not a crash`() {
        listOf("1e2147483647", "-1e2147483647", "12e2147483647", "100e2147483647", "1e2147483648", "1e99999999999", "-1e99999999999")
            .forEach {
                field("""{"order":{"amount":$it}}""") shouldBe "order.amount"
                field("""{"version":$it}""") shouldBe "version"
            }
        field("""{"order":{"amount":1e-2147483648}}""") shouldBe "order.amount"
        field("""{"order":{"amount":0.5e-2147483647}}""") shouldBe "order.amount"
    }

    @Test
    fun `an explicit null is the same as an absent field`() {
        ok("""{"profile":null,"order":{"amount":null,"currency":null,"features":null},"sdk":{"theme":null,"googlePay":null}}""") shouldBe
            QaConfig()
    }

    @Test
    fun `a blank string is null for every nullable string`() {
        val config = ok("""{"profile":"","sdk":{"locale":" ","googlePay":{"environment":" ","merchantId":"\t"}}}""")
        config.profile shouldBe null
        config.sdk.locale shouldBe null
        config.sdk.googlePay.environment shouldBe null
        config.sdk.googlePay.merchantId shouldBe null
    }

    @Test
    fun `a non blank string is never trimmed`() {
        ok("""{"order":{"clientId":" client "}}""").order.clientId shouldBe " client "
    }

    @Test
    fun `a known object field that is not an object fails with its own path`() {
        field("""{"order":1}""") shouldBe "order"
        field("""{"sdk":[]}""") shouldBe "sdk"
        field("""{"sdk":{"googlePay":"x"}}""") shouldBe "sdk.googlePay"
        field("""{"sdk":{"applePay":true}}""") shouldBe "sdk.applePay"
        field("""{"sdk":{"sslPinning":1}}""") shouldBe "sdk.sslPinning"
    }

    @Test
    fun `an element of pins that is not an object fails as the pins field`() {
        field("""{"sdk":{"sslPinning":{"pins":["x"]}}}""") shouldBe "sdk.sslPinning.pins"
        field("""{"sdk":{"sslPinning":{"pins":"x"}}}""") shouldBe "sdk.sslPinning.pins"
        field("""{"sdk":{"sslPinning":{"pins":[{"host":"a.example"}]}}}""") shouldBe "sdk.sslPinning.pins"
    }

    @Test
    fun `a url scheme is compared ignoring case and a bare scheme is rejected`() {
        ok("""{"sdk":{"keyProviderUrl":"HTTPS://dev.bpcbt.com/keys"}}""").sdk.keyProviderUrl shouldBe "HTTPS://dev.bpcbt.com/keys"
        ok("""{"sdk":{"keyProviderUrl":"HTTPS://h"}}""").sdk.keyProviderUrl shouldBe "HTTPS://h"
        field("""{"sdk":{"keyProviderUrl":"http\u017f://h"}}""") shouldBe "sdk.keyProviderUrl"
        field("""{"sdk":{"keyProviderUrl":"https://"}}""") shouldBe "sdk.keyProviderUrl"
        field("""{"sdk":{"keyProviderUrl":"ftp://dev.bpcbt.com"}}""") shouldBe "sdk.keyProviderUrl"
        field("""{"sdk":{"keyProviderUrl":"https://dev.bpcbt.com/a b"}}""") shouldBe "sdk.keyProviderUrl"
    }

    @Test
    fun `the first failing field in the table order is the one reported`() {
        field("""{"sdk":{"theme":"nope"},"order":{"amount":"x"}}""") shouldBe "order.amount"
        field("""{"order":{"currency":"XXX"},"sdk":{"theme":1}}""") shouldBe "order.currency"
        field("""{"order":{"features":{}},"profile":"UPPER"}""") shouldBe "profile"
        field("""{"order":{"source":"nope"},"version":2}""") shouldBe "version"
    }

    @Test
    fun `sets are written once and in the canonical order`() {
        val config =
            ok(
                """{"sdk":{"googlePay":{"allowedCardNetworks":["VISA","AMEX","VISA"],""" +
                    """"allowedAuthMethods":["CRYPTOGRAM_3DS","PAN_ONLY"]}}}""",
            )
        config.sdk.googlePay.allowedCardNetworks shouldBe listOf("AMEX", "VISA")
        config.sdk.googlePay.allowedAuthMethods shouldBe listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
        val unordered = QaConfig(sdk = SdkOptions(googlePay = GooglePayOptions(allowedCardNetworks = listOf("VISA", "AMEX", "VISA"))))
        ok(QaConfigJson.encode(unordered)).sdk.googlePay.allowedCardNetworks shouldBe listOf("AMEX", "VISA")
    }

    @Test
    fun `features keep the read order without duplicates`() {
        ok("""{"order":{"features":["B","A","B"]}}""").order.features shouldBe listOf("B", "A")
    }

    @Test
    fun `problemOf judges a typed value by the same rules as decode`() {
        QaConfigJson.problemOf(QaConfig(order = OrderConfig(amount = -1)))?.field shouldBe "order.amount"
        QaConfigJson.problemOf(QaConfig(order = OrderConfig(features = listOf("a b"))))?.field shouldBe "order.features"
        QaConfigJson.problemOf(QaConfig(sdk = SdkOptions(locale = "xx")))?.field shouldBe "sdk.locale"
        QaConfigJson.problemOf(QaConfig()) shouldBe null
    }
}
