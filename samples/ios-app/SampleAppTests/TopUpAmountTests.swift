import XCTest
@testable import SampleApp

final class TopUpAmountTests: XCTestCase {
    func test_default_is_100_euros_and_valid() {
        XCTAssertEqual(TopUpAmount().minor, 10000)
        XCTAssertTrue(TopUpAmount().valid)
    }

    func test_zero_and_a_lone_separator_are_not_valid() {
        XCTAssertEqual(TopUpAmount.parse("0").text, "0")
        XCTAssertFalse(TopUpAmount.parse("0").valid)
        XCTAssertEqual(TopUpAmount.parse(".").text, "0.")
        XCTAssertFalse(TopUpAmount.parse(".").valid)
    }

    func test_a_trailing_separator_still_counts() {
        XCTAssertEqual(TopUpAmount.parse("10.").minor, 1000)
        XCTAssertTrue(TopUpAmount.parse("10.").valid)
    }

    func test_text_from_the_system_keyboard_is_cleaned() {
        XCTAssertEqual(TopUpAmount.parse("12,345").text, "12.34")
        XCTAssertEqual(TopUpAmount.parse("12.3.4").text, "12.34")
        XCTAssertEqual(TopUpAmount.parse("0005").text, "5")
        XCTAssertEqual(TopUpAmount.parse(",5").text, "0.5")
        XCTAssertEqual(TopUpAmount.parse("-").text, "")
        XCTAssertEqual(TopUpAmount.parse("1234567").text, "123456")
    }

    func test_limits_are_10_to_1000_euros() {
        XCTAssertFalse(TopUpAmount.parse("9.99").valid)
        XCTAssertTrue(TopUpAmount.parse("10").valid)
        XCTAssertTrue(TopUpAmount.parse("1000").valid)
        XCTAssertFalse(TopUpAmount.parse("1000.01").valid)
    }

    func test_empty_has_no_amount() {
        XCTAssertNil(TopUpAmount.parse("").minor)
        XCTAssertFalse(TopUpAmount.parse("").valid)
    }

    func test_preset_sets_whole_euros() {
        XCTAssertEqual(TopUpAmount.preset(25000).text, "250")
        XCTAssertEqual(TopUpAmount.preset(1250).text, "12.5")
    }

    func test_unconfirmed_blocks_continue() {
        XCTAssertFalse(TopUpStatus.unconfirmed.canContinue(TopUpAmount()))
        XCTAssertTrue(TopUpStatus.declined(message: nil).canContinue(TopUpAmount()))
        XCTAssertTrue(TopUpStatus.editing.canContinue(TopUpAmount()))
        XCTAssertFalse(TopUpStatus.editing.canContinue(TopUpAmount.parse("5")))
    }

    func test_parse_reads_the_Arabic_decimal_separator() {
        XCTAssertEqual(TopUpAmount.parse("12\u{066B}5").text, "12.5")
    }

    func test_parse_reads_Arabic_Indic_and_Persian_digits_as_ASCII() {
        XCTAssertEqual(TopUpAmount.parse("\u{0661}\u{0662}\u{066B}\u{0665}").text, "12.5")
        XCTAssertEqual(TopUpAmount.parse("\u{06F3}\u{06F0}").text, "30")
    }

    func test_the_field_shows_what_the_amount_kept() {
        XCTAssertEqual(TopUpAmount.parse("12.345").shown(separator: ","), "12,34")
        XCTAssertEqual(TopUpAmount.parse("05").shown(separator: "."), "5")
    }
}
