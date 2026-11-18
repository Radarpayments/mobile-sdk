// The form edits lists as text: names comma-separated, pins one `host=pin` per line, `extra` as a JSON object.

nonisolated func namesToText(_ names: [String]) -> String {
    names.joined(separator: ",")
}

nonisolated func namesFromText(_ text: String) -> [String] {
    text.unicodeScalars.split(separator: ",", omittingEmptySubsequences: false)
        .map { trimmed(stringOf($0)) }
        .filter { !$0.isEmpty }
}

nonisolated func pinsToText(_ pins: [Pin]) -> String {
    pins.map { "\($0.host)=\($0.pin)" }.joined(separator: "\n")
}

// A pin ends in '=', so a line is split at the first one: a host has none.
nonisolated func pinsFromText(_ text: String) -> Decoded<[Pin]> {
    let pins = lines(text).map(trimmed).filter { !$0.isEmpty }.map { line in
        let scalars = line.unicodeScalars
        guard let equals = scalars.firstIndex(of: "=") else { return Pin(host: line, pin: "") }
        return Pin(host: stringOf(scalars[..<equals]), pin: stringOf(scalars[scalars.index(after: equals)...]))
    }
    return pinsProblem(pins).map { .invalid($0) } ?? .ok(pins)
}

nonisolated func extraToText(_ extra: JSONObject) -> String {
    JSONValue.object(extra).text
}

// `extra` is level 3 of a configuration (the root, `order`, `extra`): two levels fewer keep the document within its limit.
nonisolated func extraFromText(_ text: String) -> Decoded<JSONObject> {
    if isBlank(text) { return .ok([:]) }
    if let message = limitProblem(text, maxDepth: DocumentLimits.maxDepth - 2) {
        return .invalid(Problem(field: QaConfigJson.extra, message: message))
    }
    guard case let .object(extra) = JSONValue.parse(text) else {
        return .invalid(Problem(field: QaConfigJson.extra, message: "must be a JSON object"))
    }
    return extraProblem(extra).map { .invalid($0) } ?? .ok(extra)
}

/// The lines of [text] split at "\r\n", "\n" or "\r", as Kotlin's `lines()` splits them.
nonisolated private func lines(_ text: String) -> [String] {
    var result: [String] = []
    var line = String.UnicodeScalarView()
    var afterReturn = false
    for scalar in text.unicodeScalars {
        if afterReturn, scalar == "\n" {
            afterReturn = false
            continue
        }
        afterReturn = scalar == "\r"
        if scalar == "\n" || scalar == "\r" {
            result.append(String(line))
            line = String.UnicodeScalarView()
        } else {
            line.append(scalar)
        }
    }
    result.append(String(line))
    return result
}
