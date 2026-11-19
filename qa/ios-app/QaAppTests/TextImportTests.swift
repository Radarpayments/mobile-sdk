@testable import QaApp
import XCTest

@MainActor
final class TextImportTests: XCTestCase {
    private let configJson = #"{"profile":"sandbox","order":{"amount":500}}"#
    private let profileJson = #"{"name":"sandbox","gatewayUrl":"https://dev.bpcbt.com/payment","api":"v2","apiKey":"key-1"}"#
    private var accepted: [[Intake]] = []

    private func typed(_ text: String) -> TextImport {
        let textImport = TextImport()
        textImport.edit(text)
        return textImport
    }

    private func submit(_ textImport: TextImport) -> Bool {
        textImport.submit { self.accepted.append($0) }
    }

    func test_import_is_off_while_the_text_is_empty_after_trimming() {
        XCTAssertFalse(typed("").canImport)
        XCTAssertFalse(typed(" \r\n\t ").canImport)
        XCTAssertFalse(typed("\u{FEFF} \n").canImport)
        XCTAssertTrue(typed("{").canImport)
        XCTAssertTrue(typed("\u{00A0}").canImport)
    }

    func test_blank_text_is_not_handed_on() {
        let textImport = typed("  \n")

        XCTAssertFalse(submit(textImport))
        XCTAssertEqual(accepted, [])
        XCTAssertNil(textImport.problem)
    }

    func test_a_configuration_is_handed_on_as_clipboard_text_is_and_the_screen_closes() {
        let textImport = typed("\n  \(configJson)\n")

        XCTAssertTrue(submit(textImport))
        XCTAssertEqual(accepted, [[.config(QaConfig(profile: "sandbox", order: OrderConfig(amount: 500)))]])
        XCTAssertNil(textImport.problem)
        XCTAssertEqual(textImport.text, "")
    }

    func test_a_profile_is_handed_on_to_be_confirmed() {
        let textImport = typed(profileJson)

        XCTAssertTrue(submit(textImport))
        let profile = StandProfile(name: "sandbox", gatewayUrl: "https://dev.bpcbt.com/payment", api: .v2, apiKey: "key-1")
        XCTAssertEqual(accepted, [[.profile(profile, needsConfirmation: true)]])
    }

    func test_a_rejection_keeps_the_text_and_says_why() throws {
        let text = #"{"order":{"amount":"five"}}"#
        let textImport = typed(text)

        XCTAssertFalse(submit(textImport))
        let rejection = try XCTUnwrap(intake(fromText: text).first)
        guard case let .rejected(problem) = rejection else { return XCTFail("\(rejection) is not a rejection") }
        XCTAssertEqual(accepted, [[rejection]])
        XCTAssertEqual(textImport.problem, problem)
        XCTAssertEqual(textImport.text, text)
    }

    func test_text_that_is_neither_json_nor_a_link_is_rejected_at_the_root() {
        let textImport = typed("payrdr")

        XCTAssertFalse(submit(textImport))
        XCTAssertEqual(textImport.problem?.field, "$")
    }

    func test_editing_the_text_clears_the_problem() {
        let textImport = typed("payrdr")
        _ = submit(textImport)

        textImport.edit("payrdr{")

        XCTAssertNil(textImport.problem)
    }

    func test_clearing_drops_the_text_and_the_problem() {
        let textImport = typed("payrdr")
        _ = submit(textImport)

        textImport.clear()

        XCTAssertEqual(textImport.text, "")
        XCTAssertNil(textImport.problem)
        XCTAssertFalse(textImport.canImport)
    }
}
