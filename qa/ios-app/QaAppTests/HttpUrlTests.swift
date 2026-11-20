@testable import QaApp
import XCTest

final class HttpUrlTests: XCTestCase {
    func test_whitespace_is_what_unicode_calls_white_space() {
        // Lone surrogates (U+D800–U+DFFF) cannot be in a Swift string, so they are skipped.
        for code in UInt32(0)...0xFFFF {
            guard let scalar = Unicode.Scalar(code) else { continue }
            let url = "https://dev.bpcbt.com/a\(Character(scalar))b"
            XCTAssertEqual(isHttpUrl(url), !scalar.properties.isWhitespace, String(format: "U+%04X", code))
        }
    }

    func test_a_host_with_a_space_ends_before_it() {
        XCTAssertFalse(isHttpUrl("https://dev bpcbt.com"))
        XCTAssertFalse(isHttpUrl("https:// dev.bpcbt.com"))
        XCTAssertTrue(isHttpUrl("https://dev.bpcbt.com/payment"))
    }

    func test_a_line_break_at_the_end_is_not_part_of_a_url() {
        XCTAssertFalse(isHttpUrl("https://dev.bpcbt.com\n"))
        XCTAssertFalse(isHttpUrl("https://dev.bpcbt.com\u{2028}"))
    }

    func test_the_scheme_ignores_ascii_case_only() {
        XCTAssertTrue(isHttpUrl("HtTpS://h"))
        XCTAssertTrue(isHttpUrl("HTTP://h"))
        XCTAssertFalse(isHttpUrl("http\u{17F}://h"))
        XCTAssertFalse(isHttpUrl("https:///path"))
        XCTAssertFalse(isHttpUrl("https://?q"))
        XCTAssertFalse(isHttpUrl("https://#f"))
        XCTAssertTrue(isHttpUrl("https://h/?q#f"))
    }
}
