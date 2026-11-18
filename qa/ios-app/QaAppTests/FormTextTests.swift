@testable import QaApp
import XCTest

final class FormTextTests: XCTestCase {
    private let pin = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    private func field<T>(_ decoded: Decoded<T>) -> String? {
        guard case let .invalid(problem) = decoded else { return nil }
        return problem.field
    }

    func test_names_are_split_on_commas_and_trimmed() {
        XCTAssertEqual(namesFromText(" VERIFY ,FORCE_CREATE_BINDING,, "), ["VERIFY", "FORCE_CREATE_BINDING"])
    }

    func test_a_pin_line_is_split_at_the_first_equals_sign() {
        XCTAssertEqual(pinsFromText("dev.bpcbt.com=\(pin)"), .ok([Pin(host: "dev.bpcbt.com", pin: pin)]))
    }

    func test_blank_pin_lines_are_skipped() {
        XCTAssertEqual(pinsFromText("\n\n"), .ok([]))
        XCTAssertEqual(
            pinsFromText("a.example=\(pin)\r\n  \r\nb.example=\(pin)\n"),
            .ok([Pin(host: "a.example", pin: pin), Pin(host: "b.example", pin: pin)])
        )
        XCTAssertEqual(pinsFromText("a.example=\(pin)\rb.example=\(pin)").map(\.count), .ok(2))
    }

    func test_a_pin_line_without_a_host_names_the_pins_field() {
        XCTAssertEqual(field(pinsFromText("=AAAA")), "sdk.sslPinning.pins")
    }

    func test_a_pin_line_that_is_not_a_pin_names_the_pins_field() {
        XCTAssertEqual(field(pinsFromText("dev.bpcbt.com")), "sdk.sslPinning.pins")
        XCTAssertEqual(field(pinsFromText("dev.bpcbt.com=AAAA")), "sdk.sslPinning.pins")
    }

    func test_a_pin_line_whose_host_is_a_url_names_the_pins_field() {
        XCTAssertEqual(field(pinsFromText("https://dev.bpcbt.com=\(pin)")), "sdk.sslPinning.pins")
        XCTAssertEqual(field(pinsFromText("dev.bpcbt.com:443=\(pin)")), "sdk.sslPinning.pins")
    }

    func test_blank_extra_text_is_an_empty_object() {
        XCTAssertEqual(extraFromText("  "), .ok([:]))
    }

    func test_extra_text_that_is_not_an_object_names_the_field() {
        XCTAssertEqual(extraFromText("[1]"), .invalid(Problem(field: "order.extra", message: "must be a JSON object")))
        XCTAssertEqual(field(extraFromText("{")), "order.extra")
    }

    func test_extra_text_with_a_forbidden_key_names_the_field() {
        XCTAssertEqual(
            extraFromText("{\"Password\":\"x\"}"),
            .invalid(Problem(field: "order.extra", message: "must not contain username, password, token"))
        )
    }

    func test_pins_and_names_survive_the_round_trip_through_text() {
        let pins = [Pin(host: "a.example", pin: pin), Pin(host: "b.example", pin: pin)]
        XCTAssertEqual(pinsFromText(pinsToText(pins)), .ok(pins))
        let names = ["VERIFY", "FORCE_CREATE_BINDING"]
        XCTAssertEqual(namesFromText(namesToText(names)), names)
        let extra: JSONObject = ["language": .string("en"), "n": .number("1.50")]
        XCTAssertEqual(extraFromText(extraToText(extra)), .ok(extra))
        XCTAssertEqual(extraFromText(extraToText([:])), .ok([:]))
    }
}
