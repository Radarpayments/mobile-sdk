import Foundation
import RadarpaymentsSDKPayment

/// What the journal screen shows for one or two entries: a gateway exchange or a single line.
nonisolated enum JournalItem: Equatable {
    /// A request of `merchant` and the answer to it, if one came.
    case exchange(JournalExchange)
    /// An entry of the SDK, of the app's run, or a gateway answer whose request is gone from the journal.
    case line(JournalEntry)

    var entries: [JournalEntry] {
        switch self {
        case let .exchange(exchange): [exchange.request] + (exchange.response.map { [$0] } ?? [])
        case let .line(entry): [entry]
        }
    }
}

nonisolated struct JournalExchange: Equatable {
    let request: JournalEntry
    let response: JournalEntry?

    private var requestParts: [Substring] {
        dropPrefix(request.message, requestMark).split(separator: " ", maxSplits: 2, omittingEmptySubsequences: false)
    }

    var method: String {
        requestParts.first.map(String.init) ?? ""
    }

    var url: String {
        requestParts.count > 1 ? String(requestParts[1]) : ""
    }

    /// The last segment of the URL's path, `register.do`; the whole URL when it has none.
    var path: String {
        var path = Substring(url)
        if let query = path.firstIndex(of: "?") { path = path[..<query] }
        while path.hasSuffix("/") { path = path.dropLast() }
        if let slash = path.lastIndex(of: "/") { path = path[path.index(after: slash)...] }
        return path.isEmpty ? url : String(path)
    }

    var host: String {
        hostOf(url)
    }

    /// The request as journalled after its URL: the masked headers and the body.
    var requestDetail: String {
        requestParts.count > 2 ? String(requestParts[2]) : ""
    }

    /// The HTTP status, or nil when the answer is not in yet or never came.
    var status: Int? {
        response.flatMap { statusOf($0.message) }
    }

    /// No answer at all: a timeout or a network error, journalled as `← <message>`.
    var unanswered: Bool {
        response != nil && status == nil
    }

    var failed: Bool {
        unanswered || status.map { !(200 ..< 300).contains($0) } == true
    }

    /// The answer as journalled after its status, or the reason it never came.
    var responseDetail: String? {
        guard let response else { return nil }
        let text = dropPrefix(response.message, responseMark)
        guard status != nil else { return text }
        return String(text.dropFirst(statusLength).drop { $0 == " " })
    }

    /// `200 · 4.3 s`, `no answer · 20 s`, or `…` while the answer is awaited.
    var outcome: String {
        guard let response else { return "…" }
        let millis = Int64(((response.time.timeIntervalSince1970 - request.time.timeIntervalSince1970) * 1000).rounded())
        return "\(status.map(String.init) ?? "no answer") · \(durationText(millis))"
    }
}

extension JournalEntry {
    /// The short source shown in a line's second column.
    nonisolated var source: String {
        if sameText(tag, Merchant.tag) { return "GW" }
        if sameText(tag, QaRun.tag) { return "QA" }
        return "SDK"
    }
}

/// Pairs each request of `merchant` with the next answer of `merchant`, which is the answer to it: the app sends one
/// gateway request at a time and journals the answer, or its absence, before the next one. Any other entry is a line of
/// its own, and the entries stay in the order they were journalled. The journal itself is not changed, so the report
/// and the copied text keep one line per entry.
nonisolated func journalItems(_ entries: [JournalEntry]) -> [JournalItem] {
    var items: [JournalItem] = []
    var open: Int?
    for entry in entries {
        let gateway = sameText(entry.tag, Merchant.tag)
        if gateway && entry.message.unicodeScalars.starts(with: requestMark.unicodeScalars) {
            items.append(.exchange(JournalExchange(request: entry, response: nil)))
            open = items.count - 1
        } else if gateway, entry.message.unicodeScalars.starts(with: responseMark.unicodeScalars),
                  let index = open, case let .exchange(exchange) = items[index] {
            items[index] = .exchange(JournalExchange(request: exchange.request, response: entry))
            open = nil
        } else {
            items.append(.line(entry))
        }
    }
    return items
}

/// The journal screen's segments. `rawValue` is the text of `qa_journal_level` and the end of each segment's identifier.
nonisolated enum JournalFilter: String, CaseIterable {
    case all
    case gateway
    case sdk
    case errors

    var label: String {
        switch self {
        case .all: "All"
        case .gateway: "Gateway"
        case .sdk: "SDK"
        case .errors: "Errors"
        }
    }

    func accepts(_ item: JournalItem) -> Bool {
        switch self {
        case .all: return true
        case .gateway: return item.entries.allSatisfy { sameText($0.tag, Merchant.tag) }
        case .sdk: return !item.entries.contains { sameText($0.tag, Merchant.tag) }
        case .errors:
            if item.entries.contains(where: { levelRank($0.level) >= levelRank(.warning) }) { return true }
            if case let .exchange(exchange) = item { return exchange.failed }
            return false
        }
    }
}

/// The items [filter] keeps whose tag or message of some entry contains [query], ignoring case.
nonisolated func shown(_ items: [JournalItem], filter: JournalFilter, query: String) -> [JournalItem] {
    let search = FoldedText(query)
    return items.filter { item in filter.accepts(item) && item.entries.contains { $0.contains(search) } }
}

/// Under ten seconds with one decimal (`4.3 s`), whole seconds from there (`20 s`); digits are ASCII in any locale. The
/// choice follows the rounded value, so 9 950 ms reads `10 s`, not `10.0 s`.
nonisolated func durationText(_ millis: Int64) -> String {
    let safe = max(0, millis)
    let tenths = (safe + 50) / 100
    if tenths >= 100 { return "\((safe + 500) / 1000) s" }
    return "\(tenths / 10).\(tenths % 10) s"
}

private nonisolated func statusOf(_ message: String) -> Int? {
    let text = Array(dropPrefix(message, responseMark).unicodeScalars)
    guard text.count >= statusLength, text.count == statusLength || text[statusLength] == " " else { return nil }
    let code = text[..<statusLength]
    guard code.allSatisfy({ $0.value >= 0x30 && $0.value <= 0x39 }) else { return nil }
    return Int(String(String.UnicodeScalarView(code)))
}

private nonisolated func dropPrefix(_ text: String, _ prefix: String) -> String {
    let scalars = text.unicodeScalars
    guard scalars.starts(with: prefix.unicodeScalars) else { return text }
    return String(String.UnicodeScalarView(scalars.dropFirst(prefix.unicodeScalars.count)))
}

private nonisolated let requestMark = "→ "
private nonisolated let responseMark = "← "
private nonisolated let statusLength = 3
