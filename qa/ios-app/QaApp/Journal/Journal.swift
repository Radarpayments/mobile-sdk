import Combine
import Foundation
import RadarpaymentsSDKPayment

nonisolated struct JournalEntry: Equatable {
    let time: Date
    let level: LogLevel
    let tag: String
    let message: String

    /// `HH:mm:ss.SSS LEVEL tag: message` in [timeZone], the device's zone by default. Formatted by hand: a date formatter
    /// would print the digits of the user's locale, and the report must read the same on every device.
    func line(timeZone: TimeZone = .current) -> String {
        // Rounded, not truncated: a date made from whole milliseconds is a binary fraction that can fall a hair short.
        let millis = Int64((time.timeIntervalSince1970 * 1000).rounded()) + Int64(timeZone.secondsFromGMT(for: time)) * 1000
        let ofDay = (millis % Self.millisPerDay + Self.millisPerDay) % Self.millisPerDay
        let hours = ofDay / 3_600_000
        let minutes = ofDay / 60000 % 60
        let seconds = ofDay / 1000 % 60
        let rest = ofDay % 1000
        let time = "\(pad(hours, 2)):\(pad(minutes, 2)):\(pad(seconds, 2)).\(pad(rest, 3))"
        return "\(time) \(levelName(level)) \(tag): \(message)"
    }

    private static let millisPerDay: Int64 = 86_400_000

    private func pad(_ value: Int64, _ width: Int) -> String {
        let digits = String(value)
        return String(repeating: "0", count: max(0, width - digits.count)) + digits
    }
}

/// The SDK's log of one run, kept in memory as it was logged: nothing is filtered or redacted here. Holds the last
/// [capacity] entries, oldest first.
///
/// The SDK logs from any thread, so the buffer is behind a lock and `snapshot()` reads it from anywhere; `entries`
/// follows it on the main queue for SwiftUI.
nonisolated final class Journal: NSObject, ObservableObject, LogTransport, @unchecked Sendable {
    /// Each value is a copy that never changes afterwards. Published on the main queue, shortly after the change.
    @MainActor @Published private(set) var entries: [JournalEntry] = []

    private let capacity: Int
    private let clock: () -> Date
    private let lock = NSLock()
    private var buffer: [JournalEntry] = []
    private var publishScheduled = false

    init(capacity: Int = 2000, clock: @escaping () -> Date = Date.init) {
        precondition(capacity > 0, "capacity must be positive, was \(capacity)")
        self.capacity = capacity
        self.clock = clock
    }

    /// The entries as they are now, from any thread.
    func snapshot() -> [JournalEntry] {
        lock.lock()
        defer { lock.unlock() }
        return buffer
    }

    func log(event: LogEvent) {
        let message = event.cause.map { event.message + suffix($0) } ?? event.message
        record(event.level, tag: event.tag, message: message)
    }

    func record(_ level: LogLevel, tag: String, message: String) {
        lock.lock()
        defer { lock.unlock() }
        if buffer.count == capacity { buffer.removeFirst() }
        buffer.append(JournalEntry(time: clock(), level: level, tag: tag, message: message))
        schedulePublish()
    }

    func clear() {
        lock.lock()
        defer { lock.unlock() }
        buffer.removeAll()
        schedulePublish()
    }

    // One pending publish at a time, and it reads the buffer when it runs: a burst of events costs one copy, and an
    // older copy can never land after a newer one.
    private func schedulePublish() {
        guard !publishScheduled else { return }
        publishScheduled = true
        DispatchQueue.main.async { [self] in
            lock.lock()
            publishScheduled = false
            let copy = buffer
            lock.unlock()
            entries = copy
        }
    }

    private func suffix(_ cause: KotlinThrowable) -> String {
        let name = typeName(of: cause)
        return cause.message.map { " (\(name): \($0))" } ?? " (\(name))"
    }
}

/// The entries at [minLevel] or above whose tag or message contains [query], ignoring case.
nonisolated func filtered(_ entries: [JournalEntry], minLevel: LogLevel, query: String) -> [JournalEntry] {
    entries.filter { entry in
        levelRank(entry.level) >= levelRank(minLevel)
            && (containsIgnoringCase(entry.tag, query) || containsIgnoringCase(entry.message, query))
    }
}

nonisolated func levelName(_ level: LogLevel) -> String {
    switch level {
    case .debug: "DEBUG"
    case .info: "INFO"
    case .warning: "WARNING"
    case .error: "ERROR"
    }
}

nonisolated private func levelRank(_ level: LogLevel) -> Int {
    switch level {
    case .debug: 0
    case .info: 1
    case .warning: 2
    case .error: 3
    }
}
