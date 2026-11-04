import Foundation

extension ApplePayConfig {
    /// Apple Pay with Swift defaults: only the merchant identifier is required.
    ///
    /// - Parameters:
    ///   - merchantId: the Apple Pay merchant identifier, e.g. `merchant.com.example`
    ///   - supportedNetworks: card networks the merchant accepts; Visa and Mastercard by default
    ///   - merchantCapabilities: 3-D Secure, credit and debit by default
    ///   - countryCode: overrides the merchant's country the SDK takes from the order, e.g. `"DE"`
    public convenience init(
        merchantId: String,
        supportedNetworks: Set<ApplePayNetwork> = [.visa, .mastercard],
        merchantCapabilities: Set<ApplePayMerchantCapability> = [.threeDSecure, .credit, .debit],
        countryCode: String? = nil
    ) {
        self.init(
            __merchantId: merchantId,
            supportedNetworks: supportedNetworks,
            merchantCapabilities: merchantCapabilities,
            countryCode: countryCode
        )
    }
}
