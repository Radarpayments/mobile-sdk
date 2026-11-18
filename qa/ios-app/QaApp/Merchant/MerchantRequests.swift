import Foundation

private nonisolated let doneUrl = "sdk://done"
private nonisolated let withoutToCard = "WITHOUT_TO_CARD"
private nonisolated let orderNumberDigits = 20

/// One call to the gateway, as it is sent and as the journal shows it.
nonisolated enum GatewayRequest: CustomStringConvertible, CustomDebugStringConvertible, CustomReflectable {
    case form(url: String, parameters: [(String, String)])
    case json(url: String, body: JSONObject, apiKey: String? = nil)

    static let journalLimit = 4000
    static let cutMarker = "…"
    static let apiKeyHeader = "X-API-KEY"

    var url: String {
        switch self {
        case let .form(url, _), let .json(url, _, _): url
        }
    }

    /// The journal line: the form as typed rather than percent-encoded, the password and the API key as `***`.
    func printed(_ secrets: GatewaySecrets) -> String {
        let headers: String
        let body: String
        switch self {
        case let .form(_, parameters):
            headers = ""
            body = parameters.map { name, value in "\(name)=\(sameText(name, "password") ? GatewaySecrets.mask : value)" }
                .joined(separator: "&")
        case let .json(_, object, apiKey):
            headers = apiKey == nil ? "" : " \(Self.apiKeyHeader): \(GatewaySecrets.mask)"
            var shown = object
            if shown["password"] != nil { shown["password"] = .string(GatewaySecrets.mask) }
            body = JSONValue.object(shown).text
        }
        return secrets.bounded("→ POST \(url)\(headers) \(body)", limit: Self.journalLimit, cutMarker: Self.cutMarker)
    }

    // The parameters carry the password: a print of the request names only where it goes.
    var description: String {
        "GatewayRequest(\(url))"
    }

    var debugDescription: String {
        description
    }

    var customMirror: Mirror {
        Mirror(self, children: [], displayStyle: .enum)
    }
}

/// A number the gateway has not seen: it rejects a second order with the same number for the merchant.
nonisolated func newOrderNumber() -> String {
    let hex = UUID().uuidString.lowercased().unicodeScalars.filter { $0 != "-" }
    return "qa-" + stringOf(hex.prefix(orderNumberDigits))
}

nonisolated func gatewayBase(_ profile: StandProfile) -> String {
    var scalars = Array(trimmed(profile.gatewayUrl).unicodeScalars)
    while scalars.last == "/" {
        scalars.removeLast()
    }
    return stringOf(scalars)
}

/// The API v2 root sits on the gateway's host, not under its path: `https://host/payment` → `https://host/api2`.
nonisolated func api2Url(_ baseUrl: String) -> String? {
    guard let url = URLComponents(string: trimmed(baseUrl)),
          let scheme = url.scheme, let host = url.host, !host.isEmpty
    else { return nil }
    let defaultPort = scheme == "https" ? 443 : scheme == "http" ? 80 : nil
    let port = url.port.flatMap { $0 == defaultPort ? nil : ":\($0)" } ?? ""
    return "\(scheme)://\(host)\(port)/api2"
}

nonisolated func registerUrl(_ profile: StandProfile) -> String {
    "\(gatewayBase(profile))/rest/register.do"
}

nonisolated func transferUrl(_ profile: StandProfile) -> String {
    "\(gatewayBase(profile))/rest/api/p2p/registerP2P.do"
}

nonisolated func sessionUrl(_ profile: StandProfile) -> String? {
    api2Url(profile.gatewayUrl).map { "\($0)/sessions" }
}

nonisolated func statusUrl(_ profile: StandProfile) -> String {
    "\(gatewayBase(profile))/rest/getOrderStatusExtended.do"
}

/// `register.do`. [numericCurrency] is the order's currency, which the caller has looked up.
nonisolated func registerForm(
    _ order: OrderConfig,
    _ profile: StandProfile,
    orderNumber: String,
    numericCurrency: String
) -> [(String, String)] {
    var fields: [(String, String)] = [
        ("userName", profile.login),
        ("password", profile.password),
        ("amount", String(order.amount)),
        ("currency", numericCurrency),
        ("orderNumber", orderNumber),
        ("returnUrl", doneUrl),
    ]
    if let clientId = order.clientId, !isBlank(clientId) { fields.append(("clientId", clientId)) }
    // ⚠ Unconfirmed on a stand: register.do is assumed to take the features as one comma-separated parameter.
    if !order.features.isEmpty { fields.append(("features", order.features.joined(separator: ","))) }
    for (name, value) in order.extra {
        let position = fields.firstIndex { sameText($0.0, name) }
        switch (value, position) {
        case (.null, let position?): fields.remove(at: position)
        case (.null, nil): break
        case (_, let position?): fields[position].1 = formText(value)
        case (_, nil): fields.append((name, formText(value)))
        }
    }
    return fields
}

/// `registerP2P.do`, a one-leg transfer: the gateway requires WITHOUT_TO_CARD and the "A" indicator for it.
nonisolated func transferJson(
    _ order: OrderConfig,
    _ profile: StandProfile,
    orderNumber: String,
    numericCurrency: String
) -> JSONObject {
    var features: [String] = []
    for feature in [withoutToCard] + order.features where !features.contains(where: { sameText($0, feature) }) {
        features.append(feature)
    }
    var body = JSONObject()
    body["username"] = .string(profile.login)
    body["password"] = .string(profile.password)
    body["amount"] = .number(String(order.amount))
    body["currency"] = .string(numericCurrency)
    body["orderNumber"] = .string(orderNumber)
    body["returnUrl"] = .string(doneUrl)
    body["transactionTypeIndicator"] = .string("A")
    body["features"] = .object(["feature": .array(features.map(JSONValue.string))])
    if let clientId = order.clientId, !isBlank(clientId) { body["clientId"] = .string(clientId) }
    return withExtra(body, order.extra)
}

/// `POST {api2}/sessions`; the API key goes in a header, not here.
nonisolated func sessionJson(_ order: OrderConfig) -> JSONObject {
    var body = JSONObject()
    body["amount"] = .number(String(order.amount))
    body["currency"] = .string(order.currency)
    body["successUrl"] = .string(doneUrl)
    body["failureUrl"] = .string(doneUrl)
    if let clientId = order.clientId, !isBlank(clientId) { body["merchantCustomerId"] = .string(clientId) }
    // ⚠ Unconfirmed on a stand: sessions are assumed to take the features as a plain array of names.
    if !order.features.isEmpty { body["features"] = .array(order.features.map(JSONValue.string)) }
    return withExtra(body, order.extra)
}

/// `getOrderStatusExtended.do`. ⚠ Unconfirmed on a stand: the order is assumed to be named by `orderId` alone.
nonisolated func statusForm(_ orderId: String, _ profile: StandProfile) -> [(String, String)] {
    [("userName", profile.login), ("password", profile.password), ("orderId", orderId)]
}

/// The form as it goes on the wire. `+`, `&` and `=` are taken out of the allowed set: left raw, a value with them
/// would arrive as a space or split into another parameter.
nonisolated func formBody(_ parameters: [(String, String)]) -> Data {
    var allowed = CharacterSet.urlQueryAllowed
    allowed.remove(charactersIn: "+&=")
    let encode = { (text: String) in text.addingPercentEncoding(withAllowedCharacters: allowed) ?? "" }
    return Data(parameters.map { "\(encode($0.0))=\(encode($0.1))" }.joined(separator: "&").utf8)
}

// A form has only text: a JSON string goes without its quotes, any other value as its JSON text.
nonisolated private func formText(_ value: JSONValue) -> String {
    switch value {
    case let .string(text): text
    default: value.text
    }
}

// A replaced key keeps its position, so the journal shows the request in its usual order. A null in extra is how QA
// leaves a parameter out.
nonisolated private func withExtra(_ body: JSONObject, _ extra: JSONObject) -> JSONObject {
    var result = body
    for (name, value) in extra {
        result[name] = value == .null ? nil : value
    }
    return result
}
