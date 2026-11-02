package net.payrdr.mobile.payment.sdk.payment.applepay

import platform.PassKit.PKMerchantCapability3DS
import platform.PassKit.PKMerchantCapabilityCredit
import platform.PassKit.PKMerchantCapabilityDebit
import platform.PassKit.PKMerchantCapabilityEMV
import platform.PassKit.PKPaymentNetworkAmex
import platform.PassKit.PKPaymentNetworkDiscover
import platform.PassKit.PKPaymentNetworkJCB
import platform.PassKit.PKPaymentNetworkMasterCard
import platform.PassKit.PKPaymentNetworkVisa

/**
 * Map [ApplePayNetwork] → `PKPaymentNetwork` string constant. iOS 15.0 supports all of these.
 *
 * K/N types PassKit's `PKPaymentNetwork*` constants as `String?` since they are nullable
 * in Objective-C headers; in practice they are never `nil` on any deployment target we care
 * about, so we `!!` here.
 */
internal fun ApplePayNetwork.toPkPaymentNetwork(): String =
    when (this) {
        ApplePayNetwork.AMEX -> PKPaymentNetworkAmex!!
        ApplePayNetwork.DISCOVER -> PKPaymentNetworkDiscover!!
        ApplePayNetwork.JCB -> PKPaymentNetworkJCB!!
        ApplePayNetwork.MASTERCARD -> PKPaymentNetworkMasterCard!!
        ApplePayNetwork.VISA -> PKPaymentNetworkVisa!!
    }

/** Map capability enum-set to PKMerchantCapability bitmask. */
internal fun Set<ApplePayMerchantCapability>.toPkMerchantCapabilityMask(): ULong {
    var mask: ULong = 0u
    if (ApplePayMerchantCapability.THREE_D_SECURE in this) mask = mask or PKMerchantCapability3DS
    if (ApplePayMerchantCapability.EMV in this) mask = mask or PKMerchantCapabilityEMV
    if (ApplePayMerchantCapability.CREDIT in this) mask = mask or PKMerchantCapabilityCredit
    if (ApplePayMerchantCapability.DEBIT in this) mask = mask or PKMerchantCapabilityDebit
    return mask
}
