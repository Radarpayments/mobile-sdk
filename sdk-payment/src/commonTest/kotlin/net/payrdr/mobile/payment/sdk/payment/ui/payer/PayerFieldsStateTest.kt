package net.payrdr.mobile.payment.sdk.payment.ui.payer

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.result.PayerDataRequest
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import net.payrdr.mobile.payment.sdk.payment.ui.util.FieldError
import kotlin.test.Test

class PayerFieldsStateTest {
    private val visa =
        listOf(
            PayerFieldSpec(PayerField.BILLING_CITY, mandatory = false),
            PayerFieldSpec(PayerField.BILLING_ADDRESS_LINE3, mandatory = false),
            PayerFieldSpec(PayerField.EMAIL, mandatory = true),
            PayerFieldSpec(PayerField.BILLING_ADDRESS_LINE1, mandatory = false),
            PayerFieldSpec(PayerField.BILLING_ADDRESS_LINE2, mandatory = false),
            PayerFieldSpec(PayerField.MOBILE_PHONE, mandatory = true),
        )
    private val request =
        PayerDataRequest(
            fieldsByPaymentSystem = mapOf("VISA" to visa, "MASTERCARD" to listOf(PayerFieldSpec(PayerField.EMAIL, mandatory = false))),
            prefill = mapOf(PayerField.EMAIL to "john@example.com"),
            phoneRegion = "GB",
        )

    @Test
    fun `fields follow the payment system of the card`() {
        val state = PayerFieldsState(request)
        state.specsFor(emptyList()).shouldBeEmpty()
        state.specsFor(listOf("MASTERCARD", "MAESTRO")).map { it.field } shouldContainExactly listOf(PayerField.EMAIL)
        state.specsFor(listOf("VISA")).map { it.field } shouldContainExactly
            listOf(PayerField.EMAIL, PayerField.MOBILE_PHONE, PayerField.BILLING_ADDRESS_LINE1, PayerField.BILLING_CITY)
    }

    @Test
    fun `extra address lines are revealed one by one`() {
        val state = PayerFieldsState(request)
        state.canAddAddressLine(listOf("VISA")) shouldBe true
        state.addAddressLine()
        state.specsFor(listOf("VISA")).map { it.field } shouldContainExactly
            listOf(
                PayerField.EMAIL,
                PayerField.MOBILE_PHONE,
                PayerField.BILLING_ADDRESS_LINE1,
                PayerField.BILLING_ADDRESS_LINE2,
                PayerField.BILLING_CITY,
            )
        state.addAddressLine()
        state.canAddAddressLine(listOf("VISA")) shouldBe false
    }

    @Test
    fun `removing an address line moves the next one up and hides the last`() {
        val state = PayerFieldsState(request)
        state.addAddressLine()
        state.addAddressLine()
        state.onValueChange(PayerField.BILLING_ADDRESS_LINE2, "Floor 3")
        state.onValueChange(PayerField.BILLING_ADDRESS_LINE3, "Office 12")
        state.removeAddressLine(PayerField.BILLING_ADDRESS_LINE2)
        state.value(PayerField.BILLING_ADDRESS_LINE2) shouldBe "Office 12"
        state.value(PayerField.BILLING_ADDRESS_LINE3) shouldBe ""
        state.specsFor(listOf("VISA")).map { it.field } shouldContainExactly
            listOf(
                PayerField.EMAIL,
                PayerField.MOBILE_PHONE,
                PayerField.BILLING_ADDRESS_LINE1,
                PayerField.BILLING_ADDRESS_LINE2,
                PayerField.BILLING_CITY,
            )
        state.canAddAddressLine(listOf("VISA")) shouldBe true
    }

    @Test
    fun `a lone second address line is shown without the add action`() {
        val state =
            PayerFieldsState(
                PayerDataRequest(mapOf("VISA" to listOf(PayerFieldSpec(PayerField.BILLING_ADDRESS_LINE2, mandatory = false)))),
            )
        state.specsFor(listOf("VISA")).map { it.field } shouldContainExactly listOf(PayerField.BILLING_ADDRESS_LINE2)
        state.canAddAddressLine(listOf("VISA")) shouldBe false
    }

    @Test
    fun `phone starts with the suggested country and sends E164`() {
        val state = PayerFieldsState(request)
        state.phoneCountry?.iso shouldBe "GB"
        state.onPhoneInput("07700 900123")
        state.phoneNational shouldBe "7700900123"
        state.value(PayerField.MOBILE_PHONE) shouldBe "+447700900123"
        state.errorOf(visa.last()) shouldBe null
    }

    @Test
    fun `pasted international number switches the country and the picker keeps typed digits`() {
        val state = PayerFieldsState(request)
        state.onPhoneInput("+357 96 123456")
        state.phoneCountry?.iso shouldBe "CY"
        state.value(PayerField.MOBILE_PHONE) shouldBe "+35796123456"
        state.selectPhoneCountry("GR")
        state.phoneNational shouldBe "96123456"
        state.value(PayerField.MOBILE_PHONE) shouldBe "+3096123456"
    }

    @Test
    fun `without a suggestion the device region is used and an empty phone is not sent`() {
        val state = PayerFieldsState(request.copy(phoneRegion = null), deviceRegion = "DE")
        state.phoneCountry?.iso shouldBe "DE"
        state.suggestedPhoneCountries.map { it.iso } shouldBe listOf("DE")
        state.value(PayerField.MOBILE_PHONE) shouldBe ""
        state.onFocusChange(PayerField.MOBILE_PHONE, true)
        state.onFocusChange(PayerField.MOBILE_PHONE, false)
        state.visibleErrorOf(visa.last()) shouldBe FieldError.PHONE_EMPTY
    }

    @Test
    fun `prefilled phone is split into country and digits`() {
        val state = PayerFieldsState(request, prefillOverride = mapOf(PayerField.MOBILE_PHONE to "+35799000000"))
        state.phoneCountry?.iso shouldBe "CY"
        state.phoneNational shouldBe "99000000"
    }

    @Test
    fun `submit validates shown fields and returns non-blank values`() {
        val state = PayerFieldsState(request, prefillOverride = mapOf(PayerField.BILLING_CITY to "Leeds"))
        val specs = state.specsFor(listOf("VISA"))
        state.validateAll(specs) shouldBe false
        state.onValueChange(PayerField.MOBILE_PHONE, "7700 900123")
        state.validateAll(specs) shouldBe true
        state.submitValues(specs) shouldBe
            mapOf(
                PayerField.EMAIL to "john@example.com",
                PayerField.MOBILE_PHONE to "+447700900123",
                PayerField.BILLING_CITY to "Leeds",
            )
    }

    @Test
    fun `optional billing rests as the add row - the summary once filled - the fields while a value is wrong`() {
        val billing =
            listOf(
                PayerFieldSpec(PayerField.BILLING_ADDRESS_LINE1, mandatory = false),
                PayerFieldSpec(PayerField.BILLING_CITY, mandatory = false),
            )
        val state = PayerFieldsState(request)
        state.billingView(billing) shouldBe BillingView.ADD
        state.onValueChange(PayerField.BILLING_CITY, "Leeds")
        state.billingView(billing) shouldBe BillingView.SUMMARY
        state.onValueChange(PayerField.BILLING_ADDRESS_LINE1, "Улица")
        state.billingView(billing) shouldBe BillingView.FIELDS
        state.validateAll(billing) shouldBe false
        state.onValueChange(PayerField.BILLING_ADDRESS_LINE1, "")
        state.onValueChange(PayerField.BILLING_CITY, "")
        state.billingView(billing) shouldBe BillingView.ADD
        PayerFieldsState(request, prefillOverride = mapOf(PayerField.BILLING_CITY to "Leeds")).billingView(billing) shouldBe
            BillingView.SUMMARY
    }

    @Test
    fun `input is cut at the gateway limit and read-only needs a value`() {
        val state = PayerFieldsState(request)
        state.onValueChange(PayerField.BILLING_CITY, "x".repeat(60))
        state.value(PayerField.BILLING_CITY).length shouldBe 40
        state.isEditable(PayerFieldSpec(PayerField.EMAIL, mandatory = true, editable = false)) shouldBe false
        state.isEditable(PayerFieldSpec(PayerField.BILLING_STATE, mandatory = true, editable = false)) shouldBe true
    }
}
