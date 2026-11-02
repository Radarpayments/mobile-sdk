package net.payrdr.mobile.payment.sdk.payment.flow

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.BillingPayerData
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.BindingItem
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.SessionStatusResponse
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import kotlin.test.Test

class PayerDataMapperTest {
    // Same settings as DefaultHttpClientFactory.
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

    private fun session(body: String): SessionStatusResponse = json.decodeFromString(SessionStatusResponse.serializer(), body)

    @Test
    fun `parses the gateway shape per payment system`() {
        // Captured from dev.bpcbt.com getSessionStatus.do, plus a field this SDK does not collect.
        val request =
            PayerDataMapper.request(
                session(
                    """
                    {"payerDataParamsNeedToBeFilled":{"VISA":[
                      {"name":"MOBILE_PHONE","mandatory":true,"editable":true},
                      {"name":"EMAIL","mandatory":true,"editable":true},
                      {"name":"BILLING_ADDRESS_LINE1","mandatory":false,"editable":true},
                      {"name":"BILLING_COUNTRY","mandatory":false,"editable":false},
                      {"name":"PAYER_NICKNAME","mandatory":true,"editable":true}
                    ]},"customerDetails":{"suggestedCountryCodes":[]}}
                    """.trimIndent(),
                ),
            )

        request.fieldsFor("VISA") shouldContainExactly
            listOf(
                PayerFieldSpec(PayerField.MOBILE_PHONE, mandatory = true),
                PayerFieldSpec(PayerField.EMAIL, mandatory = true),
                PayerFieldSpec(PayerField.BILLING_ADDRESS_LINE1, mandatory = false),
                PayerFieldSpec(PayerField.BILLING_COUNTRY, mandatory = false, editable = false),
            )
        request.fieldsFor("visa").size shouldBe 4
        request.fieldsFor("MASTERCARD").shouldBeEmpty()
        request.fieldsFor(null).shouldBeEmpty()
        request.isEmpty shouldBe false
    }

    @Test
    fun `missing or null params mean nothing to collect`() {
        PayerDataMapper.request(session("""{"orderNumber":"1"}""")).isEmpty shouldBe true
        PayerDataMapper.request(session("""{"payerDataParamsNeedToBeFilled":null,"customerDetails":null}""")).isEmpty shouldBe true
    }

    @Test
    fun `prefill follows the payment page`() {
        val request =
            PayerDataMapper.request(
                session(
                    """
                    {"customerDetails":{"email":"john.smith@example.com","phone":"+447700900123",
                       "suggestedCountryCodes":[{"phoneCountryCode":"+44","alpha2":"GB"}]},
                     "orderPayerData":{"mobilePhone":"+35799123456"},
                     "billingPayerData":{"billingCity":"London","billingPostalCode":"NW1 6XE","billingAddressLine1":" "}}
                    """.trimIndent(),
                ),
            )

        request.prefill shouldBe
            mapOf(
                PayerField.EMAIL to "john.smith@example.com",
                PayerField.MOBILE_PHONE to "+35799123456",
                PayerField.BILLING_CITY to "London",
                PayerField.BILLING_POSTAL_CODE to "NW1 6XE",
                PayerField.BILLING_COUNTRY to "GB",
            )
        request.phoneRegion shouldBe "GB"
    }

    @Test
    fun `billing country from the order wins and unknown codes are dropped`() {
        PayerDataMapper
            .request(
                session(
                    """{"billingPayerData":{"billingCountry":"de"},"customerDetails":{"suggestedCountryCodes":[{"alpha2":"GB"}]}}""",
                ),
            ).prefill[PayerField.BILLING_COUNTRY] shouldBe "DE"
        PayerDataMapper
            .request(session("""{"billingPayerData":{"billingCountry":"XX"}}"""))
            .prefill[PayerField.BILLING_COUNTRY] shouldBe null
    }

    @Test
    fun `binding prefill uses the remembered payer data`() {
        PayerDataMapper.bindingPrefill(
            BindingItem(
                id = "b",
                label = "411111**1111 12/26",
                paymentSystem = "VISA",
                payerEmail = "jane@example.com",
                payerPhone = "",
                bindingBillingData = BillingPayerData(billingCity = "Leeds"),
            ),
        ) shouldBe mapOf(PayerField.EMAIL to "jane@example.com", PayerField.BILLING_CITY to "Leeds")
    }

    @Test
    fun `transfer fields of the sender and the recipient are collected and prefilled`() {
        // Trimmed getSessionStatus of an AFT order (registerP2P with WITHOUT_TO_CARD), as the gateway sends it.
        val request =
            PayerDataMapper.request(
                session(
                    """
                    {"orderPaymentWay":"P2P","orderFeatures":["WITHOUT_TO_CARD"],
                     "payerDataParamsNeedToBeFilled":{
                       "MASTERCARD":[{"name":"RECIPIENT_FIRST_NAME","mandatory":true,"editable":true},
                                     {"name":"RECIPIENT_ACCOUNT","mandatory":true,"editable":false},
                                     {"name":"RECIPIENT_COUNTRY","mandatory":true,"editable":true}],
                       "VISA":[{"name":"PAYER_FIRST_NAME","mandatory":true,"editable":true},
                               {"name":"PAYER_BIRTHDAY","mandatory":true,"editable":true},
                               {"name":"PAYER_ACCOUNT_NUMBER_TYPE","mandatory":false,"editable":true},
                               {"name":"BILLING_CITY","mandatory":true,"editable":true}]},
                     "billingPayerData":{"billingCity":"Berlin","payerFirstName":"Test","payerLastName":"User",
                       "payerCombinedName":"Test User","payerBirthday":"19901212","payerAccountNumberType":"RTN"},
                     "billingRecipientData":{"recipientAccount":"test","recipientFirstName":"Rita","recipientCountry":"de",
                       "recipientIdType":"IDTP1"}}
                    """.trimIndent(),
                ),
            )

        request.fieldsFor("MASTERCARD") shouldContainExactly
            listOf(
                PayerFieldSpec(PayerField.RECIPIENT_FIRST_NAME, mandatory = true),
                PayerFieldSpec(PayerField.RECIPIENT_ACCOUNT, mandatory = true, editable = false),
                PayerFieldSpec(PayerField.RECIPIENT_COUNTRY, mandatory = true),
            )
        request.fieldsFor("VISA").map { it.field } shouldContainExactly
            listOf(PayerField.PAYER_FIRST_NAME, PayerField.PAYER_BIRTHDAY, PayerField.PAYER_ACCOUNT_NUMBER_TYPE, PayerField.BILLING_CITY)
        request.prefill shouldBe
            mapOf(
                PayerField.BILLING_CITY to "Berlin",
                PayerField.PAYER_FIRST_NAME to "Test",
                PayerField.PAYER_LAST_NAME to "User",
                PayerField.PAYER_BIRTHDAY to "19901212",
                PayerField.PAYER_ACCOUNT_NUMBER_TYPE to "1",
                PayerField.RECIPIENT_ACCOUNT to "test",
                PayerField.RECIPIENT_FIRST_NAME to "Rita",
                PayerField.RECIPIENT_COUNTRY to "DE",
                PayerField.RECIPIENT_ID_TYPE to "IDTP1",
            )
    }

    @Test
    fun `account types the gateway sends by name prefill the picker by code`() {
        val names =
            mapOf(
                "RTN" to "1",
                "IBAN" to "2",
                "CARD_ACCOUNT" to "3",
                "EMAIL" to "4",
                "PHONE_NUMBER" to "5",
                "BAN_AND_BIC" to "6",
                "WALLET_ID" to "7",
                "SOCIAL_NETWORK_ID" to "8",
                "OTHER" to "100",
                "wallet_id" to "7",
                "2" to "2",
                "100" to "100",
            )
        names.forEach { (sent, code) ->
            val prefill =
                PayerDataMapper
                    .request(
                        session(
                            """{"billingPayerData":{"payerAccountNumberType":"$sent"},
                               "billingRecipientData":{"recipientAccountNumberType":"$sent"}}""",
                        ),
                    ).prefill
            prefill[PayerField.PAYER_ACCOUNT_NUMBER_TYPE] shouldBe code
            prefill[PayerField.RECIPIENT_ACCOUNT_NUMBER_TYPE] shouldBe code
        }
    }

    @Test
    fun `an account type the picker does not know is not prefilled`() {
        val prefill =
            PayerDataMapper
                .request(
                    session(
                        """{"billingPayerData":{"payerAccountNumberType":"FOO"},
                           "billingRecipientData":{"recipientAccountNumberType":"9"}}""",
                    ),
                ).prefill
        prefill.containsKey(PayerField.PAYER_ACCOUNT_NUMBER_TYPE) shouldBe false
        prefill.containsKey(PayerField.RECIPIENT_ACCOUNT_NUMBER_TYPE) shouldBe false
    }
}
