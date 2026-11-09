package net.payrdr.mobile.payment.sdk.payment.applepay

import io.kotest.matchers.shouldBe
import platform.PassKit.PKMerchantCapability3DS
import platform.PassKit.PKMerchantCapabilityCredit
import platform.PassKit.PKMerchantCapabilityDebit
import platform.PassKit.PKMerchantCapabilityEMV
import platform.PassKit.PKPaymentNetworkAmex
import platform.PassKit.PKPaymentNetworkDiscover
import platform.PassKit.PKPaymentNetworkJCB
import platform.PassKit.PKPaymentNetworkMasterCard
import platform.PassKit.PKPaymentNetworkVisa
import kotlin.test.Test

class ApplePayNetworkMappingTest {
    @Test
    fun every_network_is_offered_to_apple_pay_as_its_own_passkit_network() {
        ApplePayNetwork.entries.associateWith { it.toPkPaymentNetwork() } shouldBe
            mapOf(
                ApplePayNetwork.AMEX to PKPaymentNetworkAmex,
                ApplePayNetwork.DISCOVER to PKPaymentNetworkDiscover,
                ApplePayNetwork.JCB to PKPaymentNetworkJCB,
                ApplePayNetwork.MASTERCARD to PKPaymentNetworkMasterCard,
                ApplePayNetwork.VISA to PKPaymentNetworkVisa,
            )
    }

    @Test
    fun every_capability_sets_its_own_passkit_flag() {
        ApplePayMerchantCapability.entries.associateWith { setOf(it).toPkMerchantCapabilityMask() } shouldBe
            mapOf(
                ApplePayMerchantCapability.THREE_D_SECURE to PKMerchantCapability3DS,
                ApplePayMerchantCapability.EMV to PKMerchantCapabilityEMV,
                ApplePayMerchantCapability.CREDIT to PKMerchantCapabilityCredit,
                ApplePayMerchantCapability.DEBIT to PKMerchantCapabilityDebit,
            )
        emptySet<ApplePayMerchantCapability>().toPkMerchantCapabilityMask() shouldBe 0uL
    }
}
