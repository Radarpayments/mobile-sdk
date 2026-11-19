import Combine

/// The configuration form's state: one text per field of `Tags.fields`. An edit that parses becomes `config` at once; one
/// that does not stays a draft with its `Problem`, and `config` keeps the last value in which every field parsed, so that
/// the store and a launch never see a half-typed field.
@MainActor
final class ConfigForm: ObservableObject {
    @Published private(set) var config: QaConfig

    // Every edited field keeps its text until the next set(), even a valid one: "VERIFY," parses to the same features
    // as "VERIFY", and showing the latter would eat the comma being typed.
    @Published private var drafts: [String: String] = [:]
    @Published private var problems: [String: Problem] = [:]

    init(_ initial: QaConfig) {
        config = initial
    }

    /// The first pending problem in the order of the form.
    var problem: Problem? {
        Tags.fields.lazy.compactMap { self.problems[$0] }.first
    }

    /// The pending problem of the field `tag`, nil when its text parsed.
    func problem(_ tag: String) -> Problem? {
        problems[tag]
    }

    func text(_ tag: String) -> String {
        drafts[tag] ?? field(tag).read(config)
    }

    func edit(_ tag: String, _ text: String) {
        drafts[tag] = text
        let applied: Decoded<QaConfig> = switch field(tag).write(config, text) {
        case let .ok(written): checked(written)
        case let .invalid(problem): .invalid(problem)
        }
        switch applied {
        case let .ok(config):
            self.config = config
            problems[tag] = nil
        case let .invalid(problem):
            problems[tag] = problem
        }
    }

    func set(_ config: QaConfig) {
        drafts = [:]
        problems = [:]
        self.config = config
    }
}

private struct Field {
    let read: (QaConfig) -> String
    let write: (QaConfig, String) -> Decoded<QaConfig>
}

// A written field goes through the codec, which applies every rule of the contract (sizes, sets, patterns) and puts sets
// in the order of the contract, so the form accepts exactly what an import would.
private func checked(_ config: QaConfig) -> Decoded<QaConfig> {
    QaConfigJson.decode(QaConfigJson.encode(config))
}

private func field(_ tag: String) -> Field {
    guard let field = fields[tag] else { preconditionFailure("\(tag) is not a field of the configuration") }
    return field
}

private extension Decoded {
    func map<R>(_ transform: (T) -> R) -> Decoded<R> {
        switch self {
        case let .ok(value): .ok(transform(value))
        case let .invalid(problem): .invalid(problem)
        }
    }
}

private func order(
    _ read: @escaping (OrderConfig) -> String,
    _ write: @escaping (OrderConfig, String) -> Decoded<OrderConfig>
) -> Field {
    Field(read: { read($0.order) }, write: { config, text in
        write(config.order, text).map { var copy = config; copy.order = $0; return copy }
    })
}

private func sdk(
    _ read: @escaping (SdkOptions) -> String,
    _ write: @escaping (SdkOptions, String) -> Decoded<SdkOptions>
) -> Field {
    Field(read: { read($0.sdk) }, write: { config, text in
        write(config.sdk, text).map { var copy = config; copy.sdk = $0; return copy }
    })
}

private func applePay(
    _ read: @escaping (ApplePayOptions) -> String,
    _ write: @escaping (ApplePayOptions, String) -> Decoded<ApplePayOptions>
) -> Field {
    sdk({ read($0.applePay) }, { sdk, text in
        write(sdk.applePay, text).map { var copy = sdk; copy.applePay = $0; return copy }
    })
}

private func ok(_ text: String) -> Decoded<String?> {
    .ok(isBlank(text) ? nil : text)
}

// The form reads the amount in major units of the order's currency; the document keeps minor units.
private func amount(_ text: String, _ currency: String) -> Decoded<Int64> {
    let digits = Currencies.minorDigits(currency)
    guard let minor = majorToMinor(text, digits: digits) else {
        return .invalid(Problem(field: "order.amount", message: "must be digits with at most \(digits) after the point"))
    }
    guard minor <= QaConfigJson.maxAmount else {
        return .invalid(
            Problem(field: "order.amount", message: "must be at most \(minorToMajor(QaConfigJson.maxAmount, digits: digits))")
        )
    }
    return .ok(minor)
}

private func flag(_ path: String, _ text: String) -> Decoded<Bool> {
    if sameText(text, "true") { return .ok(true) }
    if sameText(text, "false") { return .ok(false) }
    return .invalid(Problem(field: path, message: "must be true or false"))
}

private func choice<T: RawRepresentable & CaseIterable>(_ path: String, _ text: String, _: T.Type) -> Decoded<T>
    where T.RawValue == String {
    if let value = T.allCases.first(where: { sameText($0.rawValue, text) }) { return .ok(value) }
    let names = T.allCases.map(\.rawValue).joined(separator: ", ")
    return .invalid(Problem(field: path, message: "must be one of \(names)"))
}

private let fields: [String: Field] = [
    Tags.profile: Field(read: { $0.profile ?? "" }, write: { config, text in
        ok(text).map { var copy = config; copy.profile = $0; return copy }
    }),
    Tags.orderSource: order({ $0.source.rawValue }) { order, text in
        choice("order.source", text, OrderSource.self).map { var copy = order; copy.source = $0; return copy }
    },
    Tags.orderType: order({ $0.type.rawValue }) { order, text in
        choice("order.type", text, OrderType.self).map { var copy = order; copy.type = $0; return copy }
    },
    Tags.orderAmount: order({ minorToMajor($0.amount, digits: Currencies.minorDigits($0.currency)) }) { order, text in
        amount(text, order.currency).map { var copy = order; copy.amount = $0; return copy }
    },
    Tags.orderCurrency: order({ $0.currency }) { order, text in
        var copy = order
        copy.currency = text
        return .ok(copy)
    },
    Tags.orderClientId: order({ $0.clientId ?? "" }) { order, text in
        ok(text).map { var copy = order; copy.clientId = $0; return copy }
    },
    Tags.orderMdOrder: order({ $0.mdOrder ?? "" }) { order, text in
        ok(text).map { var copy = order; copy.mdOrder = $0; return copy }
    },
    Tags.orderSessionId: order({ $0.sessionId ?? "" }) { order, text in
        ok(text).map { var copy = order; copy.sessionId = $0; return copy }
    },
    Tags.orderFeatures: order({ namesToText($0.features) }) { order, text in
        var copy = order
        copy.features = distinct(namesFromText(text))
        return .ok(copy)
    },
    Tags.orderExtra: order({ extraToText($0.extra) }) { order, text in
        extraFromText(text).map { var copy = order; copy.extra = $0; return copy }
    },
    Tags.sdkLocale: sdk({ $0.locale ?? "" }) { sdk, text in
        ok(text).map { var copy = sdk; copy.locale = $0; return copy }
    },
    Tags.sdkTheme: sdk({ $0.theme.rawValue }) { sdk, text in
        choice("sdk.theme", text, ThemePreset.self).map { var copy = sdk; copy.theme = $0; return copy }
    },
    Tags.sdkShowResultScreen: sdk({ String($0.showResultScreen) }) { sdk, text in
        flag("sdk.showResultScreen", text).map { var copy = sdk; copy.showResultScreen = $0; return copy }
    },
    Tags.sdkScanner: sdk({ String($0.scanner) }) { sdk, text in
        flag("sdk.scanner", text).map { var copy = sdk; copy.scanner = $0; return copy }
    },
    Tags.sdkKeyProviderUrl: sdk({ $0.keyProviderUrl ?? "" }) { sdk, text in
        ok(text).map { var copy = sdk; copy.keyProviderUrl = $0; return copy }
    },
    Tags.sdkSslPinningEnabled: sdk({ String($0.sslPinning.enabled) }) { sdk, text in
        flag("sdk.sslPinning.enabled", text).map { var copy = sdk; copy.sslPinning.enabled = $0; return copy }
    },
    Tags.sdkSslPinningPins: sdk({ pinsToText($0.sslPinning.pins) }) { sdk, text in
        pinsFromText(text).map { var copy = sdk; copy.sslPinning.pins = $0; return copy }
    },
    Tags.sdkApplePayEnabled: applePay({ String($0.enabled) }) { options, text in
        flag("sdk.applePay.enabled", text).map { var copy = options; copy.enabled = $0; return copy }
    },
    Tags.sdkApplePayMerchantId: applePay({ $0.merchantId }) { options, text in
        var copy = options
        copy.merchantId = text
        return .ok(copy)
    },
    Tags.sdkApplePaySupportedNetworks: applePay({ namesToText($0.supportedNetworks) }) { options, text in
        var copy = options
        copy.supportedNetworks = namesFromText(text)
        return .ok(copy)
    },
    Tags.sdkApplePayMerchantCapabilities: applePay({ namesToText($0.merchantCapabilities) }) { options, text in
        var copy = options
        copy.merchantCapabilities = namesFromText(text)
        return .ok(copy)
    },
    Tags.sdkApplePayCountryCode: applePay({ $0.countryCode ?? "" }) { options, text in
        ok(text).map { var copy = options; copy.countryCode = $0; return copy }
    },
]
