@testable import QaApp
import XCTest

final class CheckoutsTests: XCTestCase {
    func test_a_checkout_on_screen_is_reused_whatever_is_wanted() {
        XCTAssertTrue(reusesCheckout(presenting: true, key: "a", wanted: "b"))
        XCTAssertTrue(reusesCheckout(presenting: true, key: nil, wanted: "b"))
    }

    func test_a_closed_checkout_is_reused_only_for_the_same_configuration_and_profile() {
        XCTAssertTrue(reusesCheckout(presenting: false, key: "a", wanted: "a"))
        XCTAssertFalse(reusesCheckout(presenting: false, key: "a", wanted: "b"))
        XCTAssertFalse(reusesCheckout(presenting: false, key: nil, wanted: "a"))
    }

    func test_canonically_equivalent_texts_are_different_keys() {
        XCTAssertFalse(reusesCheckout(presenting: false, key: "\u{212A}", wanted: "K"))
    }
}
