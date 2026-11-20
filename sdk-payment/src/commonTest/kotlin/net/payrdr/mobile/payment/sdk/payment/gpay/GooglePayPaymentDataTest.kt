package net.payrdr.mobile.payment.sdk.payment.gpay

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GooglePayPaymentDataTest {
    @Test
    fun `the token is the tokenization data passed on as the json string google pay gives`() {
        val paymentData =
            """
            {"apiVersion":2,"apiVersionMinor":0,
             "paymentMethodData":{"type":"CARD","description":"Visa •••• 1111",
              "info":{"cardNetwork":"VISA","cardDetails":"1111"},
              "tokenizationData":{"type":"PAYMENT_GATEWAY",
               "token":"{\"signature\":\"MEUCIQ==\",\"protocolVersion\":\"ECv2\",\"signedMessage\":\"{\\\"encryptedMessage\\\":\\\"abc\\\"}\"}"}}}
            """.trimIndent()

        googlePayToken(paymentData) shouldBe
            """{"signature":"MEUCIQ==","protocolVersion":"ECv2","signedMessage":"{\"encryptedMessage\":\"abc\"}"}"""
    }

    @Test
    fun `an answer without tokenization data has no token`() {
        googlePayToken("""{"paymentMethodData":{"type":"CARD"}}""").shouldBeNull()
    }

    @Test
    fun `a blank or non-text token is no token`() {
        googlePayToken("""{"paymentMethodData":{"tokenizationData":{"token":"  "}}}""").shouldBeNull()
        googlePayToken("""{"paymentMethodData":{"tokenizationData":{"token":null}}}""").shouldBeNull()
        googlePayToken("""{"paymentMethodData":{"tokenizationData":{"token":{"signature":"x"}}}}""").shouldBeNull()
    }

    @Test
    fun `an answer that is not json has no token`() {
        googlePayToken("not json").shouldBeNull()
        googlePayToken("[]").shouldBeNull()
    }

    @Test
    fun `the payer is the email and the billing address phone`() {
        val paymentData =
            """
            {"apiVersion":2,"email":"buyer@example.com",
             "paymentMethodData":{"type":"CARD",
              "info":{"cardNetwork":"VISA","billingAddress":{"name":"A Buyer","countryCode":"US","phoneNumber":"+1 650-555-5555"}},
              "tokenizationData":{"type":"PAYMENT_GATEWAY","token":"{}"}}}
            """.trimIndent()

        googlePayPayer(paymentData) shouldBe GooglePayPayer(email = "buyer@example.com", phone = "+1 650-555-5555")
    }

    @Test
    fun `a blank or missing contact is no contact`() {
        googlePayPayer("""{"email":"  ","paymentMethodData":{"info":{"billingAddress":{"phoneNumber":""}}}}""") shouldBe GooglePayPayer()
        googlePayPayer("""{"email":null,"paymentMethodData":{"info":{"billingAddress":{}}}}""") shouldBe GooglePayPayer()
        googlePayPayer("""{"paymentMethodData":{"info":{}}}""") shouldBe GooglePayPayer()
        googlePayPayer("""{"email":"buyer@example.com","paymentMethodData":"CARD"}""") shouldBe GooglePayPayer(email = "buyer@example.com")
    }

    @Test
    fun `an answer that is not a json object has no payer`() {
        googlePayPayer("not json") shouldBe GooglePayPayer()
        googlePayPayer("[]") shouldBe GooglePayPayer()
    }
}
