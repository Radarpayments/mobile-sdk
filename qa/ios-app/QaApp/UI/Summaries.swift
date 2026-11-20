import Foundation

/// The rows of the home screen; each problem of the configuration is shown on the row whose screen fixes it.
nonisolated enum HomeRow {
    case stand
    case order
    case amount
    case features
    case theme
    case behaviour
    case wallet
    case sslPinning
}

/// The row of [field], a path of the contract such as `order.mdOrder`.
nonisolated func homeRow(_ field: String) -> HomeRow {
    if sameText(field, "profile") || field.hasPrefix("profile.") { return .stand }
    if sameText(field, "order.amount") || sameText(field, "order.currency") { return .amount }
    if sameText(field, "order.features") { return .features }
    if field.hasPrefix("order.") { return .order }
    if sameText(field, "sdk.theme") { return .theme }
    if field.hasPrefix("sdk.applePay.") || field.hasPrefix("sdk.googlePay.") { return .wallet }
    if field.hasPrefix("sdk.sslPinning.") { return .sslPinning }
    return .behaviour
}

/// A problem as the value of a row: the message with the field's last name in front when the message needs a subject,
/// so `order.mdOrder` "is required" reads `mdOrder is required` and "select a stored profile" stays as it is.
nonisolated func rowProblem(_ problem: Problem) -> String {
    let needsSubject = subjectless.contains { problem.message.hasPrefix("\($0) ") }
    guard needsSubject else { return problem.message }
    let name = problem.field.split(separator: ".", omittingEmptySubsequences: false).last.map(String.init) ?? problem.field
    return "\(name) \(problem.message)"
}

/// The fields the Order screen shows for [source]: the ones a run from that source uses.
nonisolated func orderFields(_ source: OrderSource) -> [String] {
    switch source {
    case .register:
        [Tags.orderType, Tags.orderAmount, Tags.orderCurrency, Tags.orderFeatures, Tags.orderClientId, Tags.orderExtra]
    case .mdOrder: [Tags.orderMdOrder]
    case .sessionId: [Tags.orderSessionId]
    }
}

/// `register · payment`, `mdOrder · 01ea8a09-…`, `sessionId · ps_…`; the id is `empty` while there is none.
nonisolated func orderSummary(_ order: OrderConfig) -> String {
    switch order.source {
    case .register: "\(order.source.rawValue) · \(order.type.rawValue)"
    case .mdOrder: "\(order.source.rawValue) · \(order.mdOrder ?? empty)"
    case .sessionId: "\(order.source.rawValue) · \(order.sessionId ?? empty)"
    }
}

/// The amount in major units with its currency, `10.00 EUR`.
nonisolated func amountSummary(_ order: OrderConfig) -> String {
    "\(minorToMajor(order.amount, digits: Currencies.minorDigits(order.currency))) \(order.currency)"
}

nonisolated func featuresSummary(_ order: OrderConfig) -> String {
    order.features.isEmpty ? none : order.features.joined(separator: ", ")
}

/// The extra parameters' names, `none` without any.
nonisolated func extraSummary(_ order: OrderConfig) -> String {
    order.extra.isEmpty ? none : order.extra.keys.joined(separator: ", ")
}

nonisolated func clientIdSummary(_ order: OrderConfig) -> String {
    order.clientId ?? none
}

/// `en · result · scanner · key provider`: the locale or `system locale`, then the switches that are on.
nonisolated func behaviourSummary(_ sdk: SdkOptions) -> String {
    [
        sdk.locale ?? "system locale",
        sdk.showResultScreen ? "result" : nil,
        sdk.scanner ? "scanner" : nil,
        sdk.keyProviderUrl != nil ? "key provider" : nil,
    ].compactMap { $0 }.joined(separator: " · ")
}

/// `off` or `on`: Apple Pay has no environment to name, unlike Google Pay's `on · test`.
nonisolated func applePaySummary(_ sdk: SdkOptions) -> String {
    sdk.applePay.enabled ? "on" : off
}

/// `off`, `1 pin`, `2 pins`.
nonisolated func sslPinningSummary(_ sdk: SdkOptions) -> String {
    let pinning = sdk.sslPinning
    guard pinning.enabled else { return off }
    return pinning.pins.count == 1 ? "1 pin" : "\(pinning.pins.count) pins"
}

/// The second line of a stand: `dev.bpcbt.com · API v1`.
nonisolated func standSummary(_ profile: StandProfile) -> String {
    "\(hostOf(profile.gatewayUrl)) · API \(profile.api.rawValue)"
}

nonisolated func hostOf(_ url: String) -> String {
    URL(string: url)?.host ?? url
}

// Messages of the checks that read as a predicate of the field: "is required", "must be digits …".
private nonisolated let subjectless = ["is", "must", "has", "are", "cannot"]
private nonisolated let none = "none"
private nonisolated let off = "off"
private nonisolated let empty = "empty"
