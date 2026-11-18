import Foundation
@testable import QaApp
import XCTest

final class RunReportTests: MerchantTestCase {
    private let app = AppInfo(appVersion: "1.0.0-beta.1 (7)", sdkVersion: "4.0.0-beta.1", device: "Google Pixel 8", os: "Android 15 (API 35)")
    private let config = QaConfig(profile: "v1")
    private let utc = TimeZone(identifier: "UTC")!

    private func reportOf(_ profile: StandProfile, _ outcome: RunOutcome, _ entries: [JournalEntry] = [], _ zone: TimeZone? = nil) -> String {
        runReport(app: app, finished: FinishedRun(outcome: outcome, config: config, profile: profile), entries: entries, timeZone: zone ?? utc)
    }

    private func completed(_ status: OrderStatus = .known(orderStatus: 2, actionCode: 0, actionCodeDescription: "Approved")) -> RunOutcome {
        RunOutcome(type: .completed, mdOrder: "abc", sessionId: nil, error: nil, httpStatus: nil, status: status)
    }

    private func entry(_ millis: Int64, _ level: LogLevel, _ tag: String, _ message: String) -> JournalEntry {
        JournalEntry(time: Date(timeIntervalSince1970: Double(millis) / 1000), level: level, tag: tag, message: message)
    }

    private func assertContains(_ text: String, _ part: String, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertTrue(text.contains(part), "\(part)\n--- not in ---\n\(text)", file: file, line: line)
    }

    func test_the_report_names_the_versions_the_device_the_profile_and_the_result() {
        let report = reportOf(v1, completed())

        assertContains(report, "== Versions ==\nQA app: 1.0.0-beta.1 (7)\nSDK: 4.0.0-beta.1")
        assertContains(report, "== Device ==\nGoogle Pixel 8\nAndroid 15 (API 35)")
        assertContains(report, "== Profile ==\nName: v1\nGateway URL: https://dev.bpcbt.com/payment\nAPI: v1")
        assertContains(report, "== Result ==\nqa_result_type: completed\nmdOrder: abc\nSession id: -")
        assertContains(report, "== Order status ==\norderStatus=2, actionCode=0, Approved")
    }

    func test_the_sections_come_in_the_contracts_order() {
        let titles = reportOf(v1, completed()).split(separator: "\n").filter { $0.hasPrefix("== ") }

        XCTAssertEqual(
            titles,
            ["== Versions ==", "== Device ==", "== Profile ==", "== Configuration ==", "== Result ==", "== Order status ==", "== Journal =="]
        )
    }

    func test_the_report_prints_the_config_as_json() {
        assertContains(reportOf(v1, completed()), "== Configuration ==\n\(QaConfigJson.encode(config))\n")
    }

    func test_the_report_prints_the_journal_lines() {
        let report = reportOf(v1, completed(), [entry(3_723_004, .info, "merchant", "← 200 {}")])

        assertContains(report, "== Journal ==\nTime zone: UTC+00:00\n01:02:03.004 INFO merchant: ← 200 {}")
    }

    func test_the_report_of_a_failed_run_carries_the_error_and_the_http_status() {
        let outcome = RunOutcome(type: .registrationFailed, mdOrder: nil, sessionId: nil, error: "Access denied", httpStatus: 200, status: .notQueried)

        let report = reportOf(v1, outcome)

        assertContains(report, "qa_result_type: registration_failed")
        assertContains(report, "Error: Access denied")
        assertContains(report, "HTTP status: 200")
        assertContains(report, "== Order status ==\nnot queried: the checkout did not run")
    }

    func test_the_report_of_a_sheet_that_did_not_open_says_the_checkout_did_not_run() {
        let outcome = RunOutcome(type: .sdkError, mdOrder: "abc", sessionId: nil, error: "PayrdrCheckoutError", httpStatus: nil, status: .notQueried)

        let report = reportOf(v1, outcome)

        assertContains(report, "qa_result_type: sdk_error")
        assertContains(report, "Error: PayrdrCheckoutError\nHTTP status: -")
        assertContains(report, "== Order status ==\nnot queried: the checkout did not run")
    }

    func test_the_report_says_why_a_status_was_not_asked() {
        assertContains(reportOf(v2, completed(.notQueried)), "not queried: API v2 has no status query")
        var noPassword = v1
        noPassword.password = ""
        assertContains(reportOf(noPassword, completed(.notQueried)), "not queried: the profile has no login and password")
        let nameless = RunOutcome(type: .canceled, mdOrder: nil, sessionId: nil, error: nil, httpStatus: nil, status: .notQueried)
        assertContains(reportOf(v1, nameless), "not queried: the checkout named no order")
    }

    func test_a_failed_status_query_is_printed_with_its_error() {
        assertContains(
            reportOf(v1, completed(.failed(httpStatus: 200, message: "Order not found"))),
            "== Order status ==\nerror: Order not found"
        )
    }

    func test_the_report_of_a_real_exchange_holds_no_secret() async throws {
        // Each call's gateway quotes the secret it was sent, as a stand's error page may.
        StubURLProtocol.reset { request in
            let quoted = request.url?.path.hasSuffix("/sessions") == true ? "key-1" : "password-1"
            return .answer(status: 200, body: #"{"orderId":"abc","id":"session-1","echo":"\#(quoted)"}"#)
        }
        let merchant = merchant()
        _ = try await merchant.register(order: config.order, profile: v1)
        _ = try await merchant.register(order: config.order, profile: v2)

        let report = reportOf(v1, completed(), journal.snapshot())

        XCTAssertFalse(report.contains("password-1"), report)
        XCTAssertFalse(report.contains("key-1"), report)
        assertContains(report, "dev.bpcbt.com")
        assertContains(report, "/rest/register.do")
    }

    func test_the_report_describes_the_config_and_the_profile_of_its_own_run() {
        let launched = QaConfig(profile: "v2", order: OrderConfig(amount: 4200))

        let report = runReport(app: app, finished: FinishedRun(outcome: completed(), config: launched, profile: v2), entries: [], timeZone: utc)

        assertContains(report, "== Configuration ==\n\(QaConfigJson.encode(launched))\n")
        assertContains(report, "Name: v2")
        assertContains(report, "API: v2")
    }

    func test_the_journal_names_its_time_zone() {
        let moscow = TimeZone(identifier: "Europe/Moscow")!

        assertContains(
            reportOf(v1, completed(), [entry(0, .info, "qa", "a line")], moscow),
            "== Journal ==\nTime zone: UTC+03:00\n03:00:00.000 INFO qa: a line"
        )
    }

    func test_a_utc_offset_is_printed_by_hand() {
        let epoch = Date(timeIntervalSince1970: 0)
        XCTAssertEqual(utcOffset(TimeZone(identifier: "UTC")!, at: epoch), "UTC+00:00")
        XCTAssertEqual(utcOffset(TimeZone(identifier: "Asia/Kolkata")!, at: epoch), "UTC+05:30")
        XCTAssertEqual(utcOffset(TimeZone(identifier: "America/St_Johns")!, at: epoch), "UTC-03:30")
    }

    // The README's example is Android's output; iOS prints every line of it the same but for the JSON document, which
    // is the platform's own spelling of the same configuration.
    func test_the_report_is_the_readmes_example_but_for_the_configuration_json() throws {
        let mdOrder = "8f1e4c2a-0b3d-4e5f-9a6b-7c8d9e0f1a2b"
        let launched = QaConfig(profile: "dev")
        let finished = FinishedRun(
            outcome: RunOutcome(
                type: .completed,
                mdOrder: mdOrder,
                sessionId: nil,
                error: nil,
                httpStatus: nil,
                status: .known(orderStatus: 2, actionCode: 0, actionCodeDescription: "Approved")
            ),
            config: launched,
            profile: StandProfile(name: "dev", gatewayUrl: "https://dev.bpcbt.com/payment", login: "login-1", password: "password-1")
        )
        let register = "https://dev.bpcbt.com/payment/rest/register.do"
        let entries = [
            entry(45_296_789, .info, "merchant", "→ POST \(register) userName=login-1&password=***"),
            entry(45_297_102, .info, "merchant", "← 200 {\"orderId\":\"\(mdOrder)\"}"),
        ]

        let report = runReport(app: app, finished: finished, entries: entries, timeZone: utc)

        let example = try readmeExample()
        let opening = "== Configuration ==\n"
        let closing = "\n\n== Result ==\n"
        let start = try XCTUnwrap(example.range(of: opening))
        let end = try XCTUnwrap(example.range(of: closing, range: start.upperBound..<example.endIndex))
        let expected = example.replacingCharacters(in: start.upperBound..<end.lowerBound, with: QaConfigJson.encode(launched))
        XCTAssertEqual(report, expected)
        let androidJson = String(example[start.upperBound..<end.lowerBound])
        XCTAssertEqual(json(androidJson), json(QaConfigJson.encode(launched)), "the README's document is not this configuration")
    }

    private func readmeExample() throws -> String {
        let url = URL(fileURLWithPath: #filePath).deletingLastPathComponent().appendingPathComponent("../../README.md")
        let readme = try String(contentsOf: url, encoding: .utf8)
        let fence = "```text\n"
        let section = try XCTUnwrap(readme.range(of: "\n### Run report\n"), "qa/README.md has no \"### Run report\" section")
        let start = try XCTUnwrap(
            readme.range(of: fence, range: section.upperBound..<readme.endIndex),
            "the \"Run report\" section of qa/README.md has no ```text block"
        )
        let end = try XCTUnwrap(
            readme.range(of: "\n```", range: start.upperBound..<readme.endIndex),
            "the ```text block of the \"Run report\" section is not closed"
        )
        return String(readme[start.upperBound...end.lowerBound])
    }
}
