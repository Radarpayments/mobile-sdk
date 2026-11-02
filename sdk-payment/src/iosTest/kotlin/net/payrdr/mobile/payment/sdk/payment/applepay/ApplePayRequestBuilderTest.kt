package net.payrdr.mobile.payment.sdk.payment.applepay

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import platform.PassKit.PKMerchantCapability3DS
import platform.PassKit.PKMerchantCapabilityCredit
import platform.PassKit.PKMerchantCapabilityDebit
import platform.PassKit.PKPaymentNetworkMasterCard
import platform.PassKit.PKPaymentNetworkVisa
import platform.PassKit.PKPaymentSummaryItem
import kotlin.test.Test

class ApplePayRequestBuilderTest {
    private fun sampleConfig() =
        ApplePayConfig(
            merchantId = "merchant.net.payrdr.test",
            supportedNetworks =
                setOf(
                    ApplePayNetwork.VISA,
                    ApplePayNetwork.MASTERCARD,
                ),
            merchantCapabilities =
                setOf(
                    ApplePayMerchantCapability.THREE_D_SECURE,
                    ApplePayMerchantCapability.CREDIT,
                    ApplePayMerchantCapability.DEBIT,
                ),
        )

    @Test fun `builds request with merchantId currency country`() {
        val request =
            ApplePayRequestBuilder.build(
                config = sampleConfig(),
                amountMinorUnits = 10055L,
                currencyCode = "USD",
                countryCode = "US",
                merchantDisplayName = "Test Merchant",
            )
        request.merchantIdentifier shouldBe "merchant.net.payrdr.test"
        request.currencyCode shouldBe "USD"
        request.countryCode shouldBe "US"
    }

    @Test fun `encodes supportedNetworks`() {
        val request =
            ApplePayRequestBuilder.build(
                config = sampleConfig(),
                amountMinorUnits = 100L,
                currencyCode = "USD",
                countryCode = "US",
                merchantDisplayName = "Test Merchant",
            )

        @Suppress("UNCHECKED_CAST")
        val networks = request.supportedNetworks as List<String>
        networks.shouldContainAll(listOf(PKPaymentNetworkVisa, PKPaymentNetworkMasterCard))
    }

    @Test fun `encodes merchantCapabilities and single summary item`() {
        val request =
            ApplePayRequestBuilder.build(
                config = sampleConfig(),
                amountMinorUnits = 10055L,
                currencyCode = "USD",
                countryCode = "US",
                merchantDisplayName = "Test Merchant",
            )
        // raw value = bitmask of all three flags
        val expectedMask = PKMerchantCapability3DS or PKMerchantCapabilityCredit or PKMerchantCapabilityDebit
        request.merchantCapabilities shouldBe expectedMask

        request.paymentSummaryItems.size shouldBe 1
        // summary item label = merchantDisplayName
        @Suppress("UNCHECKED_CAST")
        val firstItem = request.paymentSummaryItems[0] as PKPaymentSummaryItem
        firstItem.label shouldBe "Test Merchant"
    }

    @Test fun `config countryCode overrides the runtime country and the currency stays the order's`() {
        val cfgWithOverrides =
            sampleConfig().copy(countryCode = "CY")
        val request =
            ApplePayRequestBuilder.build(
                config = cfgWithOverrides,
                amountMinorUnits = 100L,
                currencyCode = "USD",
                countryCode = "US",
                merchantDisplayName = "Test Merchant",
            )
        request.currencyCode shouldBe "USD"
        request.countryCode shouldBe "CY"
    }

    @Test fun `paymentSummaryItem amount is major-unit dot-decimal string`() {
        val request =
            ApplePayRequestBuilder.build(
                config = sampleConfig(),
                amountMinorUnits = 10055L,
                currencyCode = "USD",
                countryCode = "US",
                merchantDisplayName = "Test Merchant",
            )

        @Suppress("UNCHECKED_CAST")
        val item = request.paymentSummaryItems[0] as PKPaymentSummaryItem
        item.amount.stringValue shouldBe "100.55"
    }

    @Test fun `build rejects empty supportedNetworks`() {
        val bad = sampleConfig().copy(supportedNetworks = emptySet())
        shouldThrow<IllegalArgumentException> {
            ApplePayRequestBuilder.build(bad, 100L, "USD", "US", "Test")
        }
    }

    @Test fun `build rejects blank merchantDisplayName`() {
        shouldThrow<IllegalArgumentException> {
            ApplePayRequestBuilder.build(sampleConfig(), 100L, "USD", "US", "  ")
        }
    }
}
