@testable import QaApp
import XCTest

final class JSONValueTests: XCTestCase {
    func test_a_document_is_parsed_into_values() {
        XCTAssertEqual(
            JSONValue.parse(" {\"a\":[1,-2.5e+3,true,false,null,\"x\\u00e9\\ud83d\\ude00\\n\"],\"b\":{}} "),
            .object([
                "a": .array([.number("1"), .number("-2.5e+3"), .bool(true), .bool(false), .null, .string("xé😀\n")]),
                "b": .object([:]),
            ])
        )
    }

    func test_a_number_keeps_its_spelling() {
        XCTAssertEqual(JSONValue.parse("[100.0,1e400]"), .array([.number("100.0"), .number("1e400")]))
        XCTAssertEqual(JSONValue.array([.number("100.0"), .number("1e400")]).text, "[100.0,1e400]")
    }

    func test_the_last_of_duplicate_keys_wins() {
        XCTAssertEqual(JSONValue.parse("{\"a\":1,\"a\":2}"), .object(["a": .number("2")]))
    }

    // kotlinx alone would read a bare word or a malformed number as a literal and keep a lone surrogate; Android checks
    // the text with isStrictJson first, so both platforms refuse these.
    func test_text_that_is_not_strict_json_is_rejected() {
        let texts = [
            "{\"a\":01}", "{\"a\":abc}", "{\"a\":+1}", "{\"a\":.5}", "{\"a\":1.}", "{\"a\":1e}", "{\"a\":-}", "{\"a\":NaN}",
            "{\"a\":tru}", "{\"a\":\"\\ud800\"}", "{\"a\":\"\\x\"}", "{\"a\":\"x\ty\"}", "{\"a\":[1,]}", "{\"a\" 1}",
            "{\"a\":1 2}", "\u{A0}{}", "{} x",
        ]
        for text in texts {
            XCTAssertNil(JSONValue.parse(text), text)
        }
    }

    func test_written_text_escapes_what_json_needs() {
        let value = JSONValue.object(["k": .string("\"\\\n\u{1}é")])
        XCTAssertEqual(value.text, "{\"k\":\"\\\"\\\\\\n\\u0001é\"}")
        XCTAssertEqual(JSONValue.parse(value.text), value)
    }
}
