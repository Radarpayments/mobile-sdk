@testable import QaApp
import XCTest

final class MerchantRegisterTests: MerchantTestCase {
    func test_a_payment_order_is_a_register_do_form() async throws {
        let result = try await merchant().register(order: OrderConfig(), profile: v1)

        XCTAssertEqual(result, .registered(mdOrder: "abc", sessionId: nil))
        let sent = try single()
        XCTAssertEqual(sent.method, "POST")
        XCTAssertEqual(sent.url, "https://dev.bpcbt.com/payment/rest/register.do")
        XCTAssertEqual(sent.headers["Content-Type"]?.hasPrefix("application/x-www-form-urlencoded"), true)
        XCTAssertEqual(sent.formNames, ["userName", "password", "amount", "currency", "orderNumber", "returnUrl"])
        XCTAssertEqual(
            sent.form,
            [
                "userName": "login-1",
                "password": "password-1",
                "amount": "10000",
                "currency": "978",
                "orderNumber": "qa-1",
                "returnUrl": "sdk://done",
            ]
        )
    }

    func test_a_trailing_slash_of_the_gateway_url_is_not_doubled() async throws {
        var profile = v1
        profile.gatewayUrl = "https://dev.bpcbt.com/payment/"

        _ = try await merchant().register(order: OrderConfig(), profile: profile)

        XCTAssertEqual(try single().url, "https://dev.bpcbt.com/payment/rest/register.do")
    }

    func test_the_client_id_and_the_features_join_the_form() async throws {
        let order = OrderConfig(clientId: "c1", features: ["VERIFY", "FORCE_CREATE_BINDING"])

        _ = try await merchant().register(order: order, profile: v1)

        let form = try single().form
        XCTAssertEqual(form["clientId"], "c1")
        XCTAssertEqual(form["features"], "VERIFY,FORCE_CREATE_BINDING")
    }

    func test_a_form_value_with_reserved_characters_survives() async throws {
        _ = try await merchant().register(order: OrderConfig(clientId: "a+b&c=d"), profile: v1)

        let sent = try single()
        XCTAssertEqual(sent.form["clientId"], "a+b&c=d")
        XCTAssertEqual(sent.formNames.last, "clientId")
    }

    func test_the_currency_goes_as_its_numeric_code() async throws {
        _ = try await merchant().register(order: OrderConfig(currency: "USD"), profile: v1)

        XCTAssertEqual(try single().form["currency"], "840")
    }

    func test_an_unknown_currency_fails_before_anything_is_sent() async throws {
        let result = try await merchant().register(order: OrderConfig(currency: "XXZ"), profile: v1)

        XCTAssertEqual(result, .failed(httpStatus: nil, message: "Unknown currency XXZ"))
        XCTAssertTrue(StubURLProtocol.requests.isEmpty)
    }

    func test_extra_replaces_a_form_parameter_and_adds_its_own() async throws {
        let order = OrderConfig(extra: object(#"{"returnUrl":"app://x","jsonParams":{"a":1}}"#))

        _ = try await merchant().register(order: order, profile: v1)

        let sent = try single()
        XCTAssertEqual(sent.formNames.filter { $0 == "returnUrl" }.count, 1)
        XCTAssertEqual(sent.form["returnUrl"], "app://x")
        XCTAssertEqual(sent.form["jsonParams"], #"{"a":1}"#)
    }

    func test_scalar_extra_values_go_into_the_form_as_their_text() async throws {
        let order = OrderConfig(extra: object(#"{"n":5,"b":true,"s":"x y","list":[1,"a"]}"#))

        _ = try await merchant().register(order: order, profile: v1)

        let form = try single().form
        XCTAssertEqual(form["n"], "5")
        XCTAssertEqual(form["b"], "true")
        XCTAssertEqual(form["s"], "x y")
        XCTAssertEqual(form["list"], #"[1,"a"]"#)
    }

    func test_an_aft_order_is_a_registerP2P_json_with_WITHOUT_TO_CARD_first() async throws {
        let order = OrderConfig(type: .aft, features: ["VERIFY", "WITHOUT_TO_CARD"])

        _ = try await merchant().register(order: order, profile: v1)

        let sent = try single()
        XCTAssertEqual(sent.url, "https://dev.bpcbt.com/payment/rest/api/p2p/registerP2P.do")
        XCTAssertEqual(sent.headers["Content-Type"]?.hasPrefix("application/json"), true)
        let body = try XCTUnwrap(sent.jsonObject)
        XCTAssertEqual(body["username"], .string("login-1"))
        XCTAssertEqual(body["password"], .string("password-1"))
        XCTAssertEqual(body["amount"], .number("10000"))
        XCTAssertEqual(body["currency"], .string("978"))
        XCTAssertEqual(body["orderNumber"], .string("qa-1"))
        XCTAssertEqual(body["returnUrl"], .string("sdk://done"))
        XCTAssertEqual(body["transactionTypeIndicator"], .string("A"))
        XCTAssertEqual(body["features"], json(#"{"feature":["WITHOUT_TO_CARD","VERIFY"]}"#))
        XCTAssertNil(body["clientId"])
    }

    func test_an_aft_order_carries_the_client_id() async throws {
        _ = try await merchant().register(order: OrderConfig(type: .aft, clientId: "c1"), profile: v1)

        let body = try XCTUnwrap(try single().jsonObject)
        XCTAssertEqual(body["clientId"], .string("c1"))
        XCTAssertEqual(body["features"], json(#"{"feature":["WITHOUT_TO_CARD"]}"#))
    }

    func test_extra_replaces_a_whole_json_value() async throws {
        let order = OrderConfig(type: .aft, extra: object(#"{"features":{"feature":["X"]}}"#))

        _ = try await merchant().register(order: order, profile: v1)

        XCTAssertEqual(try single().jsonObject?["features"], json(#"{"feature":["X"]}"#))
    }

    func test_a_v2_profile_opens_a_session() async throws {
        _ = try await merchant().register(order: OrderConfig(clientId: "c1"), profile: v2)

        let sent = try single()
        XCTAssertEqual(sent.url, "https://dev.bpcbt.com/api2/sessions")
        XCTAssertEqual(sent.headers["X-API-KEY"], "key-1")
        XCTAssertEqual(sent.headers["Content-Type"]?.hasPrefix("application/json"), true)
        let body = try XCTUnwrap(sent.jsonObject)
        XCTAssertEqual(body["amount"], .number("10000"))
        XCTAssertEqual(body["currency"], .string("EUR"))
        XCTAssertEqual(body["successUrl"], .string("sdk://done"))
        XCTAssertEqual(body["failureUrl"], .string("sdk://done"))
        XCTAssertEqual(body["merchantCustomerId"], .string("c1"))
        XCTAssertNil(body["features"])
    }

    func test_a_session_carries_the_features_as_an_array() async throws {
        _ = try await merchant().register(order: OrderConfig(features: ["VERIFY"]), profile: v2)

        let body = try XCTUnwrap(try single().jsonObject)
        XCTAssertEqual(body["features"], .array([.string("VERIFY")]))
        XCTAssertNil(body["merchantCustomerId"])
    }

    func test_an_aft_order_on_a_v2_profile_fails_before_anything_is_sent() async throws {
        let result = try await merchant().register(order: OrderConfig(type: .aft), profile: v2)

        guard case let .failed(httpStatus, _) = result else { return XCTFail("\(result)") }
        XCTAssertNil(httpStatus)
        XCTAssertTrue(StubURLProtocol.requests.isEmpty)
    }

    func test_a_registered_order_returns_its_mdOrder() async throws {
        answer(#"{"orderId":"abc","formUrl":"https://dev.bpcbt.com/payment/merchants/x/payment_ru.html?mdOrder=abc"}"#)

        let result = try await merchant().register(order: OrderConfig(), profile: v1)

        XCTAssertEqual(result, .registered(mdOrder: "abc", sessionId: nil))
    }

    func test_a_session_returns_its_id_and_the_decoded_mdOrder() async throws {
        let mdOrder = "0f0e1d2c-3b4a-5968-7766-554433221100"
        let id = sessionId(of: mdOrder)
        answer(#"{"id":"\#(id)"}"#)

        let result = try await merchant().register(order: OrderConfig(), profile: v2)

        XCTAssertEqual(result, .registered(mdOrder: mdOrder, sessionId: id))
    }

    func test_a_session_id_that_does_not_decode_still_registers() async throws {
        answer(#"{"id":"zzz"}"#)

        let result = try await merchant().register(order: OrderConfig(), profile: v2)

        XCTAssertEqual(result, .registered(mdOrder: nil, sessionId: "zzz"))
    }

    func test_a_session_error_carries_the_gateways_message() async throws {
        answer(400, #"{"error":{"errorCode":"7","errorMessage":"Bad amount"}}"#)

        let result = try await merchant().register(order: OrderConfig(), profile: v2)

        XCTAssertEqual(result, .failed(httpStatus: 400, message: "Bad amount"))
    }

    func test_a_gateway_error_carries_its_message_and_the_http_status() async throws {
        answer(200, #"{"errorCode":"5","errorMessage":"Access denied"}"#)

        let result = try await merchant().register(order: OrderConfig(), profile: v1)

        XCTAssertEqual(result, .failed(httpStatus: 200, message: "Access denied"))
    }

    func test_an_html_error_page_fails_with_the_status_and_the_start_of_the_body() async throws {
        answer(502, "<html><body>" + String(repeating: "x", count: 1000) + "</body></html>")

        let result = try await merchant().register(order: OrderConfig(), profile: v1)

        guard case let .failed(httpStatus, message) = result else { return XCTFail("\(result)") }
        XCTAssertEqual(httpStatus, 502)
        XCTAssertTrue(message.hasPrefix("HTTP 502: <html>"), message)
        XCTAssertEqual(message.utf16.count, "HTTP 502: ".utf16.count + 300)
    }

    func test_an_empty_body_fails_with_the_status() async throws {
        answer(200, "")

        let result = try await merchant().register(order: OrderConfig(), profile: v1)

        XCTAssertEqual(result, .failed(httpStatus: 200, message: "HTTP 200: (empty body)"))
    }

    func test_no_answer_in_time_fails_without_a_status() async throws {
        StubURLProtocol.reset { _ in .after(seconds: 10, .answer(status: 200, body: #"{"orderId":"abc"}"#)) }

        let result = try await merchant(timeout: 0.05).register(order: OrderConfig(), profile: v1)

        XCTAssertEqual(result, .failed(httpStatus: nil, message: "The gateway did not answer in 0 s"))
    }

    func test_a_network_failure_fails_without_a_status() async throws {
        StubURLProtocol.reset { _ in .fail(URLError(.networkConnectionLost)) }

        let result = try await merchant().register(order: OrderConfig(), profile: v1)

        guard case let .failed(httpStatus, message) = result else { return XCTFail("\(result)") }
        XCTAssertNil(httpStatus)
        XCTAssertEqual(message, "URLError: " + URLError(.networkConnectionLost).localizedDescription)
        XCTAssertFalse(message.contains("dev.bpcbt.com"), message)
    }

    func test_a_network_failure_names_no_url_and_no_secret() async throws {
        var profile = v1
        profile.gatewayUrl = "https://login-1:password-1@dev.bpcbt.com/payment"
        StubURLProtocol.reset { request in
            .fail(URLError(.cannotFindHost, userInfo: [NSURLErrorFailingURLErrorKey: request.url as Any]))
        }

        let result = try await merchant().register(order: OrderConfig(), profile: profile)

        guard case let .failed(_, message) = result else { return XCTFail("\(result)") }
        XCTAssertFalse(message.contains("dev.bpcbt.com"), message)
        XCTAssertFalse(journalText.contains("password-1"), journalText)
    }

    func test_cancellation_is_not_swallowed() async throws {
        let started = expectation(description: "the request reached the gateway")
        StubURLProtocol.reset { _ in .hang }
        StubURLProtocol.notifyOnStart(started)
        let merchant = merchant()
        let profile = v1

        let task = Task { try await merchant.register(order: OrderConfig(), profile: profile) }
        await fulfillment(of: [started], timeout: 5)
        task.cancel()

        let outcome = await task.result
        XCTAssertThrowsError(try outcome.get()) { XCTAssertTrue($0 is CancellationError, "\($0)") }
    }

    func test_the_callers_own_timeout_is_not_turned_into_a_failure() async throws {
        let started = expectation(description: "the request reached the gateway")
        StubURLProtocol.reset { _ in .hang }
        StubURLProtocol.notifyOnStart(started)
        let merchant = merchant()
        let profile = v1

        let task = Task { try await merchant.register(order: OrderConfig(), profile: profile) }
        let deadline = Task {
            try await Task.sleep(nanoseconds: 300_000_000)
            task.cancel()
        }
        let outcome = await task.result
        _ = await deadline.result

        await fulfillment(of: [started], timeout: 1)
        XCTAssertThrowsError(try outcome.get()) { XCTAssertTrue($0 is CancellationError, "\($0)") }
        XCTAssertFalse(journalText.contains("did not answer"), journalText)
    }

    func test_an_http_stack_that_reports_our_timeout_as_an_error_still_times_out() async throws {
        // URLSession ends the task the timeout cancels with URLError.cancelled.
        StubURLProtocol.reset { _ in .hang }

        let result = try await merchant(timeout: 0.05).register(order: OrderConfig(), profile: v1)

        XCTAssertEqual(result, .failed(httpStatus: nil, message: "The gateway did not answer in 0 s"))
        XCTAssertFalse(journalText.contains("URLError"), journalText)
    }

    func test_a_body_that_does_not_end_in_time_is_no_answer() async throws {
        StubURLProtocol.reset { _ in .headOnly(status: 200) }

        let result = try await merchant(timeout: 0.2).register(order: OrderConfig(), profile: v1)

        XCTAssertEqual(result, .failed(httpStatus: nil, message: "The gateway did not answer in 0 s"))
        XCTAssertEqual(journal.snapshot().last?.message, "← The gateway did not answer in 0 s")
        XCTAssertEqual(journal.snapshot().last?.level, .warning)
    }

    func test_the_requests_own_timeout_does_not_come_before_the_merchants() async throws {
        _ = try await merchant(timeout: 120).register(order: OrderConfig(), profile: v1)

        XCTAssertGreaterThan(try single().timeoutInterval, 120)
        let configuration = makeMerchantSession().configuration
        XCTAssertGreaterThan(configuration.timeoutIntervalForRequest, 120)
        XCTAssertGreaterThan(configuration.timeoutIntervalForResource, 120)
    }

    func test_a_null_in_extra_removes_a_form_parameter() async throws {
        let order = OrderConfig(extra: object(#"{"returnUrl":null,"absent":null}"#))

        _ = try await merchant().register(order: order, profile: v1)

        XCTAssertEqual(try single().formNames, ["userName", "password", "amount", "currency", "orderNumber"])
    }

    func test_a_null_in_extra_removes_a_json_value() async throws {
        _ = try await merchant().register(order: OrderConfig(type: .aft, extra: object(#"{"returnUrl":null}"#)), profile: v1)
        _ = try await merchant().register(order: OrderConfig(extra: object(#"{"failureUrl":null}"#)), profile: v2)

        let requests = StubURLProtocol.requests
        XCTAssertEqual(requests.count, 2)
        XCTAssertEqual(
            Set(requests[0].jsonObject?.keys ?? []),
            ["username", "password", "amount", "currency", "orderNumber", "transactionTypeIndicator", "features"]
        )
        XCTAssertEqual(Set(requests[1].jsonObject?.keys ?? []), ["amount", "currency", "successUrl"])
    }

    func test_the_journal_shows_that_the_api_key_went_out_not_the_key() async throws {
        answer(#"{"id":"zzz"}"#)

        _ = try await merchant().register(order: OrderConfig(), profile: v2)
        _ = try await merchant().register(order: OrderConfig(type: .aft), profile: v1)

        let sent = journal.snapshot().map(\.message).filter { $0.hasPrefix("→") }
        XCTAssertEqual(sent.count, 2)
        XCTAssertTrue(sent[0].hasPrefix("→ POST https://dev.bpcbt.com/api2/sessions X-API-KEY: *** {"), sent[0])
        XCTAssertFalse(sent[0].contains("key-1"), sent[0])
        XCTAssertFalse(sent[1].contains("X-API-KEY"), sent[1])
    }

    func test_the_journal_shows_the_exchange_without_the_password_and_the_api_key() async throws {
        _ = try await merchant().register(order: OrderConfig(), profile: v1)
        _ = try await merchant().register(order: OrderConfig(type: .aft), profile: v1)
        _ = try await merchant().register(order: OrderConfig(), profile: v2)

        let text = journalText
        XCTAssertTrue(text.contains("→ POST https://dev.bpcbt.com/payment/rest/register.do"), text)
        XCTAssertTrue(text.contains("login-1"), text)
        XCTAssertTrue(text.contains(#"← 200 {"orderId":"abc"}"#), text)
        XCTAssertTrue(text.contains("***"), text)
        XCTAssertFalse(text.contains("password-1"), text)
        XCTAssertFalse(text.contains("key-1"), text)
        XCTAssertEqual(Set(journal.snapshot().map(\.tag)), ["merchant"])
    }

    func test_a_form_request_is_journaled_as_typed_with_the_password_masked() async throws {
        _ = try await merchant().register(order: OrderConfig(clientId: "a b"), profile: v1)

        XCTAssertEqual(
            journal.snapshot().first?.message,
            "→ POST https://dev.bpcbt.com/payment/rest/register.do "
                + "userName=login-1&password=***&amount=10000&currency=978&orderNumber=qa-1&returnUrl=sdk://done&clientId=a b"
        )
        XCTAssertEqual(journal.snapshot().map(\.level), [.info, .info])
    }

    func test_an_answer_that_is_not_a_success_is_journaled_as_a_warning() async throws {
        answer(500, "busy")

        _ = try await merchant().register(order: OrderConfig(), profile: v1)

        XCTAssertEqual(journal.snapshot().map(\.level), [.info, .warning])
        XCTAssertEqual(journal.snapshot().last?.message, "← 500 busy")
    }

    func test_a_long_answer_is_cut_in_the_journal() async throws {
        answer(200, String(repeating: "z", count: 5000))

        _ = try await merchant().register(order: OrderConfig(), profile: v1)

        let line = try XCTUnwrap(journal.snapshot().last?.message)
        XCTAssertEqual(line, "← 200 " + String(repeating: "z", count: 4000 - "← 200 ".utf16.count) + "…")
    }

    func test_a_reply_that_echoes_the_secrets_keeps_them_out_of_the_journal_and_the_message() async throws {
        var profile = v1
        profile.password = #"pa ss&"zq"-1"#
        let echo = #"raw=pa ss&"zq"-1 form=pa+ss%26%22zq%22-1 url=pa%20ss%26%22zq%22-1 "#
            + #"json=pa ss&\"zq\"-1 html=pa ss&amp;&quot;zq&quot;-1"#
        answer(500, "<pre>userName=login-1 \(echo)</pre>")

        let result = try await merchant().register(order: OrderConfig(), profile: profile)

        XCTAssertEqual(
            result,
            .failed(httpStatus: 500, message: "HTTP 500: <pre>userName=login-1 raw=*** form=*** url=*** json=*** html=***</pre>")
        )
        XCTAssertFalse(journalText.contains("zq"), journalText)
    }

    func test_a_secret_on_the_edge_of_the_cut_is_not_left_half_visible() async throws {
        answer(500, String(repeating: "x", count: 295) + "password-1" + String(repeating: "y", count: 100))

        let result = try await merchant().register(order: OrderConfig(), profile: v1)

        guard case let .failed(_, message) = result else { return XCTFail("\(result)") }
        XCTAssertFalse(message.contains("passw"), message)
        XCTAssertTrue(message.contains("x***"), message)
    }

    func test_a_gateway_message_that_quotes_the_api_key_is_masked() async throws {
        answer(401, #"{"error":{"errorMessage":"Unknown key key-1"}}"#)

        let result = try await merchant().register(order: OrderConfig(), profile: v2)

        XCTAssertEqual(result, .failed(httpStatus: 401, message: "Unknown key ***"))
        XCTAssertFalse(journalText.contains("key-1"), journalText)
    }

    func test_an_answer_too_deep_to_read_fails_with_the_body() async throws {
        let deep = String(repeating: "[", count: 30_000) + String(repeating: "]", count: 30_000)
        answer(200, deep)

        let result = try await merchant().register(order: OrderConfig(), profile: v1)

        XCTAssertEqual(result, .failed(httpStatus: 200, message: "HTTP 200: " + String(repeating: "[", count: 300)))
    }

    func test_an_answer_over_the_document_limit_is_not_read() async throws {
        answer(#"{"orderId":"abc","padding":""# + String(repeating: "p", count: 70_000) + #""}"#)

        let result = try await merchant().register(order: OrderConfig(), profile: v1)

        guard case let .failed(httpStatus, message) = result else { return XCTFail("\(result)") }
        XCTAssertEqual(httpStatus, 200)
        XCTAssertTrue(message.hasPrefix(#"HTTP 200: {"orderId":"abc""#), message)
    }

    func test_the_order_id_is_read_from_the_whole_body() async throws {
        answer(#"{"padding":""# + String(repeating: "p", count: 5000) + #"","orderId":"abc"}"#)

        let result = try await merchant().register(order: OrderConfig(), profile: v1)

        XCTAssertEqual(result, .registered(mdOrder: "abc", sessionId: nil))
    }

    private func sessionId(of mdOrder: String) -> String {
        "ps_" + base58(Array("ps:\(mdOrder)".utf8))
    }
}

func base58(_ bytes: [UInt8]) -> String {
    let alphabet = Array("123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz")
    var number = bytes
    var digits: [Character] = []
    while number.contains(where: { $0 != 0 }) {
        var remainder = 0
        var quotient: [UInt8] = []
        for byte in number {
            let value = remainder * 256 + Int(byte)
            let digit = value / 58
            remainder = value % 58
            if !quotient.isEmpty || digit != 0 { quotient.append(UInt8(digit)) }
        }
        digits.append(alphabet[remainder])
        number = quotient
    }
    return String(digits.reversed())
}
