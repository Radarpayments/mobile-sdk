/// Where the order comes from: registered by the app, or an existing order or session.
nonisolated enum OrderSource: String, CaseIterable {
    case register
    case mdOrder
    case sessionId
}

nonisolated enum OrderType: String, CaseIterable {
    case payment
    case aft
}

nonisolated enum ThemePreset: String, CaseIterable {
    case system
    case light
    case dark
    case contrast
    case rounded
    case square
    case largeText
    case debug
}

nonisolated struct Pin: Equatable {
    var host: String
    var pin: String
}

nonisolated struct OrderConfig: Equatable {
    var source: OrderSource = .register
    var type: OrderType = .payment
    var amount: Int64 = 10000
    var currency = "EUR"
    var clientId: String?
    var features: [String] = []
    var extra: JSONObject = [:]
    var mdOrder: String?
    var sessionId: String?
}

nonisolated struct SslPinningOptions: Equatable {
    var enabled = false
    var pins: [Pin] = []
}

// The sets default to the canonical order of the contract, so that a decoded empty document equals the default.
nonisolated struct GooglePayOptions: Equatable {
    var enabled = false
    var merchantId: String?
    var merchantFullName: String?
    var gateway: String?
    var gatewayMerchantId: String?
    var environment: String?
    var allowedCardNetworks = ["MASTERCARD", "VISA"]
    var allowedAuthMethods = ["PAN_ONLY", "CRYPTOGRAM_3DS"]
    var countryCode: String?
    var billingAddressRequired = false
}

nonisolated struct ApplePayOptions: Equatable {
    var enabled = false
    var merchantId = "merchant.net.payrdr.samples.test"
    var supportedNetworks = ["MASTERCARD", "VISA"]
    var merchantCapabilities = ["CREDIT", "DEBIT", "THREE_D_SECURE"]
    var countryCode: String?
}

nonisolated struct SdkOptions: Equatable {
    var locale: String?
    var theme: ThemePreset = .system
    var showResultScreen = false
    var googlePay = GooglePayOptions()
    var applePay = ApplePayOptions()
    var scanner = false
    var sslPinning = SslPinningOptions()
    var keyProviderUrl: String?
}

/// A run configuration, the document of `qa/README.md`. It names a stored profile and never carries credentials.
nonisolated struct QaConfig: Equatable {
    var profile: String?
    var order = OrderConfig()
    var sdk = SdkOptions()
}
