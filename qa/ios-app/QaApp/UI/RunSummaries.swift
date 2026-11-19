import Foundation

/// What a run of [type] came to, as a word for people; `qa_result_type` keeps the contract's id.
nonisolated func outcomeTitle(_ type: ResultType) -> String {
    switch type {
    case .completed: "Completed"
    case .failed: "Failed"
    case .canceled: "Canceled"
    case .unknown: "Unknown"
    case .registrationFailed: "Registration failed"
    case .sdkError: "SDK error"
    }
}

/// How an outcome is coloured: the only colour of the app besides problems.
nonisolated enum Tone {
    case ok
    case bad
    case neutral
}

nonisolated func outcomeTone(_ type: ResultType) -> Tone {
    switch type {
    case .completed: .ok
    case .failed, .registrationFailed, .sdkError: .bad
    case .canceled, .unknown: .neutral
    }
}

/// The order's status in short: `status 2 · action 0`, `status error`, `status not queried`.
nonisolated func statusSummary(_ status: OrderStatus) -> String {
    switch status {
    case let .known(orderStatus, actionCode, _):
        "status \(orderStatus.map(String.init) ?? missing) · action \(actionCode.map(String.init) ?? missing)"
    case .failed: "status error"
    case .notQueried: "status not queried"
    }
}

/// The result banner's detail: `orderStatus 2 · actionCode 0` and the description, `status error: <message>`,
/// `status not queried`.
nonisolated func statusDetail(_ status: OrderStatus) -> [String] {
    switch status {
    case let .known(orderStatus, actionCode, description):
        [
            "orderStatus \(orderStatus.map(String.init) ?? missing) · actionCode \(actionCode.map(String.init) ?? missing)",
            description,
        ].compactMap { $0 }
    case let .failed(_, message): ["status error: \(message)"]
    case .notQueried: ["status not queried"]
    }
}

/// The second line of the home screen's last run: `10:50 · status 2 · action 0`.
nonisolated func lastRunSummary(_ time: Date, _ status: OrderStatus, timeZone: TimeZone = .current) -> String {
    "\(timeOfDay(time, timeZone: timeZone, precision: .minutes)) · \(statusSummary(status))"
}

/// `payment · 10.00 EUR` for a registered order; the source and its id otherwise.
nonisolated func launchedOrderSummary(_ order: OrderConfig) -> String {
    order.source == .register ? "\(order.type.rawValue) · \(amountSummary(order))" : orderSummary(order)
}

/// `2 entries`: the items the journal screen lists, a gateway request and its answer counting once, so the number
/// matches what the Journal row opens.
nonisolated func entriesSummary(_ entries: [JournalEntry]) -> String {
    let count = journalItems(entries).count
    return count == 1 ? "1 entry" : "\(count) entries"
}

// A status field the gateway left out.
private nonisolated let missing = "-"
