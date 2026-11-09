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
}
