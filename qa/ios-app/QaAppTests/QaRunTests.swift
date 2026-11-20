import Foundation
@testable import QaApp
import RadarpaymentsSDKPayment
import XCTest

private let statusPath = "/rest/getOrderStatusExtended.do"
private let approved = #"{"errorCode":"0","orderStatus":2,"actionCode":0,"actionCodeDescription":"Approved"}"#

// The gateway every test talks to unless it says otherwise; `special` answers the paths it knows, nil for the rest.
private func serve(_ special: @escaping (String) -> StubReply? = { _ in nil }) {
    StubURLProtocol.reset { request in
        let path = request.url?.path ?? ""
        if let reply = special(path) { return reply }
        return usual(path)
    }
}

private func usual(_ path: String) -> StubReply {
    if path.hasSuffix(statusPath) { return .answer(status: 200, body: approved) }
    if path.hasSuffix("/sessions") { return .answer(status: 200, body: #"{"id":"session-1"}"#) }
    return .answer(status: 200, body: #"{"orderId":"abc"}"#)
}

private struct Boom: LocalizedError {
    var errorDescription: String? { "boom" }
}

private struct Silent: Error {}

@MainActor
final class QaRunTests: MerchantTestCase {
    private var opened: [OpenOrder] = []
    private var presented: [(QaConfig, StandProfile)] = []
    private var presentCalls = 0
    // What the sheet does when asked to open, by call number; it opens unless told otherwise.
    private var onPresent: (Int, OpenOrder) -> Bool = { _, _ in true }

    private lazy var run = QaRun(merchant: merchant(), journal: journal) { [unowned self] order, config, profile in
        opened.append(order)
        presented.append((config, profile))
        let call = presentCalls
        presentCalls += 1
        return onPresent(call, order)
    }

    private let register = QaConfig(profile: "v1")
    private let session = QaConfig(profile: "v2")

    nonisolated override func setUp() {
        super.setUp()
        serve()
    }

    private func existing(_ mdOrder: String) -> QaConfig {
        QaConfig(profile: "v1", order: OrderConfig(source: .mdOrder, mdOrder: mdOrder))
    }

    private func existingSession(_ sessionId: String) -> QaConfig {
        QaConfig(profile: "v1", order: OrderConfig(source: .sessionId, sessionId: sessionId))
    }

    private var outcome: RunOutcome? {
        if case let .finished(finished) = run.state { return finished.outcome }
        return nil
    }

    private func finished(file: StaticString = #filePath, line: UInt = #line) async throws -> RunOutcome {
        await run.waitUntilSettled()
        return try XCTUnwrap(outcome, "\(run.state)", file: file, line: line)
    }

    private func requests(to path: String) -> [RecordedRequest] {
        StubURLProtocol.requests.filter { $0.url?.hasSuffix(path) == true }
    }

    private var journalEntries: [JournalEntry] {
        journal.snapshot()
    }

    func test_a_launch_without_a_profile_is_invalid() async {
        run.launch(config: register, profile: nil)
        await run.waitUntilSettled()

        XCTAssertEqual(run.state, .invalid(Problem(field: "profile", message: "select a stored profile")))
        XCTAssertTrue(StubURLProtocol.requests.isEmpty)
    }

    func test_a_registered_order_opens_with_its_md_order() async {
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()

        XCTAssertEqual(run.state, .inSheet(OpenOrder(mdOrder: "abc", sessionId: nil)))
        XCTAssertEqual(opened, [OpenOrder(mdOrder: "abc", sessionId: nil)])
        XCTAssertEqual(requests(to: "/rest/register.do").count, 1)
        XCTAssertEqual(StubURLProtocol.requests.count, 1)
    }

    func test_a_registered_session_opens_with_its_session_id() async {
        run.launch(config: session, profile: v2)
        await run.waitUntilSettled()

        XCTAssertEqual(run.state, .inSheet(OpenOrder(mdOrder: nil, sessionId: "session-1")))
        XCTAssertEqual(opened, [OpenOrder(mdOrder: nil, sessionId: "session-1")])
    }

    func test_the_sheet_is_given_the_runs_config_and_profile() async {
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()

        XCTAssertEqual(presented.map(\.0), [register])
        XCTAssertEqual(presented.map(\.1), [v1])
    }

    func test_an_existing_md_order_opens_without_a_request_and_without_credentials() async {
        var noLogin = v1
        noLogin.login = ""

        run.launch(config: existing("given"), profile: noLogin)
        XCTAssertEqual(run.state, .inSheet(OpenOrder(mdOrder: "given", sessionId: nil)))
        await run.waitUntilSettled()

        XCTAssertEqual(opened, [OpenOrder(mdOrder: "given", sessionId: nil)])
        XCTAssertTrue(StubURLProtocol.requests.isEmpty)
    }

    func test_an_existing_session_opens_without_a_request() async {
        run.launch(config: existingSession("session-2"), profile: v1)
        await run.waitUntilSettled()

        XCTAssertEqual(run.state, .inSheet(OpenOrder(mdOrder: nil, sessionId: "session-2")))
        XCTAssertTrue(StubURLProtocol.requests.isEmpty)
    }

    func test_a_failed_registration_finishes_the_run_without_a_sheet() async throws {
        serve { _ in .answer(status: 200, body: #"{"errorCode":"5","errorMessage":"Access denied"}"#) }

        run.launch(config: register, profile: v1)

        let ended = try await finished()

        XCTAssertEqual(
            ended,
            RunOutcome(type: .registrationFailed, mdOrder: nil, sessionId: nil, error: "Access denied", httpStatus: 200, status: .notQueried)
        )
        XCTAssertTrue(opened.isEmpty)
    }

    func test_a_finished_run_keeps_the_config_and_the_profile_it_was_launched_with() async throws {
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()
        run.onResult(CheckoutResultCompleted(orderId: "abc"))
        await run.waitUntilSettled()
        guard case let .finished(completed) = run.state else { return XCTFail("\(run.state)") }
        XCTAssertEqual(completed.config, register)
        XCTAssertEqual(completed.profile, v1)

        serve { _ in .answer(status: 200, body: #"{"errorCode":"5","errorMessage":"Access denied"}"#) }
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()
        guard case let .finished(refused) = run.state else { return XCTFail("\(run.state)") }
        XCTAssertEqual(refused.config, register)
        XCTAssertEqual(refused.profile, v1)

        onPresent = { _, _ in false }
        run.launch(config: existing("given"), profile: v2)
        await run.waitUntilSettled()
        guard case let .finished(unopened) = run.state else { return XCTFail("\(run.state)") }
        XCTAssertEqual(unopened.outcome.type, .sdkError)
        XCTAssertEqual(unopened.config, existing("given"))
        XCTAssertEqual(unopened.profile, v2)
    }

    func test_a_finished_state_prints_no_credentials() async {
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()
        run.onResult(CheckoutResultCompleted(orderId: "abc"))
        await run.waitUntilSettled()
        let v1Text = String(describing: run.state)
        XCTAssertTrue(v1Text.contains("dev.bpcbt.com"), v1Text)
        XCTAssertFalse(v1Text.contains("login-1"))
        XCTAssertFalse(v1Text.contains("password-1"))

        run.launch(config: session, profile: v2)
        await run.waitUntilSettled()
        run.onResult(CheckoutResultCompleted(orderId: "abc"))
        await run.waitUntilSettled()
        let v2Text = String(describing: run.state)
        XCTAssertTrue(v2Text.contains("dev.bpcbt.com"), v2Text)
        XCTAssertFalse(v2Text.contains("key-1"))
    }

    func test_a_second_launch_during_the_registration_is_ignored() async {
        let gate = DispatchSemaphore(value: 0)
        serve { path in .gated(gate, usual(path)) }

        run.launch(config: register, profile: v1)
        run.launch(config: register, profile: v1)
        XCTAssertEqual(run.state, .registering)
        gate.signal()
        await run.waitUntilSettled()
        XCTAssertEqual(run.state, .inSheet(OpenOrder(mdOrder: "abc", sessionId: nil)))
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()

        XCTAssertEqual(requests(to: "/rest/register.do").count, 1)
        XCTAssertEqual(opened.count, 1)
    }

    func test_a_launch_while_the_sheet_is_open_is_ignored() async {
        run.launch(config: existing("given"), profile: v1)
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()

        XCTAssertEqual(run.state, .inSheet(OpenOrder(mdOrder: "given", sessionId: nil)))
        XCTAssertTrue(StubURLProtocol.requests.isEmpty)
        XCTAssertEqual(opened.count, 1)
    }

    func test_a_launch_while_the_status_is_asked_is_ignored() async throws {
        let gate = DispatchSemaphore(value: 0)
        serve { path in path.hasSuffix(statusPath) ? .gated(gate, usual(path)) : nil }

        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()
        run.onResult(CheckoutResultCompleted(orderId: "abc"))
        run.launch(config: register, profile: v1)
        XCTAssertEqual(run.state, .queryingStatus)
        gate.signal()

        let ended = try await finished()

        XCTAssertEqual(ended.type, .completed)
        XCTAssertEqual(requests(to: "/rest/register.do").count, 1)
    }

    func test_a_completed_sheet_asks_the_status_of_the_registered_order() async throws {
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()
        run.onResult(CheckoutResultCompleted(orderId: "abc"))

        let ended = try await finished()

        XCTAssertEqual(
            ended,
            RunOutcome(
                type: .completed,
                mdOrder: "abc",
                sessionId: nil,
                error: nil,
                httpStatus: nil,
                status: .known(orderStatus: 2, actionCode: 0, actionCodeDescription: "Approved")
            )
        )
        XCTAssertEqual(requests(to: statusPath).map { $0.form["orderId"] }, ["abc"])
    }

    func test_a_session_run_takes_the_md_order_from_the_result() async throws {
        run.launch(config: existingSession("session-2"), profile: v1)
        run.onResult(CheckoutResultCompleted(orderId: "from-result"))

        let outcome = try await finished()
        XCTAssertEqual(outcome.mdOrder, "from-result")
        XCTAssertEqual(outcome.sessionId, "session-2")
        XCTAssertEqual(StubURLProtocol.requests.count, 1)
        XCTAssertEqual(StubURLProtocol.requests.first?.form["orderId"], "from-result")
    }

    func test_a_canceled_sheet_finishes_canceled() async throws {
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()
        run.onResult(CheckoutResultCanceled(orderId: "abc"))

        let outcome = try await finished()
        XCTAssertEqual(outcome.type, .canceled)
        XCTAssertNil(outcome.error)
    }

    func test_an_unknown_result_carries_its_cause() async throws {
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()
        run.onResult(CheckoutResultUnknown(orderId: "abc", cause: SdkException(message: "no network", cause: nil)))

        let outcome = try await finished()
        XCTAssertEqual(outcome.type, .unknown)
        XCTAssertEqual(outcome.error, "no network")
        XCTAssertNil(outcome.httpStatus)
    }

    func test_a_failed_sheet_carries_its_error_and_the_status() async throws {
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()
        run.onResult(CheckoutResultFailed(orderId: "abc", error: SdkException(message: "Payment declined: 116", cause: nil)))

        let outcome = try await finished()
        XCTAssertEqual(outcome.type, .failed)
        XCTAssertEqual(outcome.error, "Payment declined: 116")
        XCTAssertEqual(outcome.status, .known(orderStatus: 2, actionCode: 0, actionCodeDescription: "Approved"))
    }

    func test_a_gateway_error_of_the_sdk_carries_its_http_status() async throws {
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()
        let error = SdkPaymentApiException(httpStatus: KotlinInt(int: 502), errorCode: nil, message: "Bad gateway", cause: nil)
        run.onResult(CheckoutResultFailed(orderId: "abc", error: error))

        let outcome = try await finished()
        XCTAssertEqual(outcome.error, "Bad gateway")
        XCTAssertEqual(outcome.httpStatus, 502)
    }

    func test_a_sheet_that_does_not_open_finishes_the_run_as_an_sdk_error() async throws {
        onPresent = { _, _ in false }

        run.launch(config: register, profile: v1)

        let ended = try await finished()

        XCTAssertEqual(
            ended,
            RunOutcome(type: .sdkError, mdOrder: "abc", sessionId: nil, error: "PayrdrCheckoutError", httpStatus: nil, status: .notQueried)
        )
        XCTAssertEqual(journalEntries.last?.level, .error)
        XCTAssertEqual(journalEntries.last?.tag, "qa")
        XCTAssertEqual(journalEntries.last?.message, "The sheet threw PayrdrCheckoutError")
        XCTAssertEqual(requests(to: statusPath).count, 0)
    }

    func test_a_sheet_error_is_its_type_and_its_message() {
        func kotlin(_ message: String?) -> NSError {
            NSError(domain: "KotlinException", code: 0, userInfo: ["KotlinException": SdkException(message: message, cause: nil)])
        }
        XCTAssertEqual(sheetError(kotlin("boom")), "SdkException: boom")
        XCTAssertEqual(sheetError(kotlin(nil)), "SdkException")
        XCTAssertEqual(sheetError(kotlin("  \n")), "SdkException")
        XCTAssertEqual(sheetError(Boom()), "Boom: boom")
        XCTAssertEqual(sheetError(Silent()), "Silent")
        XCTAssertEqual(sheetError(PayrdrCheckoutError.notPresented), "PayrdrCheckoutError")
        XCTAssertEqual(sheetError(NSError(domain: "x", code: 1)), "NSError")
        XCTAssertEqual(sheetError(NSError(domain: "x", code: 1, userInfo: [NSLocalizedDescriptionKey: "denied"])), "NSError: denied")
    }

    func test_a_sheet_that_gives_up_after_delivering_its_result_is_still_journaled() async throws {
        onPresent = { [unowned self] _, _ in
            run.onResult(CheckoutResultCanceled(orderId: "given"))
            return false
        }

        run.launch(config: existing("given"), profile: v1)

        let ended = try await finished()

        XCTAssertEqual(ended.type, .canceled)
        XCTAssertTrue(journalEntries.map(\.message).contains("The sheet threw PayrdrCheckoutError"))
    }

    func test_the_journal_is_cleared_when_a_run_starts() async {
        journal.record(.info, tag: "earlier", message: "a line of the previous run")

        run.launch(config: existing("given"), profile: v1)
        await run.waitUntilSettled()

        XCTAssertEqual(journalEntries, [])
    }

    func test_an_invalid_launch_keeps_the_journal() {
        journal.record(.info, tag: "earlier", message: "a line of the previous run")

        run.launch(config: register, profile: nil)

        XCTAssertEqual(journalEntries.count, 1)
    }

    func test_a_result_with_no_run_in_the_sheet_is_ignored() async {
        run.onResult(CheckoutResultCompleted(orderId: "abc"))
        await run.waitUntilSettled()

        XCTAssertEqual(run.state, .idle)
        XCTAssertTrue(StubURLProtocol.requests.isEmpty)
    }

    func test_a_second_result_of_one_sheet_is_ignored() async throws {
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()
        run.onResult(CheckoutResultCompleted(orderId: "abc"))
        run.onResult(CheckoutResultCanceled(orderId: "abc"))

        let ended = try await finished()

        XCTAssertEqual(ended.type, .completed)
        XCTAssertEqual(requests(to: statusPath).count, 1)
    }

    func test_a_status_that_cannot_be_asked_finishes_not_queried() async throws {
        run.launch(config: session, profile: v2)
        await run.waitUntilSettled()
        run.onResult(CheckoutResultCompleted(orderId: "abc"))

        let outcome = try await finished()
        XCTAssertEqual(outcome.type, .completed)
        XCTAssertEqual(outcome.status, .notQueried)
        XCTAssertEqual(requests(to: statusPath).count, 0)
    }

    func test_a_failed_status_query_finishes_with_its_error() async throws {
        serve { path in
            path.hasSuffix(statusPath) ? .answer(status: 200, body: #"{"errorCode":"6","errorMessage":"Order not found"}"#) : nil
        }

        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()
        run.onResult(CheckoutResultCanceled(orderId: "abc"))

        let outcome = try await finished()
        XCTAssertEqual(outcome.type, .canceled)
        XCTAssertEqual(outcome.status, .failed(httpStatus: 200, message: "Order not found"))
    }

    func test_a_reset_returns_to_idle() async {
        run.launch(config: existing("given"), profile: v1)
        await run.waitUntilSettled()
        run.reset()

        XCTAssertEqual(run.state, .idle)
    }

    func test_a_registration_that_answers_after_a_reset_does_not_touch_the_next_run() async {
        let gate = DispatchSemaphore(value: 0)
        serve { path in .gated(gate, usual(path)) }
        let sent = expectation(description: "register.do sent")
        StubURLProtocol.notifyOnStart(sent)

        run.launch(config: register, profile: v1)
        await fulfillment(of: [sent], timeout: 5)
        run.reset()
        run.launch(config: existing("second"), profile: v1)
        XCTAssertEqual(run.state, .inSheet(OpenOrder(mdOrder: "second", sessionId: nil)))
        gate.signal()
        await run.waitUntilSettled()

        XCTAssertEqual(run.state, .inSheet(OpenOrder(mdOrder: "second", sessionId: nil)))
        XCTAssertEqual(opened, [OpenOrder(mdOrder: "second", sessionId: nil)])
        XCTAssertEqual(journalEntries, [])
    }

    func test_a_status_that_answers_after_a_reset_does_not_touch_the_next_run() async {
        let gate = DispatchSemaphore(value: 0)
        serve { path in path.hasSuffix(statusPath) ? .gated(gate, usual(path)) : nil }

        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()
        let sent = expectation(description: "status sent")
        StubURLProtocol.notifyOnStart(sent)
        run.onResult(CheckoutResultCompleted(orderId: "abc"))
        await fulfillment(of: [sent], timeout: 5)
        run.reset()
        run.launch(config: existing("second"), profile: v1)
        gate.signal()
        await run.waitUntilSettled()

        XCTAssertEqual(run.state, .inSheet(OpenOrder(mdOrder: "second", sessionId: nil)))
        XCTAssertEqual(journalEntries, [])
    }

    func test_a_registration_reset_before_it_starts_sends_nothing_into_the_next_runs_journal() async {
        run.launch(config: register, profile: v1)
        run.reset()
        run.launch(config: existing("second"), profile: v1)
        await run.waitUntilSettled()

        XCTAssertEqual(run.state, .inSheet(OpenOrder(mdOrder: "second", sessionId: nil)))
        XCTAssertEqual(journalEntries, [])
        XCTAssertTrue(StubURLProtocol.requests.isEmpty)
    }

    func test_a_status_query_reset_before_it_starts_sends_nothing_into_the_next_runs_journal() async {
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()
        let registered = StubURLProtocol.requests.count
        run.onResult(CheckoutResultCompleted(orderId: "abc"))
        run.reset()
        run.launch(config: existing("second"), profile: v1)
        await run.waitUntilSettled()

        XCTAssertEqual(run.state, .inSheet(OpenOrder(mdOrder: "second", sessionId: nil)))
        XCTAssertEqual(journalEntries, [])
        XCTAssertEqual(StubURLProtocol.requests.count, registered)
    }

    func test_a_sheet_of_a_reset_run_that_does_not_open_does_not_finish_the_next_run() async {
        onPresent = { [unowned self] call, _ in
            guard call == 0 else { return true }
            run.reset()
            run.launch(config: existing("second"), profile: v1)
            return false
        }

        run.launch(config: existing("first"), profile: v1)
        await run.waitUntilSettled()

        XCTAssertEqual(run.state, .inSheet(OpenOrder(mdOrder: "second", sessionId: nil)))
        XCTAssertEqual(opened, [OpenOrder(mdOrder: "first", sessionId: nil), OpenOrder(mdOrder: "second", sessionId: nil)])
    }

    func test_a_result_after_a_reset_is_ignored() async {
        run.launch(config: existing("given"), profile: v1)
        run.reset()
        run.onResult(CheckoutResultCompleted(orderId: "given"))
        await run.waitUntilSettled()

        XCTAssertEqual(run.state, .idle)
        XCTAssertTrue(StubURLProtocol.requests.isEmpty)
    }

    func test_a_finished_run_can_be_launched_again() async throws {
        run.launch(config: register, profile: v1)
        await run.waitUntilSettled()
        run.onResult(CheckoutResultCanceled(orderId: "abc"))
        _ = try await finished()
        run.launch(config: existing("second"), profile: v1)

        XCTAssertEqual(run.state, .inSheet(OpenOrder(mdOrder: "second", sessionId: nil)))
    }

    func test_the_result_type_ids_are_the_contracts() {
        XCTAssertEqual(
            ResultType.allCases.map(\.rawValue),
            ["completed", "failed", "canceled", "unknown", "registration_failed", "sdk_error"]
        )
    }
}
