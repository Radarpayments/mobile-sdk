import Foundation

/// "Your server": registers an order at the gateway and hands back its mdOrder (API V1) or its session (API V2).
///
/// In a real integration this code lives on the merchant's backend, which keeps the merchant
/// login and password or API key; the app only receives the order. The demo registers orders from the app
/// so that it works without a backend — never ship merchant credentials inside an app.
protocol OrderRegistrar {
    /// Registers an order for `amountMinor` euro cents; `clientId` links it to a buyer's saved cards, nil = guest.
    func register(amountMinor: Int64, clientId: String?) async -> OrderRegistration
}

/// Outcome of `OrderRegistrar.register`.
enum OrderRegistration: Equatable {
    /// The gateway accepted the order: pass `sessionId` to the SDK when there is one (`presentSession`, API V2),
    /// `mdOrder` otherwise (`present`, API V1). An API V2 session has an mdOrder too, the one its id encodes.
    case registered(mdOrder: String, sessionId: String? = nil)
    /// Login or password is not set in the developer settings; nothing was sent.
    case missingCredentials
    /// The API V2 key is not set in the developer settings; nothing was sent.
    case missingApiKey
    /// The gateway refused the order or could not be reached.
    case failed(message: String)
}

/// The merchant API a demo order is registered through.
enum ApiVersion: String {
    case v1 = "V1"
    case v2 = "V2"
}

/// What a demo order is: a payment, or a one-leg AFT transfer (API V1 `registerP2P.do`).
enum OrderType: String {
    case purchase = "PURCHASE"
    case aft = "AFT"
}

/// Details a transfer is registered with so that the sheet does not ask for them: the buyer's `email`, and
/// `billingPayerData` / `billingRecipientData` as `registerP2P.do` takes them (gateway field name → value).
struct TransferPrefill: Equatable {
    let email: String?
    let billingPayerData: [String: String]
    let billingRecipientData: [String: String]
}

/// The registrar for the API version and order type chosen in the developer settings; transfers go through API V1 and
/// carry `prefill` when there is one.
func merchantRegistrar(apiVersion: ApiVersion, baseUrl: String, login: String, password: String, apiKey: String,
                       orderType: OrderType = .purchase, prefill: TransferPrefill? = nil, session: URLSession = .shared) -> OrderRegistrar {
    switch apiVersion {
    case .v1:
        return GatewayOrderRegistrar(baseUrl: baseUrl, login: login, password: password, session: session, orderType: orderType,
                                     prefill: prefill)
    case .v2: return ApiV2SessionRegistrar(baseUrl: baseUrl, apiKey: apiKey, session: session)
    }
}

/// Registers orders through API V1 of the gateway at `baseUrl`, in euros: a payment with `register.do`, an AFT transfer
/// with `registerP2P.do` (`orderType`).
struct GatewayOrderRegistrar: OrderRegistrar, CustomStringConvertible {
    let baseUrl: String
    let login: String
    let password: String
    var session: URLSession = .shared
    var timeout: TimeInterval = 20
    var orderType: OrderType = .purchase
    var prefill: TransferPrefill?

    var description: String { "GatewayOrderRegistrar(baseUrl: \(baseUrl), orderType: \(orderType))" }

    func register(amountMinor: Int64, clientId: String?) async -> OrderRegistration {
        guard !login.isEmpty, !password.isEmpty else { return .missingCredentials }
        if orderType == .aft { return await registerTransfer(amountMinor: amountMinor, clientId: clientId) }
        guard let url = gatewayUrl("/rest/register.do") else { return .failed(message: "Bad gateway URL") }
        var request = URLRequest(url: url, timeoutInterval: timeout)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.httpBody = Data(
            registerRequestBody(
                amountMinor: amountMinor,
                clientId: clientId,
                login: login,
                password: password,
                orderNumber: newOrderNumber()
            ).utf8
        )
        return await callGateway(request, session: session, parse: parseRegisterResponse)
    }

    /// A one-leg AFT transfer (`WITHOUT_TO_CARD`, transaction type `A`): the buyer's card is debited, the money goes to
    /// the recipient; the sheet asks for the sender and recipient details the gateway needs.
    ///
    /// A transfer of 0 only saves the buyer's card for `clientId`: the gateway checks the card and reverses (`VERIFY`),
    /// and refuses `VERIFY` without `FORCE_CREATE_BINDING`.
    private func registerTransfer(amountMinor: Int64, clientId: String?) async -> OrderRegistration {
        guard let url = gatewayUrl("/rest/api/p2p/registerP2P.do") else { return .failed(message: "Bad gateway URL") }
        let features = amountMinor == 0 ? ["WITHOUT_TO_CARD", "VERIFY", "FORCE_CREATE_BINDING"] : ["WITHOUT_TO_CARD"]
        var body: [String: Any] = [
            "username": login,
            "password": password,
            "amount": amountMinor,
            "currency": "978",
            "orderNumber": newOrderNumber(),
            "returnUrl": returnUrl,
            "transactionTypeIndicator": "A",
            "features": ["feature": features],
        ]
        if let clientId { body["clientId"] = clientId }
        if let prefill {
            if let email = prefill.email { body["email"] = email }
            if !prefill.billingPayerData.isEmpty { body["billingPayerData"] = prefill.billingPayerData }
            if !prefill.billingRecipientData.isEmpty { body["billingRecipientData"] = prefill.billingRecipientData }
        }
        var request = URLRequest(url: url, timeoutInterval: timeout)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)
        return await callGateway(request, session: session, parse: parseRegisterResponse)
    }

    private func gatewayUrl(_ path: String) -> URL? {
        URL(string: baseUrl.trimmingCharacters(in: CharacterSet(charactersIn: "/")) + path)
    }

    // Gateways limit orderNumber to 32 characters; a full UUID with the prefix is 41.
    private func newOrderNumber() -> String {
        "demo-" + UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased().prefix(20)
    }
}

/// Creates API V2 sessions (`POST /api2/sessions`) at the gateway of `baseUrl` with the merchant's `apiKey`, in euros.
/// The buyer comes back from the bank page to `sdk://done`, which the SDK expects (the merchant's own pages work too).
struct ApiV2SessionRegistrar: OrderRegistrar, CustomStringConvertible {
    let baseUrl: String
    let apiKey: String
    var session: URLSession = .shared
    var timeout: TimeInterval = 20

    var description: String { "ApiV2SessionRegistrar(baseUrl: \(baseUrl))" }

    func register(amountMinor: Int64, clientId: String?) async -> OrderRegistration {
        guard !apiKey.trimmingCharacters(in: .whitespaces).isEmpty else { return .missingApiKey }
        guard let api2 = api2Url(baseUrl), let url = URL(string: api2 + "/sessions") else {
            return .failed(message: "Bad gateway URL")
        }
        var body: [String: Any] = [
            "amount": amountMinor,
            "currency": "EUR",
            "successUrl": returnUrl,
            "failureUrl": returnUrl,
        ]
        // API V2's name for the buyer whose cards the gateway keeps (API V1's clientId).
        if let clientId { body["merchantCustomerId"] = clientId }
        var request = URLRequest(url: url, timeoutInterval: timeout)
        request.httpMethod = "POST"
        request.setValue(apiKey, forHTTPHeaderField: "X-API-KEY")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)
        return await callGateway(request, session: session, parse: parseSessionResponse)
    }
}

/// One gateway call of "your server": the answer read by `parse`, whatever its HTTP status; no answer is a failure.
private func callGateway(_ request: URLRequest, session: URLSession, parse: (Data) -> OrderRegistration) async -> OrderRegistration {
    do {
        let (data, _) = try await session.data(for: request)
        return parse(data)
    } catch {
        return .failed(message: error.localizedDescription)
    }
}

private let returnUrl = "sdk://done"

/// The url-encoded form of a `register.do` call.
func registerRequestBody(amountMinor: Int64, clientId: String?, login: String, password: String, orderNumber: String) -> String {
    var items = [
        URLQueryItem(name: "userName", value: login),
        URLQueryItem(name: "password", value: password),
        URLQueryItem(name: "amount", value: String(amountMinor)),
        URLQueryItem(name: "currency", value: "978"),
        URLQueryItem(name: "orderNumber", value: orderNumber),
        URLQueryItem(name: "returnUrl", value: returnUrl),
    ]
    if let clientId { items.append(URLQueryItem(name: "clientId", value: clientId)) }
    // Form encoding: everything but unreserved characters is escaped, "+" included.
    let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "-._~"))
    return items
        .map { "\($0.name)=\(($0.value ?? "").addingPercentEncoding(withAllowedCharacters: allowed) ?? "")" }
        .joined(separator: "&")
}

/// Reads a `register.do` / `registerP2P.do` response: `orderId` on success, `errorMessage` otherwise.
func parseRegisterResponse(_ data: Data) -> OrderRegistration {
    guard let json = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else {
        return .failed(message: "Unexpected gateway response")
    }
    if let orderId = json["orderId"] as? String { return .registered(mdOrder: orderId) }
    let code = json["errorCode"].map { "\($0)" } ?? ""
    return .failed(message: json["errorMessage"] as? String ?? "Gateway error \(code)")
}

/// API V2 is served next to the payment page: `https://host/payment` → `https://host/api2`; nil for a URL without a host.
func api2Url(_ baseUrl: String) -> String? {
    guard let url = URLComponents(string: baseUrl.trimmingCharacters(in: .whitespaces)),
          let scheme = url.scheme, let host = url.host, !host.isEmpty else { return nil }
    let defaultPort = scheme == "https" ? 443 : scheme == "http" ? 80 : nil
    let port = url.port.flatMap { $0 == defaultPort ? nil : ":\($0)" } ?? ""
    return "\(scheme)://\(host)\(port)/api2"
}

/// Reads a `POST /api2/sessions` response: the session on success, `error.errorMessage` otherwise.
func parseSessionResponse(_ data: Data) -> OrderRegistration {
    guard let json = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else {
        return .failed(message: "Unexpected gateway response")
    }
    if let id = json["id"] as? String {
        guard let mdOrder = mdOrder(ofSession: id) else { return .failed(message: "Unexpected session id \(id)") }
        return .registered(mdOrder: mdOrder, sessionId: id)
    }
    let error = json["error"] as? [String: Any]
    let code = error?["errorCode"].map { "\($0)" } ?? ""
    return .failed(message: error?["errorMessage"] as? String ?? "Gateway error \(code)")
}

/// The mdOrder an API V2 session id encodes: the id is `ps_` + Base58 of `ps:` + mdOrder. Nil for anything else.
func mdOrder(ofSession sessionId: String) -> String? {
    guard sessionId.hasPrefix("ps_") else { return nil }
    let encoded = sessionId.dropFirst(3)
    let alphabet = Array("123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz")
    // The number in base 256, least significant byte first.
    var bytes: [UInt8] = []
    for char in encoded {
        guard var carry = alphabet.firstIndex(of: char) else { return nil }
        for i in bytes.indices {
            carry += Int(bytes[i]) * 58
            bytes[i] = UInt8(carry & 0xFF)
            carry >>= 8
        }
        while carry > 0 {
            bytes.append(UInt8(carry & 0xFF))
            carry >>= 8
        }
    }
    // Each leading "1" is a leading zero byte.
    let zeros = [UInt8](repeating: 0, count: encoded.prefix(while: { $0 == "1" }).count)
    guard let decoded = String(bytes: zeros + bytes.reversed(), encoding: .utf8), decoded.hasPrefix("ps:"),
          decoded.count > 3 else { return nil }
    return String(decoded.dropFirst(3))
}
