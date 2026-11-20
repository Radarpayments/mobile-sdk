@testable import QaApp
import XCTest

final class MerchantStatusTests: MerchantTestCase {
    func test_the_status_is_asked_with_the_order_id() async throws {
        answer(#"{"errorCode":"0","orderStatus":2,"actionCode":0,"actionCodeDescription":"Approved","amount":10000}"#)

        let status = try await merchant().status(orderId: "abc", profile: v1)

        XCTAssertEqual(status, .known(orderStatus: 2, actionCode: 0, actionCodeDescription: "Approved"))
        let sent = try single()
        XCTAssertEqual(sent.method, "POST")
        XCTAssertEqual(sent.url, "https://dev.bpcbt.com/payment/rest/getOrderStatusExtended.do")
        XCTAssertEqual(sent.formNames, ["userName", "password", "orderId"])
        XCTAssertEqual(sent.form, ["userName": "login-1", "password": "password-1", "orderId": "abc"])
    }

    func test_a_v2_profile_is_not_queried() async throws {
        let status = try await merchant().status(orderId: "abc", profile: v2)

        XCTAssertEqual(status, .notQueried)
        XCTAssertTrue(StubURLProtocol.requests.isEmpty)
    }

    func test_a_profile_without_credentials_is_not_queried() async throws {
        var noPassword = v1
        noPassword.password = ""
        var blankLogin = v1
        blankLogin.login = " "

        let first = try await merchant().status(orderId: "abc", profile: noPassword)
        let second = try await merchant().status(orderId: "abc", profile: blankLogin)

        XCTAssertEqual(first, .notQueried)
        XCTAssertEqual(second, .notQueried)
        XCTAssertTrue(StubURLProtocol.requests.isEmpty)
    }

    func test_no_order_id_is_not_queried() async throws {
        let first = try await merchant().status(orderId: nil, profile: v1)
        let second = try await merchant().status(orderId: "", profile: v1)

        XCTAssertEqual(first, .notQueried)
        XCTAssertEqual(second, .notQueried)
        XCTAssertTrue(StubURLProtocol.requests.isEmpty)
    }

    func test_a_status_error_carries_the_gateways_message() async throws {
        answer(#"{"errorCode":"6","errorMessage":"Order not found"}"#)

        let status = try await merchant().status(orderId: "abc", profile: v1)

        XCTAssertEqual(status, .failed(httpStatus: 200, message: "Order not found"))
    }

    func test_a_status_error_without_a_message_names_its_code() async throws {
        answer(#"{"errorCode":"6"}"#)

        let status = try await merchant().status(orderId: "abc", profile: v1)

        XCTAssertEqual(status, .failed(httpStatus: 200, message: "Gateway error 6"))
    }

    func test_a_status_answer_without_any_field_fails_with_the_body() async throws {
        answer(503, "busy")

        let status = try await merchant().status(orderId: "abc", profile: v1)

        XCTAssertEqual(status, .failed(httpStatus: 503, message: "HTTP 503: busy"))
    }

    func test_the_status_exchange_is_journaled_without_the_password() async throws {
        answer(#"{"orderStatus":2}"#)

        let status = try await merchant().status(orderId: "abc", profile: v1)

        XCTAssertEqual(status, .known(orderStatus: 2, actionCode: nil, actionCodeDescription: nil))
        XCTAssertTrue(journalText.contains("getOrderStatusExtended.do"), journalText)
        XCTAssertTrue(journalText.contains("orderId=abc"), journalText)
        XCTAssertFalse(journalText.contains("password-1"), journalText)
    }

    func test_the_status_has_the_text_of_the_result_screen() {
        XCTAssertEqual(
            OrderStatus.known(orderStatus: 2, actionCode: 0, actionCodeDescription: "Approved").text,
            "orderStatus=2, actionCode=0, Approved"
        )
        XCTAssertEqual(OrderStatus.known(orderStatus: nil, actionCode: nil, actionCodeDescription: nil).text, "orderStatus=-, actionCode=-")
        XCTAssertEqual(OrderStatus.notQueried.text, "not queried")
        XCTAssertEqual(OrderStatus.failed(httpStatus: 503, message: "HTTP 503: busy").text, "error: HTTP 503: busy")
    }
}
