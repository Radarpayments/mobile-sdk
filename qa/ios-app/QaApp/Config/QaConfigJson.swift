/// Reads and writes the run configuration document of `qa/README.md`, the same way the Android app does.
nonisolated enum QaConfigJson {
    static let maxAmount: Int64 = 999_999_999_999
    static let locales = ["en", "ru", "ro", "el", "bg", "de", "fr", "es", "ar"]
    static let googlePayEnvironments = ["test", "production"]
    static let googlePayNetworks = ["AMEX", "DISCOVER", "INTERAC", "JCB", "MASTERCARD", "VISA"]
    static let googlePayAuthMethods = ["PAN_ONLY", "CRYPTOGRAM_3DS"]
    static let applePayNetworks = ["AMEX", "DISCOVER", "JCB", "MASTERCARD", "VISA"]
    static let applePayCapabilities = ["CREDIT", "DEBIT", "EMV", "THREE_D_SECURE"]
    static let forbiddenExtraKeys = ["username", "password", "token"]

    static let pins = "sdk.sslPinning.pins"
    static let extra = "order.extra"
    private static let googlePay = "sdk.googlePay"
    private static let applePay = "sdk.applePay"

    // The table order of the contract; a group path ranks before its members.
    private static let fields = [
        "version",
        "profile",
        "order",
        "order.source",
        "order.type",
        "order.amount",
        "order.currency",
        "order.clientId",
        "order.mdOrder",
        "order.sessionId",
        "order.features",
        extra,
        "sdk",
        "sdk.locale",
        "sdk.theme",
        "sdk.showResultScreen",
        "sdk.scanner",
        "sdk.keyProviderUrl",
        "sdk.sslPinning",
        "sdk.sslPinning.enabled",
        pins,
        googlePay,
        "\(googlePay).enabled",
        "\(googlePay).billingAddressRequired",
        "\(googlePay).merchantId",
        "\(googlePay).merchantFullName",
        "\(googlePay).gateway",
        "\(googlePay).gatewayMerchantId",
        "\(googlePay).countryCode",
        "\(googlePay).environment",
        "\(googlePay).allowedCardNetworks",
        "\(googlePay).allowedAuthMethods",
        applePay,
        "\(applePay).enabled",
        "\(applePay).merchantId",
        "\(applePay).supportedNetworks",
        "\(applePay).merchantCapabilities",
        "\(applePay).countryCode",
    ]

    static func decode(_ text: String) -> Decoded<QaConfig> {
        if let message = limitProblem(text) { return .invalid(Problem(field: "$", message: message)) }
        guard case let .object(root) = JSONValue.parse(text) else { return .invalid(.notAnObject) }
        let reader = JsonReader()
        if let version = reader.integral(root, "version", "version"), version != 1 { reader.fail("version", "must be 1") }
        let config = QaConfig(
            profile: reader.nullableText(root, "profile", "profile"),
            order: readOrder(reader, reader.group(root, "order", "order")),
            sdk: readSdk(reader, reader.group(root, "sdk", "sdk"))
        )
        let problem = firstInOrder(reader.problems + [problem(of: config)].compactMap { $0 }, fields)
        return problem.map { .invalid($0) } ?? .ok(config)
    }

    /// The first rule a typed configuration breaks, by the same rules as `decode`; the form uses it before writing.
    static func problem(of config: QaConfig) -> Problem? {
        let order = config.order
        let sdk = config.sdk
        let gp = sdk.googlePay
        let ap = sdk.applePay
        let problems: [Problem?] = [
            config.profile.flatMap { isProfileName($0) ? nil : profileNameProblem("profile") },
            problemIf(!(0 ... maxAmount).contains(order.amount), "order.amount", "must be between 0 and \(maxAmount)"),
            problemIf(
                !containsText(Currencies.codes, order.currency),
                "order.currency",
                "must be one of \(Currencies.codes.joined(separator: ", "))"
            ),
            problemIf(
                order.features.contains { feature in
                    isBlank(feature) || feature.unicodeScalars.contains { $0 == "," || isBlankSpace($0) }
                },
                "order.features",
                "each feature must be non-blank, without commas and whitespace"
            ),
            extraProblem(order.extra),
            problemIf(
                sdk.locale.map { !containsText(locales, $0) } ?? false,
                "sdk.locale",
                "must be one of \(locales.joined(separator: ", "))"
            ),
            problemIf(
                sdk.keyProviderUrl.map { !isHttpUrl($0) } ?? false,
                "sdk.keyProviderUrl",
                "must be an absolute http or https URL"
            ),
            pinsProblem(sdk.sslPinning.pins),
            problemIf(
                gp.environment.map { !containsText(googlePayEnvironments, $0) } ?? false,
                "\(googlePay).environment",
                "must be test or production"
            ),
            subsetProblem("\(googlePay).allowedCardNetworks", gp.allowedCardNetworks, googlePayNetworks),
            subsetProblem("\(googlePay).allowedAuthMethods", gp.allowedAuthMethods, googlePayAuthMethods),
            problemIf(isBlank(ap.merchantId), "\(applePay).merchantId", "must not be blank"),
            subsetProblem("\(applePay).supportedNetworks", ap.supportedNetworks, applePayNetworks),
            subsetProblem("\(applePay).merchantCapabilities", ap.merchantCapabilities, applePayCapabilities),
        ]
        return problems.lazy.compactMap { $0 }.first
    }

    static func encode(_ config: QaConfig) -> String {
        let order = config.order
        let sdk = config.sdk
        let document: JSONObject = [
            "version": .number("1"),
            "profile": nullable(config.profile),
            "order": .object([
                "source": .string(order.source.rawValue),
                "type": .string(order.type.rawValue),
                "amount": .number(String(order.amount)),
                "currency": .string(order.currency),
                "clientId": nullable(order.clientId),
                "mdOrder": nullable(order.mdOrder),
                "sessionId": nullable(order.sessionId),
                "features": strings(distinct(order.features)),
                "extra": .object(order.extra),
            ]),
            "sdk": .object([
                "locale": nullable(sdk.locale),
                "theme": .string(sdk.theme.rawValue),
                "showResultScreen": .bool(sdk.showResultScreen),
                "scanner": .bool(sdk.scanner),
                "keyProviderUrl": nullable(sdk.keyProviderUrl),
                "sslPinning": .object([
                    "enabled": .bool(sdk.sslPinning.enabled),
                    "pins": .array(sdk.sslPinning.pins.map { .object(["host": .string($0.host), "pin": .string($0.pin)]) }),
                ]),
                "googlePay": .object([
                    "enabled": .bool(sdk.googlePay.enabled),
                    "billingAddressRequired": .bool(sdk.googlePay.billingAddressRequired),
                    "merchantId": nullable(sdk.googlePay.merchantId),
                    "merchantFullName": nullable(sdk.googlePay.merchantFullName),
                    "gateway": nullable(sdk.googlePay.gateway),
                    "gatewayMerchantId": nullable(sdk.googlePay.gatewayMerchantId),
                    "countryCode": nullable(sdk.googlePay.countryCode),
                    "environment": nullable(sdk.googlePay.environment),
                    "allowedCardNetworks": strings(canonical(sdk.googlePay.allowedCardNetworks, googlePayNetworks)),
                    "allowedAuthMethods": strings(canonical(sdk.googlePay.allowedAuthMethods, googlePayAuthMethods)),
                ]),
                "applePay": .object([
                    "enabled": .bool(sdk.applePay.enabled),
                    "merchantId": .string(sdk.applePay.merchantId),
                    "supportedNetworks": strings(canonical(sdk.applePay.supportedNetworks, applePayNetworks)),
                    "merchantCapabilities": strings(canonical(sdk.applePay.merchantCapabilities, applePayCapabilities)),
                    "countryCode": nullable(sdk.applePay.countryCode),
                ]),
            ]),
        ]
        return JSONValue.object(document).prettyText
    }

    private static func readOrder(_ reader: JsonReader, _ source: JSONObject) -> OrderConfig {
        let defaults = OrderConfig()
        return OrderConfig(
            source: reader.choice(source, "source", "order.source", defaults.source),
            type: reader.choice(source, "type", "order.type", defaults.type),
            amount: reader.integral(source, "amount", "order.amount") ?? defaults.amount,
            currency: reader.text(source, "currency", "order.currency") ?? defaults.currency,
            clientId: reader.nullableText(source, "clientId", "order.clientId"),
            features: reader.names(source, "features", "order.features").map(distinct) ?? defaults.features,
            extra: reader.group(source, "extra", extra),
            mdOrder: reader.nullableText(source, "mdOrder", "order.mdOrder"),
            sessionId: reader.nullableText(source, "sessionId", "order.sessionId")
        )
    }

    private static func readSdk(_ reader: JsonReader, _ source: JSONObject) -> SdkOptions {
        let defaults = SdkOptions()
        return SdkOptions(
            locale: reader.nullableText(source, "locale", "sdk.locale"),
            theme: reader.choice(source, "theme", "sdk.theme", defaults.theme),
            showResultScreen: reader.flag(source, "showResultScreen", "sdk.showResultScreen", defaults.showResultScreen),
            googlePay: readGooglePay(reader, reader.group(source, "googlePay", googlePay)),
            applePay: readApplePay(reader, reader.group(source, "applePay", applePay)),
            scanner: reader.flag(source, "scanner", "sdk.scanner", defaults.scanner),
            sslPinning: readPinning(reader, reader.group(source, "sslPinning", "sdk.sslPinning")),
            keyProviderUrl: reader.nullableText(source, "keyProviderUrl", "sdk.keyProviderUrl")
        )
    }

    private static func readPinning(_ reader: JsonReader, _ source: JSONObject) -> SslPinningOptions {
        let enabled = reader.flag(source, "enabled", "sdk.sslPinning.enabled", false)
        var pinList: [Pin] = []
        switch source["pins"] {
        case nil, .null?:
            break
        case let .array(items)?:
            for item in items {
                guard case let .object(pin) = item else {
                    reader.fail(pins, "each pin must be an object")
                    continue
                }
                pinList.append(Pin(host: reader.text(pin, "host", pins) ?? "", pin: reader.text(pin, "pin", pins) ?? ""))
            }
        default:
            reader.fail(pins, "must be a list of objects")
        }
        return SslPinningOptions(enabled: enabled, pins: pinList)
    }

    private static func readGooglePay(_ reader: JsonReader, _ source: JSONObject) -> GooglePayOptions {
        let defaults = GooglePayOptions()
        let path = googlePay
        return GooglePayOptions(
            enabled: reader.flag(source, "enabled", "\(path).enabled", defaults.enabled),
            merchantId: reader.nullableText(source, "merchantId", "\(path).merchantId"),
            merchantFullName: reader.nullableText(source, "merchantFullName", "\(path).merchantFullName"),
            gateway: reader.nullableText(source, "gateway", "\(path).gateway"),
            gatewayMerchantId: reader.nullableText(source, "gatewayMerchantId", "\(path).gatewayMerchantId"),
            environment: reader.nullableText(source, "environment", "\(path).environment"),
            allowedCardNetworks: reader.names(source, "allowedCardNetworks", "\(path).allowedCardNetworks")
                .map { canonical($0, googlePayNetworks) } ?? defaults.allowedCardNetworks,
            allowedAuthMethods: reader.names(source, "allowedAuthMethods", "\(path).allowedAuthMethods")
                .map { canonical($0, googlePayAuthMethods) } ?? defaults.allowedAuthMethods,
            countryCode: reader.nullableText(source, "countryCode", "\(path).countryCode"),
            billingAddressRequired: reader.flag(
                source, "billingAddressRequired", "\(path).billingAddressRequired", defaults.billingAddressRequired
            )
        )
    }

    private static func readApplePay(_ reader: JsonReader, _ source: JSONObject) -> ApplePayOptions {
        let defaults = ApplePayOptions()
        let path = applePay
        return ApplePayOptions(
            enabled: reader.flag(source, "enabled", "\(path).enabled", defaults.enabled),
            merchantId: reader.text(source, "merchantId", "\(path).merchantId") ?? defaults.merchantId,
            supportedNetworks: reader.names(source, "supportedNetworks", "\(path).supportedNetworks")
                .map { canonical($0, applePayNetworks) } ?? defaults.supportedNetworks,
            merchantCapabilities: reader.names(source, "merchantCapabilities", "\(path).merchantCapabilities")
                .map { canonical($0, applePayCapabilities) } ?? defaults.merchantCapabilities,
            countryCode: reader.nullableText(source, "countryCode", "\(path).countryCode")
        )
    }
}

nonisolated func profileNameProblem(_ field: String) -> Problem {
    Problem(field: field, message: "must be 1 to 32 characters of a-z, 0-9, _ and -")
}

nonisolated func isPinValid(_ pin: Pin) -> Bool {
    isPinHost(pin.host) && isPinValue(pin.pin)
}

nonisolated func pinsProblem(_ pins: [Pin]) -> Problem? {
    pins.allSatisfy(isPinValid)
        ? nil
        : Problem(field: QaConfigJson.pins, message: "each pin needs a hostname and a 44 character base64 SHA-256 pin")
}

nonisolated func extraProblem(_ extra: JSONObject) -> Problem? {
    let forbidden = QaConfigJson.forbiddenExtraKeys
    let clean = extra.keys.allSatisfy { key in !forbidden.contains { equalsIgnoringCase(key, $0) } }
    return clean ? nil : Problem(field: QaConfigJson.extra, message: "must not contain \(forbidden.joined(separator: ", "))")
}

/// Whether [values] holds [text], comparing scalars as Android compares strings.
nonisolated func containsText(_ values: [String], _ text: String) -> Bool {
    values.contains { sameText($0, text) }
}

/// [values] in reading order without repeats.
nonisolated func distinct(_ values: [String]) -> [String] {
    var seen: Set<[UInt8]> = []
    return values.filter { seen.insert(Array($0.utf8)).inserted }
}

// Unknown members are kept at the end so that `problem(of:)` still sees and reports them.
nonisolated private func canonical(_ values: [String], _ allowed: [String]) -> [String] {
    allowed.filter { containsText(values, $0) } + distinct(values.filter { !containsText(allowed, $0) })
}

nonisolated private func subsetProblem(_ field: String, _ values: [String], _ allowed: [String]) -> Problem? {
    problemIf(values.contains { !containsText(allowed, $0) }, field, "must be a subset of \(allowed.joined(separator: ", "))")
}

nonisolated private func problemIf(_ failed: Bool, _ field: String, _ message: String) -> Problem? {
    failed ? Problem(field: field, message: message) : nil
}

nonisolated private func nullable(_ text: String?) -> JSONValue {
    text.map(JSONValue.string) ?? .null
}

nonisolated private func strings(_ values: [String]) -> JSONValue {
    .array(values.map(JSONValue.string))
}
