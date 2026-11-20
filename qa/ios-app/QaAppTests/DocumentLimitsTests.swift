@testable import QaApp
import XCTest

final class DocumentLimitsTests: XCTestCase {
    /// The root object is level 1, so depth n is n objects nested in each other.
    private func nested(_ depth: Int) -> String {
        String(repeating: "{\"a\":", count: depth - 1) + "{}" + String(repeating: "}", count: depth - 1)
    }

    /// 6 bytes of `{"a":"`, the filler, 2 bytes of `"}`; 'é' is two bytes of UTF-8.
    private func sized(_ bytes: Int) -> String {
        let filler = bytes - 8
        return "{\"a\":\"" + String(repeating: "é", count: filler / 2) + String(repeating: "x", count: filler % 2) + "\"}"
    }

    private func problem<T>(_ decoded: Decoded<T>, file: StaticString = #filePath, line: UInt = #line) -> Problem {
        guard case let .invalid(problem) = decoded else {
            XCTFail("the document was accepted", file: file, line: line)
            return Problem(field: "", message: "")
        }
        return problem
    }

    func test_a_document_32_levels_deep_is_read() {
        XCTAssertEqual(QaConfigJson.decode(nested(32)), .ok(QaConfig()))
        let arrays = "{\"a\":" + String(repeating: "[", count: 30) + "{}" + String(repeating: "]", count: 30) + "}"
        XCTAssertEqual(QaConfigJson.decode(arrays), .ok(QaConfig()))
    }

    func test_a_document_33_levels_deep_is_rejected_at_the_root() {
        XCTAssertEqual(
            problem(QaConfigJson.decode(nested(33))),
            Problem(field: "$", message: "must nest at most 32 levels deep")
        )
        let arrays = "{\"a\":" + String(repeating: "[", count: 32) + "1" + String(repeating: "]", count: 32) + "}"
        XCTAssertEqual(problem(QaConfigJson.decode(arrays)).field, "$")
    }

    func test_a_very_deep_document_is_rejected_without_overflowing_the_stack() {
        XCTAssertEqual(problem(QaConfigJson.decode(nested(100_000))).field, "$")
        XCTAssertEqual(problem(QaConfigJson.decode(String(repeating: "[", count: 100_000))).field, "$")
        XCTAssertEqual(problem(StandProfileJson.decode(nested(100_000))).field, "$")
    }

    func test_brackets_inside_strings_do_not_count() {
        XCTAssertEqual(QaConfigJson.decode("{\"a\":\"" + String(repeating: "[{", count: 100) + "\"}"), .ok(QaConfig()))
        XCTAssertEqual(QaConfigJson.decode("{\"a\":\"\\\"" + String(repeating: "[{", count: 100) + "\"}"), .ok(QaConfig()))
    }

    func test_an_escaped_backslash_ends_before_the_quote_so_the_brackets_after_the_string_count() {
        let text = "{\"a\":\"\\\\\",\"b\":" + nested(32) + "}"
        XCTAssertEqual(problem(QaConfigJson.decode(text)).field, "$")
    }

    func test_a_document_of_65536_bytes_is_read_and_one_of_65537_is_rejected() {
        XCTAssertEqual(sized(65536).utf8.count, 65536)
        XCTAssertEqual(sized(65536).utf16.count, 32772)
        XCTAssertEqual(QaConfigJson.decode(sized(65536)), .ok(QaConfig()))
        XCTAssertEqual(
            problem(QaConfigJson.decode(sized(65537))),
            Problem(field: "$", message: "must be at most 65536 bytes of UTF-8")
        )
        XCTAssertEqual(problem(StandProfileJson.decode(sized(65537))).field, "$")
    }

    func test_a_character_outside_the_bmp_counts_four_bytes() {
        let emoji = "{\"a\":\"" + String(repeating: "😀", count: 4) + "\"}"
        XCTAssertNil(limitProblem(emoji, maxBytes: 24))
        XCTAssertNotNil(limitProblem(emoji, maxBytes: 23))
    }

    func test_a_limit_problem_does_not_echo_the_text() {
        XCTAssertFalse(problem(QaConfigJson.decode(sized(65537))).message.contains("é"))
    }

    func test_the_forms_extra_counts_its_depth_from_its_place_in_the_document() {
        // `extra` is level 3 of a config: the root, `order`, then `extra`.
        guard case let .ok(document) = QaConfigJson.decode("{\"order\":{\"extra\":\(nested(30))}}") else {
            return XCTFail("a document with extra 30 levels deep was rejected")
        }
        XCTAssertEqual(extraFromText(nested(30)), .ok(document.order.extra))
        XCTAssertEqual(problem(QaConfigJson.decode("{\"order\":{\"extra\":\(nested(31))}}")).field, "$")
        XCTAssertEqual(
            problem(extraFromText(nested(31))),
            Problem(field: "order.extra", message: "must nest at most 30 levels deep")
        )
        XCTAssertEqual(problem(extraFromText(nested(100_000))).field, "order.extra")
        XCTAssertEqual(problem(extraFromText(sized(65537))).field, "order.extra")
    }
}
