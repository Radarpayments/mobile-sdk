@testable import QaApp
import XCTest

final class AmountsTests: XCTestCase {
    func test_minor_units_read_as_major_units_with_the_currencys_digits() {
        XCTAssertEqual(minorToMajor(10000, digits: 2), "100.00")
        XCTAssertEqual(minorToMajor(250, digits: 2), "2.50")
        XCTAssertEqual(minorToMajor(5, digits: 2), "0.05")
        XCTAssertEqual(minorToMajor(0, digits: 2), "0.00")
        XCTAssertEqual(minorToMajor(1500, digits: 3), "1.500")
        XCTAssertEqual(minorToMajor(1500, digits: 0), "1500")
    }

    func test_major_units_are_digits_with_one_optional_point_or_comma() {
        XCTAssertEqual(majorToMinor("100", digits: 2), 10000)
        XCTAssertEqual(majorToMinor("100.00", digits: 2), 10000)
        XCTAssertEqual(majorToMinor("2.5", digits: 2), 250)
        XCTAssertEqual(majorToMinor("2,50", digits: 2), 250)
        XCTAssertEqual(majorToMinor("10.", digits: 2), 1000)
        XCTAssertEqual(majorToMinor("0.05", digits: 2), 5)
        XCTAssertEqual(majorToMinor("007", digits: 2), 700)
        XCTAssertEqual(majorToMinor("1500", digits: 0), 1500)
    }

    func test_anything_else_is_not_an_amount() {
        for text in ["", ".50", "2.505", "1.2.3", "+5", "-5", " 5", "5 ", "5 00", "５", "٥", "1e3", "2.5x"] {
            XCTAssertNil(majorToMinor(text, digits: 2), text)
        }
        XCTAssertNil(majorToMinor("15.0", digits: 0))
    }

    func test_more_than_eighteen_digits_is_not_an_amount() {
        XCTAssertEqual(majorToMinor("9999999999999999.99", digits: 2), 999_999_999_999_999_999)
        XCTAssertNil(majorToMinor("99999999999999999.99", digits: 2))
    }

    func test_an_amount_survives_the_round_trip() {
        for minor: Int64 in [0, 1, 99, 100, 10000, 999_999_999_999] {
            XCTAssertEqual(majorToMinor(minorToMajor(minor, digits: 2), digits: 2), minor)
        }
    }

    func test_every_currency_has_two_minor_digits() {
        for code in Currencies.codes {
            XCTAssertEqual(Currencies.minorDigits(code), 2, code)
        }
        XCTAssertEqual(Currencies.minorDigits("XYZ"), 2)
    }
}
