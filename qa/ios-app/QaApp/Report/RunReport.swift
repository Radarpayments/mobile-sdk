import Foundation

/// What the report says about the app and the device it ran on.
struct AppInfo: Equatable {
    var appVersion: String
    var sdkVersion: String
    var device: String
    var os: String
}

/// The plain text QA attaches to a bug, built from the `finished` run alone: versions, device, the run's profile without
/// its credentials, the configuration it was launched with, the result, the order status and the journal of the run.
/// The format is the contract's "Run report" (qa/README.md); both apps print the same text but for the JSON.
func runReport(app: AppInfo, finished: FinishedRun, entries: [JournalEntry], timeZone: TimeZone = .current) -> String {
    let outcome = finished.outcome
    let profile = finished.profile
    let sections = [
        section("Versions", ["QA app: \(app.appVersion)", "SDK: \(app.sdkVersion)"]),
        section("Device", [app.device, app.os]),
        section("Profile", ["Name: \(profile.name)", "Gateway URL: \(profile.gatewayUrl)", "API: \(profile.api.rawValue)"]),
        section("Configuration", [QaConfigJson.encode(finished.config)]),
        section(
            "Result",
            [
                "qa_result_type: \(outcome.type.rawValue)",
                "mdOrder: \(outcome.mdOrder ?? absent)",
                "Session id: \(outcome.sessionId ?? absent)",
                "Error: \(outcome.error ?? absent)",
                "HTTP status: \(outcome.httpStatus.map(String.init) ?? absent)",
            ]
        ),
        section("Order status", [statusLine(outcome, profile)]),
        section("Journal", [journalTimes(timeZone, entries)] + entries.map { $0.line(timeZone: timeZone) }),
    ]
    return sections.joined(separator: "\n\n") + "\n"
}

// The zone of the first line's time (of now when there is none), so a reader in another zone can line the times up.
private func journalTimes(_ timeZone: TimeZone, _ entries: [JournalEntry]) -> String {
    "Time zone: \(utcOffset(timeZone, at: entries.first?.time ?? Date()))"
}

/// `UTC+03:00`, `UTC-05:30`, `UTC+00:00`: built by hand, so no locale changes the digits or the sign.
func utcOffset(_ timeZone: TimeZone, at date: Date) -> String {
    let offsetMinutes = timeZone.secondsFromGMT(for: date) / 60
    let sign = offsetMinutes < 0 ? "-" : "+"
    let minutes = abs(offsetMinutes)
    return "UTC\(sign)\(pad(minutes / 60)):\(pad(minutes % 60))"
}

private func pad(_ value: Int) -> String {
    value < 10 ? "0\(value)" : "\(value)"
}

private func section(_ title: String, _ lines: [String]) -> String {
    (["== \(title) =="] + lines).joined(separator: "\n")
}

private func statusLine(_ outcome: RunOutcome, _ profile: StandProfile) -> String {
    let text = outcome.status.text
    guard outcome.status == .notQueried else { return text }
    let reason =
        if outcome.type == .registrationFailed || outcome.type == .sdkError {
            "the checkout did not run"
        } else if profile.api == .v2 {
            "API v2 has no status query"
        } else if isBlank(profile.login) || isBlank(profile.password) {
            "the profile has no login and password"
        } else {
            "the checkout named no order"
        }
    return "\(text): \(reason)"
}

private let absent = "-"
