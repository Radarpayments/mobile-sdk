package net.payrdr.mobile.payment.sdk.payment.gpay

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import kotlin.test.Test

class GooglePayRequestBuilderTest {
    private fun sampleConfig(): GooglePayConfig =
        GooglePayConfig(
            merchantId = "M-123",
            merchantFullName = "Test Merchant",
            gateway = "payrdr",
            gatewayMerchantId = "gw-42",
            environment = GooglePayEnvironment.PRODUCTION,
            allowedCardNetworks = setOf(GooglePayCardNetwork.VISA, GooglePayCardNetwork.MASTERCARD),
            allowedAuthMethods = setOf(GooglePayAuthMethod.PAN_ONLY, GooglePayAuthMethod.CRYPTOGRAM_3DS),
        )

    @Test fun `produces apiVersion 2 envelope`() {
        val json =
            GooglePayRequestBuilder.buildPaymentDataRequest(
                config = sampleConfig(),
                totalPriceMajor = "10.55",
                currencyCode = "USD",
                countryCode = "US",
            )
        val obj = Json.parseToJsonElement(json).jsonObject
        obj["apiVersion"]!!.jsonPrimitive.content shouldBe "2"
        obj["apiVersionMinor"]!!.jsonPrimitive.content shouldBe "0"
    }

    @Test fun `merchantInfo carries id and name`() {
        val json = GooglePayRequestBuilder.buildPaymentDataRequest(sampleConfig(), "1.00", "USD", "US")
        val merchant = Json.parseToJsonElement(json).jsonObject["merchantInfo"]!!.jsonObject
        merchant["merchantId"]!!.jsonPrimitive.content shouldBe "M-123"
        merchant["merchantName"]!!.jsonPrimitive.content shouldBe "Test Merchant"
    }

    @Test fun `transactionInfo carries price currency country`() {
        val json = GooglePayRequestBuilder.buildPaymentDataRequest(sampleConfig(), "10.55", "USD", "US")
        val tx = Json.parseToJsonElement(json).jsonObject["transactionInfo"]!!.jsonObject
        tx["totalPrice"]!!.jsonPrimitive.content shouldBe "10.55"
        tx["totalPriceStatus"]!!.jsonPrimitive.content shouldBe "FINAL"
        tx["currencyCode"]!!.jsonPrimitive.content shouldBe "USD"
        tx["countryCode"]!!.jsonPrimitive.content shouldBe "US"
    }

    @Test fun `allowedPaymentMethods encodes networks and auth methods correctly`() {
        val json = GooglePayRequestBuilder.buildPaymentDataRequest(sampleConfig(), "1.00", "USD", "US")
        val method =
            Json
                .parseToJsonElement(json)
                .jsonObject["allowedPaymentMethods"]!!
                .jsonArray[0]
                .jsonObject
        method["type"]!!.jsonPrimitive.content shouldBe "CARD"
        val params = method["parameters"]!!.jsonObject
        val networks = params["allowedCardNetworks"]!!.jsonArray.map { it.jsonPrimitive.content }
        val auths = params["allowedAuthMethods"]!!.jsonArray.map { it.jsonPrimitive.content }
        networks.shouldContainExactlyInAnyOrder("VISA", "MASTERCARD")
        auths.shouldContainExactlyInAnyOrder("PAN_ONLY", "CRYPTOGRAM_3DS")
    }

    @Test fun `tokenizationSpecification is PAYMENT_GATEWAY with gateway and gatewayMerchantId`() {
        val json = GooglePayRequestBuilder.buildPaymentDataRequest(sampleConfig(), "1.00", "USD", "US")
        val spec =
            Json
                .parseToJsonElement(json)
                .jsonObject["allowedPaymentMethods"]!!
                .jsonArray[0]
                .jsonObject["tokenizationSpecification"]!!
                .jsonObject
        spec["type"]!!.jsonPrimitive.content shouldBe "PAYMENT_GATEWAY"
        val p = spec["parameters"]!!.jsonObject
        p["gateway"]!!.jsonPrimitive.content shouldBe "payrdr"
        p["gatewayMerchantId"]!!.jsonPrimitive.content shouldBe "gw-42"
    }

    @Test fun `transactionInfo sets checkoutOption to COMPLETE_IMMEDIATE_PURCHASE`() {
        val json = GooglePayRequestBuilder.buildPaymentDataRequest(sampleConfig(), "1.00", "USD", "US")
        val tx = Json.parseToJsonElement(json).jsonObject["transactionInfo"]!!.jsonObject
        tx["checkoutOption"]!!.jsonPrimitive.content shouldBe "COMPLETE_IMMEDIATE_PURCHASE"
    }

    @Test fun `billingAddressRequired=true injects billingAddressParameters with FULL format`() {
        val cfg = sampleConfig().copy(billingAddressRequired = true)
        val json = GooglePayRequestBuilder.buildPaymentDataRequest(cfg, "1.00", "USD", "US")
        val params =
            Json
                .parseToJsonElement(json)
                .jsonObject["allowedPaymentMethods"]!!
                .jsonArray[0]
                .jsonObject["parameters"]!!
                .jsonObject
        params["billingAddressRequired"]!!.jsonPrimitive.content shouldBe "true"
        params["billingAddressParameters"]!!.jsonObject["format"]!!.jsonPrimitive.content shouldBe "FULL"
    }

    @Test fun `buildIsReadyToPayRequest has minimal shape without transactionInfo or merchantInfo`() {
        val json = GooglePayRequestBuilder.buildIsReadyToPayRequest(sampleConfig())
        val obj = Json.parseToJsonElement(json).jsonObject
        obj["apiVersion"]!!.jsonPrimitive.content shouldBe "2"
        obj["transactionInfo"] shouldBe null
        obj["merchantInfo"] shouldBe null
        obj["allowedPaymentMethods"]!!.jsonArray.size shouldBe 1
        // In isReadyToPay the method has NO tokenizationSpecification.
        obj["allowedPaymentMethods"]!!.jsonArray[0].jsonObject["tokenizationSpecification"] shouldBe null
    }

    @Test fun `button payment methods are the bare array PayButton takes`() {
        val json = GooglePayRequestBuilder.buildButtonPaymentMethods(sampleConfig())
        val methods = Json.parseToJsonElement(json).jsonArray
        methods.size shouldBe 1
        methods[0].jsonObject["type"]!!.jsonPrimitive.content shouldBe "CARD"
        methods[0].jsonObject["tokenizationSpecification"] shouldBe null
    }

    @Test fun `buildPaymentDataRequest rejects empty allowedCardNetworks`() {
        val cfg = sampleConfig().copy(allowedCardNetworks = emptySet())
        shouldThrow<IllegalArgumentException> {
            GooglePayRequestBuilder.buildPaymentDataRequest(cfg, "1.00", "USD", "US")
        }
    }

    @Test fun `buildPaymentDataRequest rejects malformed totalPriceMajor`() {
        shouldThrow<IllegalArgumentException> {
            GooglePayRequestBuilder.buildPaymentDataRequest(sampleConfig(), "1,00", "USD", "US")
        }
        shouldThrow<IllegalArgumentException> {
            GooglePayRequestBuilder.buildPaymentDataRequest(sampleConfig(), "$1.00", "USD", "US")
        }
        shouldThrow<IllegalArgumentException> {
            GooglePayRequestBuilder.buildPaymentDataRequest(sampleConfig(), "", "USD", "US")
        }
    }

    @Test fun `buildPaymentDataRequest rejects wrong-length currency or country`() {
        shouldThrow<IllegalArgumentException> {
            GooglePayRequestBuilder.buildPaymentDataRequest(sampleConfig(), "1.00", "US", "US")
        }
        shouldThrow<IllegalArgumentException> {
            GooglePayRequestBuilder.buildPaymentDataRequest(sampleConfig(), "1.00", "USD", "USA")
        }
    }

    @Test fun `buildPaymentDataRequest rejects a config whose gateway is not known`() {
        shouldThrow<IllegalArgumentException> {
            GooglePayRequestBuilder.buildPaymentDataRequest(sampleConfig().copy(gateway = null), "1.00", "USD", "US")
        }
    }

    @Test fun `merchantInfo leaves the name out when it is not known`() {
        val json = GooglePayRequestBuilder.buildPaymentDataRequest(sampleConfig().copy(merchantFullName = null), "1.00", "USD", "US")
        val merchant = Json.parseToJsonElement(json).jsonObject["merchantInfo"]!!.jsonObject
        merchant.keys shouldBe setOf("merchantId")
    }
}
