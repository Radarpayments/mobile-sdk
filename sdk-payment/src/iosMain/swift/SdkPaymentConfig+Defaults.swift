import Foundation

extension SdkPaymentConfig {
    /// The checkout config with Swift defaults: only the gateway address is required.
    ///
    /// - Parameters:
    ///   - baseUrl: the payment gateway, e.g. `https://pay.example.com/payment`
    ///   - keyProviderUrl: where the card-encryption key comes from; `nil` = `{baseUrl}/se/keys.do`
    ///   - sslPinning: certificate pinning; `nil` = the system trust store
    ///   - logTransport: where the SDK logs go; `nil` = nowhere
    ///   - locale: language of the checkout, such as `"de"`; `nil` = the device language
    ///   - theme: the look of the checkout; system light/dark by default
    ///   - googlePay: Google Pay (Android only); `nil` = no Google Pay button
    ///   - applePay: Apple Pay; `nil` = no Apple Pay button
    ///   - cardScannerProvider: the card scanner; `nil` = no scan button
    ///   - showResultScreen: the checkout shows its own success / declined screen before returning
    public convenience init(
        baseUrl: String,
        keyProviderUrl: String? = nil,
        sslPinning: SslPinningConfig? = nil,
        logTransport: (any LogTransport)? = nil,
        locale: String? = nil,
        theme: any PaymentTheme = PaymentThemeSystem.shared,
        googlePay: GooglePayConfig? = nil,
        applePay: ApplePayConfig? = nil,
        cardScannerProvider: (any CardScannerProvider)? = nil,
        showResultScreen: Bool = false
    ) {
        self.init(
            __baseUrl: baseUrl,
            keyProviderUrl: keyProviderUrl,
            sslPinning: sslPinning,
            logTransport: logTransport,
            locale: locale,
            theme: theme,
            googlePay: googlePay,
            applePay: applePay,
            cardScannerProvider: cardScannerProvider,
            showResultScreen: showResultScreen
        )
    }
}
