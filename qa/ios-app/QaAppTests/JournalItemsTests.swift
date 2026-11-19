@testable import QaApp
import RadarpaymentsSDKPayment
import XCTest

final class JournalItemsTests: XCTestCase {
    private lazy var request = gateway(1000, "→ POST https://dev.bpcbt.com/payment/rest/register.do body")
    private lazy var answer = gateway(5300, "← 200 {\"orderId\":\"1\"}")
    private let sdk = JournalEntry(time: Date(timeIntervalSince1970: 2), level: .info, tag: "Payment", message: "sheet shown")

    func test_a_request_pairs_with_the_next_answer_of_the_gateway() throws {
        let items = journalItems([request, sdk, answer])

        XCTAssertEqual(items.count, 2)
        guard case let .exchange(exchange) = items[0] else { return XCTFail("\(items[0])") }
        XCTAssertEqual(exchange.response, answer)
        XCTAssertEqual(exchange.method, "POST")
        XCTAssertEqual(exchange.path, "register.do")
        XCTAssertEqual(exchange.host, "dev.bpcbt.com")
        XCTAssertEqual(exchange.requestDetail, "body")
        XCTAssertEqual(exchange.status, 200)
        XCTAssertEqual(exchange.responseDetail, "{\"orderId\":\"1\"}")
        XCTAssertEqual(exchange.outcome, "200 · 4.3 s")
        XCTAssertFalse(exchange.failed)
        guard case let .line(line) = items[1] else { return XCTFail("\(items[1])") }
        XCTAssertEqual(line.source, "SDK")
    }

    func test_the_result_counts_the_items_the_journal_screen_shows() {
        let second = gateway(6000, "→ POST https://dev.bpcbt.com/payment/rest/getOrderStatusExtended.do body")
        let secondAnswer = gateway(6400, "← 200 {}")

        XCTAssertEqual(entriesSummary([request, answer, second, secondAnswer]), "2 entries")
        XCTAssertEqual(entriesSummary([request, answer]), "1 entry")
        XCTAssertEqual(entriesSummary([request, sdk, answer]), "2 entries")
    }

    func test_an_answer_without_a_request_is_a_line_of_its_own() {
        let items = journalItems([answer, request])

        guard case let .line(line) = items[0], case let .exchange(exchange) = items[1] else { return XCTFail("\(items)") }
        XCTAssertEqual(line.source, "GW")
        XCTAssertEqual(exchange.outcome, "…")
    }

    func test_a_request_that_got_no_answer_is_failed() {
        let timeout = JournalEntry(time: Date(timeIntervalSince1970: 21), level: .warning, tag: "merchant", message: "← timeout")
        guard case let .exchange(exchange) = journalItems([request, timeout]).first else { return XCTFail() }

        XCTAssertTrue(exchange.unanswered)
        XCTAssertTrue(exchange.failed)
        XCTAssertEqual(exchange.outcome, "no answer · 20 s")
        XCTAssertEqual(exchange.responseDetail, "timeout")
    }

    func test_two_requests_each_take_their_own_answer() {
        let second = gateway(6000, "→ POST https://dev.bpcbt.com/payment/rest/getOrderStatusExtended.do body")
        let secondAnswer = gateway(6500, "← 500 oops")
        let exchanges = journalItems([request, answer, second, secondAnswer]).compactMap { item -> JournalExchange? in
            if case let .exchange(exchange) = item { return exchange }
            return nil
        }

        XCTAssertEqual(exchanges.map(\.status), [200, 500])
        XCTAssertTrue(exchanges[1].failed)
    }

    func test_pairing_keeps_every_entry_in_its_order() {
        XCTAssertEqual(journalItems([request, sdk, answer]).flatMap(\.entries), [request, answer, sdk])
    }

    func test_the_filters_split_gateway_sdk_and_errors() {
        let warning = JournalEntry(time: Date(timeIntervalSince1970: 3), level: .warning, tag: "Payment", message: "slow")
        let items = journalItems([request, answer, sdk, warning])

        XCTAssertEqual(shown(items, filter: .all, query: "").count, 3)
        XCTAssertEqual(shown(items, filter: .gateway, query: "").map(\.entries), [[request, answer]])
        XCTAssertEqual(shown(items, filter: .sdk, query: "").count, 2)
        XCTAssertEqual(shown(items, filter: .errors, query: "").map(\.entries), [[warning]])
        XCTAssertEqual(shown(items, filter: .all, query: "REGISTER").map(\.entries), [[request, answer]])
        XCTAssertEqual(shown(items, filter: .all, query: "payment").count, 3)
    }

    func test_the_qa_run_is_its_own_source() {
        let items = journalItems([JournalEntry(time: Date(timeIntervalSince1970: 0), level: .info, tag: "qa", message: "launch")])

        guard case let .line(line) = items.first else { return XCTFail() }
        XCTAssertEqual(line.source, "QA")
    }

    func test_a_filters_id_is_the_text_of_the_level_control() {
        XCTAssertEqual(JournalFilter.allCases.map(\.rawValue), ["all", "gateway", "sdk", "errors"])
        XCTAssertEqual(Tags.journalFilter(JournalFilter.errors.rawValue), "qa_journal_filter_errors")
    }

    private func gateway(_ millis: Int, _ message: String) -> JournalEntry {
        JournalEntry(time: Date(timeIntervalSince1970: Double(millis) / 1000), level: .info, tag: "merchant", message: message)
    }
}
