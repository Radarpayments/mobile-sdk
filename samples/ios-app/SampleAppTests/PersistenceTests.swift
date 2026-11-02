import XCTest
@testable import SampleApp

final class PersistenceTests: XCTestCase {
    func test_defaults_are_wellKnownExampleValues() {
        XCTAssertEqual(AppPrefsDefaults.baseUrl, "https://dev.bpcbt.com/payment")
        XCTAssertEqual(AppPrefsDefaults.mdOrder, "c400b41a-aa3d-43db-8727-ac4ca9e8f701")
    }

    func test_keys_are_stable_identifiers() {
        XCTAssertEqual(AppPrefsKeys.baseUrl, "base_url")
        XCTAssertEqual(AppPrefsKeys.mdOrder, "md_order")
    }
}
