import XCTest
@testable import SampleApp

/// Answers every request with `StubProtocol.response` and records the requests it saw.
final class StubProtocol: URLProtocol {
    static var response: (Data, Int) = (Data(#"{"orderId":"md-1","formUrl":"https://x"}"#.utf8), 200)
    static var error: Error?
    static var requests: [URLRequest] = []
    static var bodies: [String] = []

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        StubProtocol.requests.append(request)
        StubProtocol.bodies.append(Self.body(of: request))
        if let error = StubProtocol.error {
            client?.urlProtocol(self, didFailWithError: error)
            return
        }
        let http = HTTPURLResponse(url: request.url!, statusCode: StubProtocol.response.1, httpVersion: nil, headerFields: nil)!
        client?.urlProtocol(self, didReceive: http, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: StubProtocol.response.0)
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}

    private static func body(of request: URLRequest) -> String {
        if let data = request.httpBody { return String(decoding: data, as: UTF8.self) }
        guard let stream = request.httpBodyStream else { return "" }
        stream.open()
        defer { stream.close() }
        var data = Data()
        var buffer = [UInt8](repeating: 0, count: 1024)
        while stream.hasBytesAvailable {
            let read = stream.read(&buffer, maxLength: buffer.count)
            if read <= 0 { break }
            data.append(buffer, count: read)
        }
        return String(decoding: data, as: UTF8.self)
    }
}

final class OrderRegistrarTests: XCTestCase {
    private let baseUrl = "https://gateway.example/payment"
    private var session: URLSession!

    override func setUp() {
        StubProtocol.response = (Data(#"{"orderId":"md-1","formUrl":"https://x"}"#.utf8), 200)
        StubProtocol.error = nil
        StubProtocol.requests = []
        StubProtocol.bodies = []
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [StubProtocol.self]
        session = URLSession(configuration: config)
    }

    private func form(_ index: Int = 0) -> [String: String] {
        var result: [String: String] = [:]
        for pair in StubProtocol.bodies[index].split(separator: "&") {
            let kv = pair.split(separator: "=", maxSplits: 1).map { String($0).removingPercentEncoding ?? String($0) }
            result[kv[0]] = kv.count > 1 ? kv[1] : ""
        }
        return result
    }

    func test_parse_success() {
        XCTAssertEqual(parseRegisterResponse(Data(#"{"orderId":"0154d1a2-c70e","formUrl":"https://x"}"#.utf8)), .registered(mdOrder: "0154d1a2-c70e"))
    }

    func test_parse_gateway_error() {
        XCTAssertEqual(parseRegisterResponse(Data(#"{"errorCode":"5","errorMessage":"Access denied"}"#.utf8)), .failed(message: "Access denied"))
    }

    func test_parse_garbage() {
        guard case .failed = parseRegisterResponse(Data("<html>".utf8)) else { return XCTFail("garbage must fail") }
    }

    func test_empty_credentials_skip_network() async {
        let result = await GatewayOrderRegistrar(baseUrl: baseUrl, login: "", password: "", session: session).register(amountMinor: 100, clientId: nil)
        XCTAssertEqual(result, .missingCredentials)
        XCTAssertTrue(StubProtocol.requests.isEmpty)
    }

    func test_guest_sends_no_clientId() async {
        let result = await GatewayOrderRegistrar(baseUrl: baseUrl, login: "login", password: "secret", session: session)
            .register(amountMinor: 157_800, clientId: nil)
        XCTAssertEqual(result, .registered(mdOrder: "md-1"))
        XCTAssertEqual(StubProtocol.requests.first?.url?.absoluteString, "\(baseUrl)/rest/register.do")
        XCTAssertEqual(StubProtocol.requests.first?.httpMethod, "POST")
        let f = form()
        XCTAssertEqual(f["amount"], "157800")
        XCTAssertEqual(f["currency"], "978")
        XCTAssertEqual(f["returnUrl"], "sdk://done")
        XCTAssertEqual(f["userName"], "login")
        XCTAssertEqual(f["password"], "secret")
        XCTAssertFalse((f["orderNumber"] ?? "").isEmpty)
        XCTAssertNil(f["clientId"])
    }

    func test_buyer_with_cards_sends_clientId() async {
        _ = await GatewayOrderRegistrar(baseUrl: baseUrl, login: "login", password: "secret", session: session)
            .register(amountMinor: 157_800, clientId: "c-1")
        XCTAssertEqual(form()["clientId"], "c-1")
    }

    func test_orderNumber_fits_32_characters() async {
        _ = await GatewayOrderRegistrar(baseUrl: baseUrl, login: "login", password: "secret", session: session)
            .register(amountMinor: 100, clientId: nil)
        let orderNumber = form()["orderNumber"] ?? ""
        XCTAssertTrue((1...32).contains(orderNumber.count), "orderNumber '\(orderNumber)' is \(orderNumber.count) chars")
    }

    func test_two_registrations_use_different_orderNumber() async {
        let registrar = GatewayOrderRegistrar(baseUrl: baseUrl, login: "login", password: "secret", session: session)
        _ = await registrar.register(amountMinor: 100, clientId: nil)
        _ = await registrar.register(amountMinor: 100, clientId: nil)
        XCTAssertNotEqual(form(0)["orderNumber"], form(1)["orderNumber"])
    }

    func test_network_error_becomes_failed() async {
        StubProtocol.error = URLError(.notConnectedToInternet)
        let result = await GatewayOrderRegistrar(baseUrl: baseUrl, login: "login", password: "secret", session: session)
            .register(amountMinor: 100, clientId: nil)
        guard case .failed = result else { return XCTFail("offline must fail") }
    }

    func test_password_is_not_in_description() {
        let registrar = GatewayOrderRegistrar(baseUrl: baseUrl, login: "login", password: "secret", session: session)
        XCTAssertFalse(String(describing: registrar).contains("secret"))
    }

    // MARK: - API V2

    // A session the dev gateway created, and the mdOrder of its paymentUrl.
    private let sessionId = "ps_2GRSigK2sUQKFSAZWbg2z7UuQxxjz7wqCjZqYyWRiq22aF8xxCDPua"
    private let sessionMdOrder = "0180a01a-a8cb-75b8-8f9d-1a5f00ab8a11"
    private var sessionBody: Data {
        Data(#"{"id":"\#(sessionId)","amount":1000,"status":"open","paymentUrl":"https://x?mdOrder=\#(sessionMdOrder)"}"#.utf8)
    }

    private func json(_ index: Int = 0) -> [String: Any] {
        (try? JSONSerialization.jsonObject(with: Data(StubProtocol.bodies[index].utf8))) as? [String: Any] ?? [:]
    }

    func test_api2_lives_next_to_the_payment_page_of_the_base_url() {
        XCTAssertEqual(api2Url("https://gateway.example/payment"), "https://gateway.example/api2")
        XCTAssertEqual(api2Url("https://gateway.example/payment/"), "https://gateway.example/api2")
        XCTAssertEqual(api2Url("https://gateway.example:8443/payment"), "https://gateway.example:8443/api2")
        XCTAssertEqual(api2Url("https://gateway.example:443/payment"), "https://gateway.example/api2")
    }

    func test_session_id_decodes_to_its_mdOrder() {
        XCTAssertEqual(mdOrder(ofSession: sessionId), sessionMdOrder)
        XCTAssertNil(mdOrder(ofSession: "not-a-session"))
        XCTAssertNil(mdOrder(ofSession: "ps_0OIl"))
        XCTAssertNil(mdOrder(ofSession: "ps_"))
    }

    func test_parse_session() {
        XCTAssertEqual(parseSessionResponse(sessionBody), .registered(mdOrder: sessionMdOrder, sessionId: sessionId))
    }

    func test_parse_api2_error() {
        let body = Data(#"{"error":{"errorCode":"unauthorized","errorMessage":"Unknown user"}}"#.utf8)
        XCTAssertEqual(parseSessionResponse(body), .failed(message: "Unknown user"))
    }

    func test_parse_api2_garbage() {
        guard case .failed = parseSessionResponse(Data("<html>".utf8)) else { return XCTFail("garbage must fail") }
        XCTAssertEqual(parseSessionResponse(Data(#"{"id":"not-a-session"}"#.utf8)), .failed(message: "Unexpected session id not-a-session"))
    }

    func test_empty_api_key_skips_network() async {
        let result = await ApiV2SessionRegistrar(baseUrl: baseUrl, apiKey: "", session: session).register(amountMinor: 100, clientId: nil)
        XCTAssertEqual(result, .missingApiKey)
        XCTAssertTrue(StubProtocol.requests.isEmpty)
    }

    func test_api2_guest_creates_a_session_that_returns_to_the_sdk() async {
        StubProtocol.response = (sessionBody, 200)
        let result = await ApiV2SessionRegistrar(baseUrl: baseUrl, apiKey: "key-1", session: session)
            .register(amountMinor: 157_800, clientId: nil)
        XCTAssertEqual(result, .registered(mdOrder: sessionMdOrder, sessionId: sessionId))
        let request = StubProtocol.requests.first
        XCTAssertEqual(request?.httpMethod, "POST")
        XCTAssertEqual(request?.url?.absoluteString, "https://gateway.example/api2/sessions")
        XCTAssertEqual(request?.value(forHTTPHeaderField: "X-API-KEY"), "key-1")
        XCTAssertEqual(request?.value(forHTTPHeaderField: "Content-Type"), "application/json")
        let body = json()
        XCTAssertEqual((body["amount"] as? NSNumber)?.int64Value, 157_800)
        XCTAssertEqual(body["currency"] as? String, "EUR")
        XCTAssertEqual(body["successUrl"] as? String, "sdk://done")
        XCTAssertEqual(body["failureUrl"] as? String, "sdk://done")
        XCTAssertNil(body["merchantCustomerId"])
    }

    func test_api2_buyer_with_cards_sends_merchantCustomerId() async {
        StubProtocol.response = (sessionBody, 200)
        _ = await ApiV2SessionRegistrar(baseUrl: baseUrl, apiKey: "key-1", session: session).register(amountMinor: 100, clientId: "c-1")
        XCTAssertEqual(json()["merchantCustomerId"] as? String, "c-1")
    }

    func test_api2_refusal_becomes_failed() async {
        StubProtocol.response = (Data(#"{"error":{"errorCode":"unauthorized","errorMessage":"Unknown user"}}"#.utf8), 401)
        let result = await ApiV2SessionRegistrar(baseUrl: baseUrl, apiKey: "key-1", session: session).register(amountMinor: 100, clientId: nil)
        XCTAssertEqual(result, .failed(message: "Unknown user"))
    }

    func test_api2_network_error_becomes_failed() async {
        StubProtocol.error = URLError(.notConnectedToInternet)
        let result = await ApiV2SessionRegistrar(baseUrl: baseUrl, apiKey: "key-1", session: session).register(amountMinor: 100, clientId: nil)
        guard case .failed = result else { return XCTFail("offline must fail") }
    }

    func test_api_key_is_not_in_description() {
        let registrar = ApiV2SessionRegistrar(baseUrl: baseUrl, apiKey: "key-1", session: session)
        XCTAssertFalse(String(describing: registrar).contains("key-1"))
    }

    func test_the_settings_pick_the_registrar() {
        let v1 = merchantRegistrar(apiVersion: .v1, baseUrl: baseUrl, login: "login", password: "secret", apiKey: "key-1")
        let v2 = merchantRegistrar(apiVersion: .v2, baseUrl: baseUrl, login: "login", password: "secret", apiKey: "key-1")
        XCTAssertTrue(v1 is GatewayOrderRegistrar)
        XCTAssertTrue(v2 is ApiV2SessionRegistrar)
    }

    // MARK: - AFT transfers (registerP2P)

    func test_an_AFT_order_is_registered_as_a_one_leg_transfer_that_returns_to_the_sdk() async {
        StubProtocol.response = (Data(#"{"errorCode":0,"errorMessage":"Successful","orderId":"md-aft","formUrl":"https://x"}"#.utf8), 200)
        let result = await GatewayOrderRegistrar(baseUrl: baseUrl, login: "login", password: "secret", session: session, orderType: .aft)
            .register(amountMinor: 100, clientId: "c-1")
        XCTAssertEqual(result, .registered(mdOrder: "md-aft"))
        XCTAssertEqual(StubProtocol.requests.count, 1)
        let request = StubProtocol.requests.first
        XCTAssertEqual(request?.httpMethod, "POST")
        XCTAssertEqual(request?.url?.absoluteString, "\(baseUrl)/rest/api/p2p/registerP2P.do")
        XCTAssertEqual(request?.value(forHTTPHeaderField: "Content-Type"), "application/json")
        let body = json()
        XCTAssertEqual(body["username"] as? String, "login")
        XCTAssertEqual(body["password"] as? String, "secret")
        XCTAssertEqual((body["amount"] as? NSNumber)?.int64Value, 100)
        XCTAssertEqual(body["currency"] as? String, "978")
        XCTAssertEqual(body["returnUrl"] as? String, "sdk://done")
        XCTAssertEqual(body["transactionTypeIndicator"] as? String, "A")
        XCTAssertEqual((body["features"] as? [String: Any])?["feature"] as? [String], ["WITHOUT_TO_CARD"])
        XCTAssertEqual(body["clientId"] as? String, "c-1")
        let orderNumber = body["orderNumber"] as? String ?? ""
        XCTAssertTrue((1...32).contains(orderNumber.count), "orderNumber '\(orderNumber)' is \(orderNumber.count) chars")
    }

    func test_an_AFT_order_of_zero_only_saves_the_buyers_card() async {
        _ = await GatewayOrderRegistrar(baseUrl: baseUrl, login: "login", password: "secret", session: session, orderType: .aft)
            .register(amountMinor: 0, clientId: "c-1")
        let body = json()
        XCTAssertEqual((body["amount"] as? NSNumber)?.int64Value, 0)
        XCTAssertEqual((body["features"] as? [String: Any])?["feature"] as? [String], ["WITHOUT_TO_CARD", "VERIFY", "FORCE_CREATE_BINDING"])
        XCTAssertEqual(body["clientId"] as? String, "c-1")
    }

    func test_an_AFT_guest_sends_no_clientId() async {
        _ = await GatewayOrderRegistrar(baseUrl: baseUrl, login: "login", password: "secret", session: session, orderType: .aft)
            .register(amountMinor: 100, clientId: nil)
        XCTAssertNil(json()["clientId"])
    }

    func test_a_refused_AFT_registration_gives_the_gateways_message() async {
        StubProtocol.response = (Data(#"{"errorCode":14,"errorMessage":"Features are invalid"}"#.utf8), 200)
        let result = await GatewayOrderRegistrar(baseUrl: baseUrl, login: "login", password: "secret", session: session, orderType: .aft)
            .register(amountMinor: 100, clientId: nil)
        XCTAssertEqual(result, .failed(message: "Features are invalid"))
    }

    func test_an_AFT_order_without_credentials_skips_network() async {
        let result = await GatewayOrderRegistrar(baseUrl: baseUrl, login: "", password: "", session: session, orderType: .aft)
            .register(amountMinor: 100, clientId: nil)
        XCTAssertEqual(result, .missingCredentials)
        XCTAssertTrue(StubProtocol.requests.isEmpty)
    }

    func test_a_payment_order_stays_on_register_do() async {
        _ = await GatewayOrderRegistrar(baseUrl: baseUrl, login: "login", password: "secret", session: session, orderType: .purchase)
            .register(amountMinor: 100, clientId: nil)
        XCTAssertEqual(StubProtocol.requests.first?.url?.absoluteString, "\(baseUrl)/rest/register.do")
    }

    func test_the_factory_passes_the_order_type_to_API_V1_only() {
        let v1 = merchantRegistrar(apiVersion: .v1, baseUrl: baseUrl, login: "l", password: "p", apiKey: "", orderType: .aft)
        XCTAssertEqual((v1 as? GatewayOrderRegistrar)?.orderType, .aft)
        let v2 = merchantRegistrar(apiVersion: .v2, baseUrl: baseUrl, login: "l", password: "p", apiKey: "k", orderType: .aft)
        XCTAssertTrue(v2 is ApiV2SessionRegistrar)
    }

    func test_an_AFT_order_carries_the_prefill_the_app_passes() async {
        _ = await GatewayOrderRegistrar(baseUrl: baseUrl, login: "login", password: "secret", session: session, orderType: .aft,
                                        prefill: NorthwindProfile.prefill)
            .register(amountMinor: 10000, clientId: nil)
        let body = json()
        XCTAssertEqual(body["email"] as? String, "alex.kim@example.com")
        let payer = body["billingPayerData"] as? [String: String]
        XCTAssertEqual(payer?["payerFirstName"], "Alex")
        XCTAssertEqual(payer?["payerBirthday"], "19900115")
        XCTAssertEqual(payer?["billingCountry"], "CY")
        let recipient = body["billingRecipientData"] as? [String: String]
        XCTAssertEqual(recipient?["recipientAccount"], "NW00007781")
        XCTAssertEqual(recipient?["recipientAccountNumberType"], "100")
        XCTAssertEqual(body["transactionTypeIndicator"] as? String, "A")
    }

    func test_an_AFT_order_without_prefill_sends_no_payer_data() async {
        _ = await GatewayOrderRegistrar(baseUrl: baseUrl, login: "login", password: "secret", session: session, orderType: .aft)
            .register(amountMinor: 100, clientId: nil)
        let body = json()
        XCTAssertNil(body["email"])
        XCTAssertNil(body["billingPayerData"])
        XCTAssertNil(body["billingRecipientData"])
    }

    func test_a_payment_order_ignores_the_prefill() async {
        _ = await GatewayOrderRegistrar(baseUrl: baseUrl, login: "login", password: "secret", session: session, prefill: NorthwindProfile.prefill)
            .register(amountMinor: 100, clientId: nil)
        XCTAssertNil(form()["email"])
        XCTAssertEqual(StubProtocol.requests.first?.url?.absoluteString, "\(baseUrl)/rest/register.do")
    }

    func test_the_factory_passes_the_prefill_to_API_V1() {
        let v1 = merchantRegistrar(apiVersion: .v1, baseUrl: baseUrl, login: "l", password: "p", apiKey: "", orderType: .aft,
                                   prefill: NorthwindProfile.prefill)
        XCTAssertEqual((v1 as? GatewayOrderRegistrar)?.prefill, NorthwindProfile.prefill)
    }
}
