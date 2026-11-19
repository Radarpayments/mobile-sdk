@testable import QaApp
import XCTest

final class TagsTests: XCTestCase {
    private func iosIdentifiers() throws -> [String] {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .appendingPathComponent("../../fixtures/identifiers.txt")
            .standardizedFileURL
        let text = try String(contentsOf: url, encoding: .utf8)
        return text.split(separator: "\n", omittingEmptySubsequences: true).compactMap { line in
            let parts = line.split(separator: "#", maxSplits: 1, omittingEmptySubsequences: false)
            let comment = parts.count > 1 ? parts[1].trimmingCharacters(in: .whitespaces).lowercased() : ""
            let identifier = parts[0].trimmingCharacters(in: .whitespaces)
            guard comment != "android", !identifier.isEmpty else { return nil }
            return identifier
        }
    }

    func test_the_identifiers_are_exactly_the_contracts_for_ios() throws {
        let fixed = try iosIdentifiers().filter { !$0.contains("<") }
        XCTAssertEqual(Set(Tags.all), Set(fixed))
        XCTAssertEqual(Tags.all.count, fixed.count)
    }

    func test_the_identifiers_that_end_in_a_value_are_exactly_the_contracts_patterns_for_ios() throws {
        let patterns = try iosIdentifiers().filter { $0.contains("<") }
        XCTAssertEqual(Set(Tags.patterns), Set(patterns))
        XCTAssertEqual(Tags.patterns.count, patterns.count)
    }

    func test_a_profile_row_is_identified_by_its_name() {
        XCTAssertEqual(Tags.profileSelect("sandbox"), "qa_profile_select_sandbox")
        XCTAssertEqual(Tags.profileEdit("sandbox"), "qa_profile_edit_sandbox")
    }

    func test_an_option_is_identified_by_its_value_in_snake_case() {
        XCTAssertEqual(Tags.option(Tags.orderCurrency, "EUR"), "qa_order_currency_eur")
        XCTAssertEqual(Tags.option(Tags.sdkTheme, "largeText"), "qa_sdk_theme_large_text")
        XCTAssertEqual(Tags.option(Tags.orderSource, "mdOrder"), "qa_order_source_md_order")
        XCTAssertEqual(Tags.option(Tags.sdkLocale, nil), "qa_sdk_locale_none")
        XCTAssertEqual(Tags.option(Tags.sdkApplePayCountryCode, ""), "qa_sdk_apple_pay_country_code_none")
        XCTAssertEqual(Tags.feature("FORCE_TDS"), "qa_order_feature_force_tds")
        XCTAssertEqual(
            Tags.option(Tags.sdkApplePayMerchantCapabilities, "THREE_D_SECURE"),
            "qa_sdk_apple_pay_merchant_capabilities_three_d_secure"
        )
        XCTAssertEqual(Tags.feature("my feature--2"), "qa_order_feature_my_feature_2")
        XCTAssertEqual(Tags.feature("ÉTAPE"), "qa_order_feature__tape")
    }

    func test_every_config_field_is_an_identifier_of_the_contract() {
        XCTAssertTrue(Set(Tags.fields).isSubset(of: Set(Tags.all)))
    }
}
