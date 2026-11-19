import Combine
import Foundation
import RadarpaymentsSDKPayment

nonisolated struct JournalEntry: Equatable {
    let time: Date
    let level: LogLevel
    let tag: String
    let message: String
    // Folded once here rather than on every keystroke of the journal search.
    fileprivate let foldedTag: FoldedText
    fileprivate let foldedMessage: FoldedText

    init(time: Date, level: LogLevel, tag: String, message: String) {
        self.time = time
        self.level = level
        self.tag = tag
        self.message = message
        foldedTag = FoldedText(tag)
        foldedMessage = FoldedText(message)
    }

    /// `HH:mm:ss.SSS LEVEL tag: message` in [timeZone], the device's zone by default. Formatted by hand: a date formatter
    /// would print the digits of the user's locale, and the report must read the same on every device.
    func line(timeZone: TimeZone = .current) -> String {
        "\(timeOfDay(time, timeZone: timeZone, precision: .millis)) \(levelName(level)) \(tag): \(message)"
    }

    /// Whether the tag or the message contains [search], ignoring case.
    func contains(_ search: FoldedText) -> Bool {
        foldedTag.contains(search) || foldedMessage.contains(search)
    }
}

nonisolated enum ClockPrecision {
    case minutes
    case seconds
    case millis
}

/// `10:50`, `10:50:30` or `10:50:30.123` of [time] in [timeZone], with ASCII digits whatever the locale.
nonisolated func timeOfDay(_ time: Date, timeZone: TimeZone = .current, precision: ClockPrecision) -> String {
    // The journal keeps whole milliseconds; rounded, because a date made of them can fall a hair short.
    let millis = Int64((time.timeIntervalSince1970 * 1000).rounded()) + Int64(timeZone.secondsFromGMT(for: time)) * 1000
    let ofDay = (millis % millisPerDay + millisPerDay) % millisPerDay
    let minutes = "\(pad(ofDay / 3_600_000, 2)):\(pad(ofDay / 60000 % 60, 2))"
    switch precision {
    case .minutes: return minutes
    case .seconds: return "\(minutes):\(pad(ofDay / 1000 % 60, 2))"
    case .millis: return "\(minutes):\(pad(ofDay / 1000 % 60, 2)).\(pad(ofDay % 1000, 3))"
    }
}

private nonisolated let millisPerDay: Int64 = 86_400_000

private nonisolated func pad(_ value: Int64, _ width: Int) -> String {
    let digits = String(value)
    return String(repeating: "0", count: max(0, width - digits.count)) + digits
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
        buffer.append(JournalEntry(time: wholeMilliseconds(clock()), level: level, tag: tag, message: message))
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

    // Android's clock counts whole milliseconds, cut rather than rounded. The microsecond of slack keeps a date that is
    // already whole milliseconds, which a binary fraction can leave a hair short of them, from losing one.
    private func wholeMilliseconds(_ date: Date) -> Date {
        let millis = (date.timeIntervalSince1970 * 1000 + 0.001).rounded(.down)
        return Date(timeIntervalSince1970: millis / 1000)
    }

    private func suffix(_ cause: KotlinThrowable) -> String {
        let name = causeName(of: cause)
        return cause.message.map { " (\(name): \($0))" } ?? " (\(name))"
    }
}

/// The entries at [minLevel] or above whose tag or message contains [query], ignoring case.
nonisolated func filtered(_ entries: [JournalEntry], minLevel: LogLevel, query: String) -> [JournalEntry] {
    let search = FoldedText(query)
    let rank = levelRank(minLevel)
    return entries.filter { entry in
        levelRank(entry.level) >= rank && entry.contains(search)
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

nonisolated func levelRank(_ level: LogLevel) -> Int {
    switch level {
    case .debug: 0
    case .info: 1
    case .warning: 2
    case .error: 3
    }
}
