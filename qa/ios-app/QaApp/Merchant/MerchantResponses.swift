import Foundation

/// What registering an order gave: the ids the SDK opens the checkout with, or why there are none.
///
/// `register.do` and `registerP2P.do` give `mdOrder`; a session gives `sessionId` and, when it decodes, `mdOrder`. A
/// failure's `httpStatus` is nil when no answer came; its message carries no secret and is bounded in length.
nonisolated enum Registration: Equatable {
    case registered(mdOrder: String?, sessionId: String?)
    case failed(httpStatus: Int?, message: String)
}

/// The order's state as the merchant's backend sees it after the checkout. `notQueried`: API v2 has no status call for
/// the merchant, or there were no credentials or no order to ask about.
nonisolated enum OrderStatus: Equatable {
    case known(orderStatus: Int?, actionCode: Int?, actionCodeDescription: String?)
    case failed(httpStatus: Int?, message: String)
    case notQueried

    /// The result screen's text (`qa_result_order_status`).
    var text: String {
        switch self {
        case let .known(orderStatus, actionCode, description):
            (["orderStatus=\(orderStatus.map(String.init) ?? "-")", "actionCode=\(actionCode.map(String.init) ?? "-")"]
                + [description].compactMap { $0 }).joined(separator: ", ")
        case let .failed(_, message): "error: \(message)"
        case .notQueried: "not queried"
        }
    }
}

private nonisolated let messageLimit = 300
private nonisolated let base58 = Array("123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".unicodeScalars)

/// `register.do` / `registerP2P.do`: the order id on success, the gateway's error otherwise.
nonisolated func registerAnswer(_ status: Int, _ body: String, _ secrets: GatewaySecrets) -> Registration {
    let json = jsonObject(of: body)
    if isSuccess(status), let orderId = json?.text("orderId") { return .registered(mdOrder: orderId, sessionId: nil) }
    return .failed(httpStatus: status, message: json?.gatewayError(secrets) ?? httpFailure(status, body, secrets))
}

/// `POST {api2}/sessions`: the session id on success, `error.errorMessage` otherwise.
nonisolated func sessionAnswer(_ status: Int, _ body: String, _ secrets: GatewaySecrets) -> Registration {
    let json = jsonObject(of: body)
    if isSuccess(status), let id = json?.text("id") { return .registered(mdOrder: mdOrder(ofSession: id), sessionId: id) }
    var error: JSONObject?
    if case let .object(object)? = json?["error"] { error = object }
    return .failed(httpStatus: status, message: error?.gatewayError(secrets) ?? httpFailure(status, body, secrets))
}

/// `getOrderStatusExtended.do`. An answer with none of the three fields is not a status, whatever its HTTP code.
nonisolated func statusAnswer(_ status: Int, _ body: String, _ secrets: GatewaySecrets) -> OrderStatus {
    let json = jsonObject(of: body)
    if let error = json?.gatewayError(secrets) { return .failed(httpStatus: status, message: error) }
    if let json, isSuccess(status) {
        let orderStatus = json.text("orderStatus").flatMap(int32)
        let actionCode = json.text("actionCode").flatMap(int32)
        let description = json.text("actionCodeDescription").map { secrets.bounded($0, limit: messageLimit) }
        if orderStatus != nil || actionCode != nil || description != nil {
            return .known(orderStatus: orderStatus, actionCode: actionCode, actionCodeDescription: description)
        }
    }
    return .failed(httpStatus: status, message: httpFailure(status, body, secrets))
}

/// The `mdOrder` a session id of API v2 carries: `ps_` and the Base58 of `ps:<mdOrder>`; nil for any other id.
nonisolated func mdOrder(ofSession sessionId: String) -> String? {
    let prefix = "ps_".unicodeScalars
    guard sessionId.unicodeScalars.starts(with: prefix) else { return nil }
    let encoded = sessionId.unicodeScalars.dropFirst(prefix.count)
    var bytes: [UInt8] = []
    for scalar in encoded {
        guard var carry = base58.firstIndex(of: scalar) else { return nil }
        for index in bytes.indices {
            carry += Int(bytes[index]) * 58
            bytes[index] = UInt8(carry & 0xFF)
            carry >>= 8
        }
        while carry > 0 {
            bytes.append(UInt8(carry & 0xFF))
            carry >>= 8
        }
    }
    let zeros = [UInt8](repeating: 0, count: encoded.prefix { $0 == base58[0] }.count)
    guard let decoded = String(bytes: zeros + bytes.reversed(), encoding: .utf8),
          decoded.unicodeScalars.starts(with: "ps:".unicodeScalars), decoded.unicodeScalars.count > 3
    else { return nil }
    return stringOf(decoded.unicodeScalars.dropFirst(3))
}

// The answer is untrusted: it is read only within the limits of a document, so a deep or huge body is not parsed (and
// fails with its start) instead of exhausting the stack or the memory.
nonisolated private func jsonObject(of body: String) -> JSONObject? {
    guard limitProblem(body) == nil, case let .object(object)? = JSONValue.parse(body) else { return nil }
    return object
}

nonisolated private func isSuccess(_ status: Int) -> Bool {
    (200 ..< 300).contains(status)
}

// Android's Int is 32 bits.
nonisolated private func int32(_ text: String) -> Int? {
    Int32(text).map(Int.init)
}

nonisolated private func httpFailure(_ status: Int, _ body: String, _ secrets: GatewaySecrets) -> String {
    "HTTP \(status): " + (isBlank(body) ? "(empty body)" : secrets.bounded(body, limit: messageLimit))
}

nonisolated private extension JSONObject {
    // A field's text, a number's included; blank as absent, since the gateway sends "orderId": "" with some errors.
    func text(_ name: String) -> String? {
        let text: String? =
            switch self[name] {
            case let .string(value)?: value
            case let .number(value)?: value
            case let .bool(value)?: value ? "true" : "false"
            default: nil
            }
        return text.flatMap { isBlank($0) ? nil : $0 }
    }

    // errorCode "0" is the gateway's "no error"; a message beside it is not a failure reason.
    func gatewayError(_ secrets: GatewaySecrets) -> String? {
        let code = text("errorCode")
        if let code, sameText(code, "0") { return nil }
        let error = text("errorMessage") ?? code.map { "Gateway error \($0)" }
        return error.map { secrets.bounded($0, limit: messageLimit) }
    }
}
