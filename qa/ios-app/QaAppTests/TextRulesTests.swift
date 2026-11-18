@testable import QaApp
import XCTest

// Android compares one UTF-16 char at a time: equal, equal upper cases, or equal lower cases of the upper cases, with
// Java's one-to-one mappings.
final class TextRulesTests: XCTestCase {
    func test_dotted_capital_i_matches_i_as_in_java() {
        XCTAssertTrue(containsIgnoringCase("İstanbul", "istanbul"))
        XCTAssertTrue(containsIgnoringCase("istanbul", "İSTANBUL"))
        XCTAssertTrue(containsIgnoringCase("İ", "I"))
        XCTAssertTrue(equalsIgnoringCase("apİKey", "apikey"))
    }

    func test_dotless_i_matches_i() {
        XCTAssertTrue(containsIgnoringCase("ıd", "ID"))
        XCTAssertTrue(equalsIgnoringCase("ıd", "id"))
    }

    func test_sharp_s_matches_its_capital_but_never_ss() {
        XCTAssertTrue(containsIgnoringCase("straße", "STRAẞE"))
        XCTAssertFalse(containsIgnoringCase("straße", "STRASSE"))
        XCTAssertFalse(containsIgnoringCase("strasse", "straße"))
    }

    func test_long_s_and_the_kelvin_sign_match_their_latin_letters() {
        XCTAssertTrue(containsIgnoringCase("ſtatus", "STATUS"))
        XCTAssertTrue(containsIgnoringCase("status", "ſ"))
        XCTAssertTrue(containsIgnoringCase("\u{212A}ZT", "kzt"))
        XCTAssertTrue(equalsIgnoringCase("\u{212A}ey", "key"))
    }

    func test_letters_beyond_the_basic_plane_match_only_themselves() {
        XCTAssertTrue(containsIgnoringCase("a\u{10428}b", "A\u{10428}B"))
        XCTAssertFalse(containsIgnoringCase("\u{10400}", "\u{10428}"))
    }

    func test_an_empty_query_is_contained_in_any_text() {
        XCTAssertTrue(containsIgnoringCase("", ""))
        XCTAssertTrue(containsIgnoringCase("anything", ""))
        XCTAssertFalse(containsIgnoringCase("", "a"))
    }

    func test_a_query_longer_than_the_text_is_not_contained() {
        XCTAssertFalse(containsIgnoringCase("ab", "abc"))
    }

    func test_the_byte_search_finds_what_the_search_by_characters_finds() {
        let texts = ["", "a", "Merchant", "POST register.do", "x" + String(repeating: "Ab", count: 40) + "c", "s S", "aaab",
                     "→ POST", "ſtatus", "\u{212A}ZT", "İd ıd", "é Ab ← ab", "\u{1F600}ab", "straße STRAẞE", "Σσς"]
        let ascii = (0x00 ... 0x7F).map { String(Unicode.Scalar(UInt8($0))) }
        let queries = ascii + ["ab", "AB", "bA", "abc", "register.DO", "  ", "ſ", "S S", "→", "ẞ", "σ", "ς", "\u{1F600}", "é", "É"]
        for text in texts + ascii {
            for query in queries {
                XCTAssertEqual(
                    containsIgnoringCase(text, query),
                    containsIgnoringCaseScalarByScalar(text, query),
                    "query \(query.unicodeScalars.map(\.value)) in \(text.unicodeScalars.map(\.value))"
                )
            }
        }
    }

    func test_filtering_a_full_journal_is_fast() {
        let message = String(repeating: "← 200 {\"orderId\":\"6f1c0a8e\",\"formUrl\":\"https://dev.bpcbt.com/payment\"} ", count: 16)
        let entries = (0 ..< 2000).map { JournalEntry(time: Date(), level: .info, tag: "merchant", message: "\(message)\($0)") }
        let start = Date()
        for query in ["o", "or", "ord", "orde", "order", "orderid", "nothing"] {
            _ = filtered(entries, minLevel: .debug, query: query)
        }
        XCTAssertLessThan(Date().timeIntervalSince(start), 0.35)
    }
}
