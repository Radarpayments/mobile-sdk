import RadarpaymentsSDKPayment

/// The SDK's config for a validated `config` on `profile`'s gateway, with the SDK's log going to `journal`.
///
/// `SslPinningConfig` has no equality, so two builds of one config with pinning are not equal: the checkout has to be
/// keyed on the `QaConfig` and the profile, not on this result.
func sdkPaymentConfig(
    config: QaConfig,
    profile: StandProfile,
    journal: any LogTransport,
    scanner: any CardScannerProvider
) -> SdkPaymentConfig {
    let sdk = config.sdk
    return SdkPaymentConfig(
        baseUrl: profile.gatewayUrl,
        keyProviderUrl: sdk.keyProviderUrl,
        sslPinning: sdk.sslPinning.enabled ? sslPinningConfig(sdk.sslPinning.pins) : nil,
        logTransport: journal,
        locale: sdk.locale,
        theme: sdk.theme.paymentTheme(),
        googlePay: nil,
        applePay: sdk.applePay.enabled ? applePayConfig(sdk.applePay) : nil,
        cardScannerProvider: sdk.scanner ? scanner : nil,
        showResultScreen: sdk.showResultScreen
    )
}

private func sslPinningConfig(_ pins: [Pin]) -> SslPinningConfig {
    pins.reduce(SslPinningConfig.companion.builder()) { builder, pin in builder.addPin(hostname: pin.host, sha256Pin: pin.pin) }
        .build()
}

private func applePayConfig(_ options: ApplePayOptions) -> ApplePayConfig {
    ApplePayConfig(
        merchantId: options.merchantId,
        supportedNetworks: Set(options.supportedNetworks.map { name in byName(name, ApplePayNetwork.allCases) { $0.toKotlinEnum().name } }),
        merchantCapabilities: Set(
            options.merchantCapabilities.map { name in byName(name, ApplePayMerchantCapability.allCases) { $0.toKotlinEnum().name } }
        ),
        countryCode: options.countryCode
    )
}

// The contract's names are the SDK's Kotlin enum names, and the validation lets only those through.
private func byName<T>(_ name: String, _ cases: [T], _ nameOf: (T) -> String) -> T {
    guard let value = cases.first(where: { nameOf($0) == name }) else {
        preconditionFailure("Apple Pay value '\(name)' passed the validation")
    }
    return value
}
