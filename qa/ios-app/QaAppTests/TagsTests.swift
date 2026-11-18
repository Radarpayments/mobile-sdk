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
            guard comment != "android", !identifier.isEmpty, identifier != "qa_profile_select_<name>" else { return nil }
            return identifier
        }
    }

    func test_the_identifiers_are_exactly_the_contracts_for_ios() throws {
        let ios = try iosIdentifiers()
        XCTAssertEqual(Set(Tags.all), Set(ios))
        XCTAssertEqual(Tags.all.count, ios.count)
    }

    func test_a_profile_row_is_identified_by_its_name() {
        XCTAssertEqual(Tags.profileSelect("sandbox"), "qa_profile_select_sandbox")
    }

    func test_every_config_field_is_an_identifier_of_the_contract() {
        XCTAssertTrue(Set(Tags.fields).isSubset(of: Set(Tags.all)))
    }
}
