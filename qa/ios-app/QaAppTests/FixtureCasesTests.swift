@testable import QaApp
import XCTest

/// Runs `qa/fixtures/cases.json`, the cases both apps decode the same way.
final class FixtureCasesTests: XCTestCase {
    private func cases() throws -> [JSONObject] {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .appendingPathComponent("../../fixtures/cases.json")
            .standardizedFileURL
        let text = try String(contentsOf: url, encoding: .utf8)
        guard case let .array(items) = try XCTUnwrap(JSONValue.parse(text)) else {
            throw XCTSkip("cases.json is not an array")
        }
        return items.compactMap { item in
            if case let .object(fields) = item { return fields }
            return nil
        }
    }

    private func string(_ value: JSONValue?) -> String? {
        if case let .string(text) = value { return text }
        return nil
    }

    func test_the_fixture_file_has_cases_of_both_kinds() throws {
        let kinds = try cases().map { string($0["kind"]) }
        XCTAssertTrue(kinds.contains("config"))
        XCTAssertTrue(kinds.contains("profile"))
        XCTAssertGreaterThan(kinds.count, 40)
    }

    func test_every_fixture_case_decodes_as_the_contract_says() throws {
        for fixture in try cases() {
            let name = string(fixture["name"]) ?? "?"
            let text = try string(fixture["inputText"]) ?? XCTUnwrap(fixture["input"], name).text
            guard case let .object(expect) = fixture["expect"] else {
                XCTFail("\(name): no expect")
                continue
            }
            let written: Decoded<String>
            if string(fixture["kind"]) == "config" {
                written = QaConfigJson.decode(text).map(QaConfigJson.encode)
            } else {
                written = StandProfileJson.decode(text).map(StandProfileJson.encode)
            }
            switch written {
            case let .ok(document):
                guard let expected = expect["document"] else {
                    XCTFail("\(name): the case expects an error but the document was accepted")
                    continue
                }
                XCTAssertEqual(JSONValue.parse(document), expected, name)
            case let .invalid(problem):
                XCTAssertEqual(problem.field, string(expect["errorField"]), "\(name): \(problem.message)")
            }
        }
    }
}
