@testable import QaApp
import XCTest

final class MerchantResponsesTests: XCTestCase {
    private let none = GatewaySecrets(profile: StandProfile(name: "v1", gatewayUrl: "https://dev.bpcbt.com/payment", login: "login-1"))

    private func secrets(password: String) -> GatewaySecrets {
        GatewaySecrets(profile: StandProfile(name: "v1", gatewayUrl: "https://dev.bpcbt.com/payment", login: "login-1", password: password))
    }

    func test_the_api2_url_keeps_the_scheme_the_host_and_a_non_default_port() {
        XCTAssertEqual(api2Url("https://dev.bpcbt.com/payment"), "https://dev.bpcbt.com/api2")
        XCTAssertEqual(api2Url("  https://dev.bpcbt.com:443/payment/  "), "https://dev.bpcbt.com/api2")
        XCTAssertEqual(api2Url("http://dev.bpcbt.com:8080/payment"), "http://dev.bpcbt.com:8080/api2")
        XCTAssertNil(api2Url("not a url"))
    }

    func test_a_session_id_decodes_to_its_mdOrder() {
        XCTAssertNil(mdOrder(ofSession: "ps_3Nm8Ua5Tgxnk"))
        XCTAssertNil(mdOrder(ofSession: "ps_emhs"))
        XCTAssertNil(mdOrder(ofSession: "zzz"))
        XCTAssertNil(mdOrder(ofSession: "ps_0OIl"))
        XCTAssertNil(mdOrder(ofSession: "ps_"))
    }

    func test_a_session_id_of_ps_colon_and_an_order_decodes() {
        // Base58 of "ps:a" (0x70 0x73 0x3a 0x61).
        XCTAssertEqual(mdOrder(ofSession: "ps_3siKPN"), "a")
        XCTAssertEqual(mdOrder(ofSession: "ps_" + base58(Array("ps:0f0e1d2c".utf8))), "0f0e1d2c")
    }

    func test_an_order_number_is_qa_and_twenty_hex_digits_new_each_time() {
        let first = newOrderNumber()

        XCTAssertTrue(first.hasPrefix("qa-"), first)
        XCTAssertEqual(first.unicodeScalars.count, 23)
        XCTAssertTrue(first.dropFirst(3).unicodeScalars.allSatisfy { "0123456789abcdef".unicodeScalars.contains($0) }, first)
        XCTAssertNotEqual(newOrderNumber(), first)
    }

    func test_a_register_answer_without_an_order_id_or_an_error_fails_with_the_body() {
        XCTAssertEqual(registerAnswer(200, #"{"formUrl":"x"}"#, none), .failed(httpStatus: 200, message: #"HTTP 200: {"formUrl":"x"}"#))
        XCTAssertEqual(registerAnswer(200, #"{"orderId":"  "}"#, none), .failed(httpStatus: 200, message: #"HTTP 200: {"orderId":"  "}"#))
        XCTAssertEqual(registerAnswer(200, #"{"errorCode":"0"}"#, none), .failed(httpStatus: 200, message: #"HTTP 200: {"errorCode":"0"}"#))
        XCTAssertEqual(registerAnswer(200, "[1]", none), .failed(httpStatus: 200, message: "HTTP 200: [1]"))
        XCTAssertEqual(registerAnswer(200, "not json", none), .failed(httpStatus: 200, message: "HTTP 200: not json"))
        XCTAssertEqual(registerAnswer(200, "   ", none), .failed(httpStatus: 200, message: "HTTP 200: (empty body)"))
    }

    func test_a_register_error_without_a_message_names_its_code() {
        XCTAssertEqual(registerAnswer(200, #"{"errorCode":5}"#, none), .failed(httpStatus: 200, message: "Gateway error 5"))
    }

    func test_an_order_id_given_as_a_number_is_read_as_its_text() {
        XCTAssertEqual(registerAnswer(200, #"{"orderId":123}"#, none), .registered(mdOrder: "123", sessionId: nil))
    }

    func test_an_order_id_under_a_failing_http_status_is_not_a_registration() {
        XCTAssertEqual(registerAnswer(500, #"{"orderId":"abc"}"#, none), .failed(httpStatus: 500, message: #"HTTP 500: {"orderId":"abc"}"#))
    }

    func test_a_session_answer_without_an_id_fails() {
        XCTAssertEqual(sessionAnswer(201, "{}", none), .failed(httpStatus: 201, message: "HTTP 201: {}"))
        XCTAssertEqual(sessionAnswer(400, #"{"error":{"errorCode":"7"}}"#, none), .failed(httpStatus: 400, message: "Gateway error 7"))
    }

    func test_a_status_answer_reads_numbers_given_as_text() {
        XCTAssertEqual(
            statusAnswer(200, #"{"orderStatus":"2","actionCode":"-2007","actionCodeDescription":null}"#, none),
            .known(orderStatus: 2, actionCode: -2007, actionCodeDescription: nil)
        )
    }

    func test_a_status_answer_with_error_code_zero_is_known() {
        XCTAssertEqual(statusAnswer(200, #"{"errorCode":"0","orderStatus":0}"#, none), .known(orderStatus: 0, actionCode: nil, actionCodeDescription: nil))
    }

    func test_a_status_number_outside_32_bits_is_not_read() {
        XCTAssertEqual(
            statusAnswer(200, #"{"orderStatus":2147483648,"actionCode":2.0,"actionCodeDescription":"x"}"#, none),
            .known(orderStatus: nil, actionCode: nil, actionCodeDescription: "x")
        )
    }

    func test_secrets_are_masked_in_every_form_they_travel_in() {
        let secrets = secrets(password: #"a+b c&d"e"#)

        XCTAssertEqual(
            secrets.mask(#"raw a+b c&d"e form a%2Bb+c%26d%22e url a%2Bb%20c%26d%22e json a+b c&d\"e html a+b c&amp;d&quot;e"#),
            "raw *** form *** url *** json *** html ***"
        )
    }

    func test_secrets_are_masked_in_the_escapes_other_serializers_use() {
        let equals = secrets(password: "p=ss")
        let mixed = secrets(password: "a/b+c<d")

        XCTAssertEqual(equals.mask(#"gson p=ss upper p=ss url p%3dss"#), "gson *** upper *** url ***")
        XCTAssertEqual(mixed.mask(#"slash a\/b+c<d html-safe a\/b+c<d plain a/b+c<d"#), "slash *** html-safe *** plain ***")
        XCTAssertEqual(mixed.mask("lower a%2fb%2bc%3cd upper a%2Fb%2Bc%3Cd"), "lower *** upper ***")
    }

    func test_secrets_are_masked_in_any_mix_of_spellings() {
        let secrets = secrets(password: "p=s s'é😀")

        XCTAssertEqual(secrets.mask(#"p%3Ds+s'%C3%A9😀"#), "***")
        XCTAssertEqual(secrets.mask(#"p%3ds%20s&#39;é%F0%9F%98%80 tail"#), "*** tail")
        XCTAssertEqual(secrets.mask("p&#x3D;s s&#39;\\u00e9😀"), "***")
        XCTAssertEqual(secrets.mask("p=s s'e😀"), "p=s s'e😀")
    }

    func test_masking_happens_before_the_cut() {
        let secrets = secrets(password: "password-1")

        let text = secrets.bounded(String(repeating: "x", count: 295) + "password-1" + String(repeating: "y", count: 10), limit: 300)

        XCTAssertEqual(text, String(repeating: "x", count: 295) + "***" + "yy")
    }

    func test_the_cut_counts_utf16_units_and_does_not_split_a_pair() {
        XCTAssertEqual(none.bounded("ab😀cd", limit: 4, cutMarker: "…"), "ab😀…")
        XCTAssertEqual(none.bounded("ab😀cd", limit: 3, cutMarker: "…"), "ab…")
        XCTAssertEqual(none.bounded("ab😀", limit: 4, cutMarker: "…"), "ab😀")
    }

    func test_no_secrets_leave_the_text_as_it_is() {
        XCTAssertEqual(none.mask("password-1"), "password-1")
    }

    func test_the_request_is_printed_without_its_secrets() {
        let request = GatewayRequest.form(
            url: "https://dev.bpcbt.com/payment/rest/x",
            parameters: [("userName", "login-1"), ("password", "password-1")]
        )

        XCTAssertEqual(request.printed(secrets(password: "password-1")), "→ POST https://dev.bpcbt.com/payment/rest/x userName=login-1&password=***")
        XCTAssertFalse(String(describing: request).contains("password-1"))
        XCTAssertFalse(String(reflecting: request).contains("password-1"))
        XCTAssertFalse(String(describing: secrets(password: "password-1")).contains("password-1"))
    }
}
