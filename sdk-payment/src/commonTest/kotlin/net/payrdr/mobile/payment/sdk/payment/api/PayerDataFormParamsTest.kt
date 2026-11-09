package net.payrdr.mobile.payment.sdk.payment.api

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import kotlin.test.Test

class PayerDataFormParamsTest {
    @Test
    fun `sends values the way the payment page does`() {
        PayerDataFormParams.build(
            mapOf(
                PayerField.EMAIL to " jane@example.com ",
                PayerField.MOBILE_PHONE to "+44 (20) 7946-0958",
                PayerField.BILLING_ADDRESS_LINE1 to "221B Baker Street",
                PayerField.BILLING_POSTAL_CODE to "NW1 6XE",
                PayerField.BILLING_COUNTRY to "GB",
                PayerField.BILLING_STATE to "",
            ),
        ) shouldBe
            listOf(
                "email" to "jane@example.com",
                "orderPayerData" to """{"mobilePhone":"+442079460958"}""",
                "billingPayerData" to """{"billingCountry":"GB","billingPostalCode":"NW1 6XE","billingAddressLine1":"221B Baker Street"}""",
            )
    }

    @Test
    fun `escapes user input in JSON`() {
        PayerDataFormParams.build(mapOf(PayerField.BILLING_CITY to """St. "John's" \ Wood""")) shouldBe
            listOf("billingPayerData" to """{"billingCity":"St. \"John's\" \\ Wood"}""")
    }

    @Test
    fun `nothing collected sends nothing`() {
        PayerDataFormParams.build(emptyMap()).shouldBeEmpty()
        PayerDataFormParams.build(mapOf(PayerField.EMAIL to "  ")).shouldBeEmpty()
    }
}
