import Combine
@testable import QaApp
import RadarpaymentsSDKPayment
import XCTest

private final class TimeoutFailure: KotlinThrowable {}

final class JournalTests: XCTestCase {
    // 2026-11-18 09:05:07.042 UTC
    private let time = Date(timeIntervalSince1970: 1_794_992_707.042)

    private func journal(capacity: Int = 2000) -> Journal {
        let time = time
        return Journal(capacity: capacity, clock: { time })
    }

    private func entry(_ level: LogLevel, _ tag: String, _ message: String) -> JournalEntry {
        JournalEntry(time: time, level: level, tag: tag, message: message)
    }

    func test_an_sdk_event_becomes_an_entry() {
        let journal = journal()
        journal.log(event: LogEvent(level: .info, tag: "PaymentFlow", message: "started", cause: nil))
        XCTAssertEqual(journal.snapshot(), [entry(.info, "PaymentFlow", "started")])
    }

    func test_the_cause_of_an_event_is_appended_to_its_message() {
        let journal = journal()
        let cause = KotlinIllegalStateException(message: "timeout")
        journal.log(event: LogEvent(level: .error, tag: "Merchant", message: "request failed", cause: cause))
        XCTAssertEqual(journal.snapshot().map(\.message), ["request failed (IllegalStateException: timeout)"])
    }

    func test_a_cause_without_a_message_is_named_alone() {
        let journal = journal()
        let cause = KotlinIllegalStateException(message: nil)
        journal.log(event: LogEvent(level: .error, tag: "Merchant", message: "request failed", cause: cause))
        XCTAssertEqual(journal.snapshot().map(\.message), ["request failed (IllegalStateException)"])
    }

    func test_a_cause_of_a_swift_subclass_is_named_by_its_type() {
        let journal = journal()
        journal.log(event: LogEvent(level: .warning, tag: "qa", message: "failed", cause: TimeoutFailure(message: "slow")))
        XCTAssertEqual(journal.snapshot().map(\.message), ["failed (TimeoutFailure: slow)"])
    }

    func test_the_oldest_entries_are_dropped_beyond_the_capacity() {
        let journal = journal()
        for index in 0 ..< 2005 {
            journal.record(.debug, tag: "t", message: "m\(index)")
        }
        let entries = journal.snapshot()
        XCTAssertEqual(entries.count, 2000)
        XCTAssertEqual(entries.first?.message, "m5")
        XCTAssertEqual(entries.last?.message, "m2004")
    }

    func test_a_snapshot_does_not_change_afterwards() {
        let journal = journal(capacity: 2)
        journal.record(.info, tag: "t", message: "a")
        journal.record(.info, tag: "t", message: "b")
        let read = journal.snapshot()
        journal.record(.info, tag: "t", message: "c")
        journal.clear()
        XCTAssertEqual(read.map(\.message), ["a", "b"])
    }

    func test_events_from_many_threads_are_all_kept_in_order_per_thread() {
        let journal = journal()
        DispatchQueue.concurrentPerform(iterations: 8) { thread in
            for index in 0 ..< 200 {
                journal.record(.debug, tag: "t\(thread)", message: "\(index)")
            }
        }
        let entries = journal.snapshot()
        XCTAssertEqual(entries.count, 1600)
        for thread in 0 ..< 8 {
            let messages = entries.filter { $0.tag == "t\(thread)" }.map(\.message)
            XCTAssertEqual(messages, (0 ..< 200).map { "\($0)" })
        }
    }

    func test_clear_empties_the_journal() {
        let journal = journal()
        journal.record(.info, tag: "t", message: "a")
        journal.clear()
        XCTAssertEqual(journal.snapshot(), [])
        journal.record(.info, tag: "t", message: "b")
        XCTAssertEqual(journal.snapshot().map(\.message), ["b"])
    }

    @MainActor
    func test_the_entries_are_published_on_the_main_queue() {
        let journal = journal()
        let published = expectation(description: "published")
        var threads: [Bool] = []
        let subscription = journal.$entries.dropFirst().sink { entries in
            threads.append(Thread.isMainThread)
            if entries.count == 3 { published.fulfill() }
        }
        DispatchQueue.global().async {
            for index in 0 ..< 3 {
                journal.record(.info, tag: "t", message: "\(index)")
            }
        }
        wait(for: [published], timeout: 5)
        subscription.cancel()
        XCTAssertEqual(journal.entries, journal.snapshot())
        XCTAssertTrue(threads.allSatisfy { $0 })
    }

    func test_the_journal_is_filtered_by_level_and_by_tag_or_message() {
        let entries = [
            entry(.debug, "merchant", "→ POST register.do"),
            entry(.info, "Merchant", "← 200"),
            entry(.warning, "PaymentFlow", "3DS challenge from MERCHANT"),
            entry(.error, "PaymentFlow", "declined"),
        ]
        XCTAssertEqual(filtered(entries, minLevel: .info, query: "merchant"), [entries[1], entries[2]])
        XCTAssertEqual(filtered(entries, minLevel: .debug, query: ""), entries)
        XCTAssertEqual(filtered(entries, minLevel: .error, query: ""), [entries[3]])
        XCTAssertEqual(filtered(entries, minLevel: .debug, query: "nothing"), [])
    }

    func test_the_query_ignores_case_the_way_android_does() {
        let entries = [entry(.info, "t", "ſtatus"), entry(.info, "t", "straße")]
        XCTAssertEqual(filtered(entries, minLevel: .debug, query: "STATUS"), [entries[0]])
        XCTAssertEqual(filtered(entries, minLevel: .debug, query: "STRASSE"), [])
    }

    func test_a_line_is_the_time_in_the_zone_then_the_level_tag_and_message() {
        let entry = entry(.warning, "PaymentFlow", "3DS challenge")
        XCTAssertEqual(entry.line(timeZone: TimeZone(secondsFromGMT: 0)!), "09:05:07.042 WARNING PaymentFlow: 3DS challenge")
        XCTAssertEqual(entry.line(timeZone: TimeZone(secondsFromGMT: 19800)!), "14:35:07.042 WARNING PaymentFlow: 3DS challenge")
        XCTAssertEqual(entry.line(timeZone: TimeZone(secondsFromGMT: -36000)!), "23:05:07.042 WARNING PaymentFlow: 3DS challenge")
    }

    func test_a_line_names_every_level_and_keeps_line_breaks() {
        let utc = TimeZone(secondsFromGMT: 0)!
        XCTAssertEqual(entry(.debug, "t", "a\nb").line(timeZone: utc), "09:05:07.042 DEBUG t: a\nb")
        XCTAssertEqual(entry(.info, "t", "m").line(timeZone: utc), "09:05:07.042 INFO t: m")
        XCTAssertEqual(entry(.error, "t", "m").line(timeZone: utc), "09:05:07.042 ERROR t: m")
    }
}
