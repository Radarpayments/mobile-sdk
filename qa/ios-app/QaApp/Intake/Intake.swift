/// One step that external input asks for, applied in list order.
nonisolated enum Intake: Equatable {
    /// A valid configuration to apply at once.
    case config(QaConfig)

    /// A valid stand profile; with `needsConfirmation` it is saved only after the tester confirms it.
    case profile(StandProfile, needsConfirmation: Bool)

    /// Start a run with the configuration in force after the preceding steps.
    case launch

    /// The input cannot be used; nothing else comes with it, so nothing is applied.
    case rejected(Problem)
}

/// Reads a `payrdrqa://config?data=…[&launch=1]` or `payrdrqa://profile?data=…` link after trimming; the scheme ignores
/// ASCII case, the host does not. A config yields `.config`, followed by `.launch` when `launch` is exactly `1`; a
/// profile yields a `.profile` that needs confirmation. Anything unreadable or invalid yields a single `.rejected`.
///
/// The link is text, not a `URL`: Foundation refuses or rewrites some links Android accepts, so `onOpenURL` passes
/// `url.absoluteString`.
nonisolated func intake(fromLink link: String) -> [Intake] {
    let trimmed = trimInput(link)
    guard let target = linkTarget(trimmed) else { return rejected(IntakeProblems.link) }
    let pairs = queryPairs(trimmed)
    let data = pairs.filter { sameText($0.name, "data") }
    guard data.count == 1, let value = data[0].value, let text = decodeData(value) else {
        return rejected(IntakeProblems.data)
    }
    switch target {
    case .config:
        // Unlike a profile link, the decoded config is not trimmed: Android decodes it as it is.
        let config: QaConfig
        switch QaConfigJson.decode(text) {
        case let .ok(decoded): config = decoded
        case let .invalid(problem): return rejected(problem)
        }
        let launches = pairs.filter { sameText($0.name, "launch") }
        let launch = launches.count == 1 && launches[0].value.map { sameText($0, "1") } == true
        return launch ? [.config(config), .launch] : [.config(config)]
    case .profile:
        return [profileIntake(text, needsConfirmation: true)]
    }
}

/// Reads the launch arguments `-qaProfile <json>`, `-qaConfig <json>` and `-qaLaunch 1` from the process arguments
/// (the first is the executable and is skipped). A flag's value is the next argument, whatever it is; a repeated flag
/// keeps its last value; a flag with nothing after it is ignored. The result applies the profile, unconfirmed, before
/// the config and launches last. When either document cannot be used the result is a single `.rejected` for the first
/// of them, and nothing is applied or launched.
///
/// The arguments are read from the array rather than `UserDefaults`, which would also take them from the app's stored
/// defaults and would turn a value that looks like a property list into something else.
nonisolated func intake(fromArguments arguments: [String]) -> [Intake] {
    var profile: String?
    var config: String?
    var launch: String?
    var index = 1
    while index < arguments.count {
        let flag = arguments[index]
        guard index + 1 < arguments.count else { break }
        let value = arguments[index + 1]
        if sameText(flag, "-qaProfile") {
            profile = value
        } else if sameText(flag, "-qaConfig") {
            config = value
        } else if sameText(flag, "-qaLaunch") {
            launch = value
        } else {
            index += 1
            continue
        }
        index += 2
    }
    var steps: [Intake] = []
    if let profile {
        let step = profileIntake(profile, needsConfirmation: false)
        if case .rejected = step { return [step] }
        steps.append(step)
    }
    if let config {
        let step = configIntake(config)
        if case .rejected = step { return [step] }
        steps.append(step)
    }
    if let launch, sameText(trimInput(launch), "1") { steps.append(.launch) }
    return steps
}

/// Reads pasted or scanned text after trimming: text starting with `payrdrqa://` in any ASCII case as
/// `intake(fromLink:)` does, otherwise a JSON object, which is a profile needing confirmation when it has a `gatewayUrl`
/// key and a config when it has not.
nonisolated func intake(fromText text: String) -> [Intake] {
    let trimmed = trimInput(text)
    if hasLinkPrefix(trimmed) { return intake(fromLink: trimmed) }
    if let message = limitProblem(trimmed) { return rejected(Problem(field: "$", message: message)) }
    guard case let .object(root) = JSONValue.parse(trimmed) else { return rejected(.notAnObject) }
    if root["gatewayUrl"] != nil { return [profileIntake(trimmed, needsConfirmation: true)] }
    return [configIntake(trimmed)]
}

private nonisolated enum IntakeProblems {
    static let link = Problem(field: "link", message: "must be payrdrqa://config or payrdrqa://profile")
    static let data = Problem(field: "data", message: "must be UTF-8 JSON, base64url-encoded in a link")
}

private nonisolated enum LinkTarget {
    case config
    case profile
}

private nonisolated let trimmedScalars: Set<Unicode.Scalar> = [" ", "\t", "\r", "\n"]

private nonisolated let linkPrefix = Array("payrdrqa://".unicodeScalars)

// Padded base64url of the largest document: 87384 characters.
private nonisolated let maxDataLength = (DocumentLimits.maxBytes + 2) / 3 * 4

private nonisolated func rejected(_ problem: Problem) -> [Intake] {
    [.rejected(problem)]
}

/// Drops one leading U+FEFF (a byte order mark that editors and clipboards leave), then ASCII space, tab, CR and LF from
/// both ends. Other whitespace stays, so both platforms trim the same characters; Swift's own trimming would take more.
nonisolated func trimInput(_ text: String) -> String {
    var scalars = Substring(text).unicodeScalars
    if scalars.first == "\u{FEFF}" { scalars.removeFirst() }
    let kept: (Unicode.Scalar) -> Bool = { !trimmedScalars.contains($0) }
    guard let first = scalars.firstIndex(where: kept), let last = scalars.lastIndex(where: kept) else { return "" }
    return stringOf(scalars[first ... last])
}

// Swift's case-insensitive comparison folds non-ASCII letters too, such as the Kelvin sign to 'k'.
private nonisolated func hasLinkPrefix(_ text: String) -> Bool {
    let head = text.unicodeScalars.prefix(linkPrefix.count).map { scalar in
        ("A" ... "Z").contains(scalar) ? Unicode.Scalar(scalar.value + 0x20)! : scalar
    }
    return head == linkPrefix
}

/// Everything between `://` and the query must be the host itself, so a user-info, a port (even an empty one), a path or
/// an escape in the host is not a link.
private nonisolated func linkTarget(_ link: String) -> LinkTarget? {
    let scalars = link.unicodeScalars
    guard !scalars.contains("#"), hasLinkPrefix(link) else { return nil }
    let host = stringOf(scalars.dropFirst(linkPrefix.count).prefix { $0 != "?" })
    if sameText(host, "config") { return .config }
    if sameText(host, "profile") { return .profile }
    return nil
}

/// Every parameter in order; one without `=` has an empty value, one whose name cannot be decoded is dropped, and a value
/// that cannot be decoded is nil.
private nonisolated func queryPairs(_ link: String) -> [(name: String, value: String?)] {
    let scalars = link.unicodeScalars
    guard let mark = scalars.firstIndex(of: "?") else { return [] }
    return scalars[scalars.index(after: mark)...].split(separator: "&").compactMap { pair in
        let equals = pair.firstIndex(of: "=")
        guard let name = percentDecode(pair[..<(equals ?? pair.endIndex)]) else { return nil }
        let raw = equals.map { pair[pair.index(after: $0)...] } ?? pair[pair.endIndex...]
        // An escape is three characters for one, so a longer raw value cannot decode to an acceptable `data`.
        return (name, raw.count > maxDataLength * 3 ? nil : percentDecode(raw))
    }
}

/// `+` stays a `+`: this is a URI query, not a form, and base64url never needs a space. Nil on a malformed escape, a hex
/// digit outside ASCII or bytes that are not UTF-8.
private nonisolated func percentDecode(_ scalars: Substring.UnicodeScalarView) -> String? {
    var bytes: [UInt8] = []
    var index = scalars.startIndex
    while index < scalars.endIndex {
        let scalar = scalars[index]
        index = scalars.index(after: index)
        guard scalar == "%" else {
            bytes.append(contentsOf: String(scalar).utf8)
            continue
        }
        guard index < scalars.endIndex, let high = hexDigit(scalars[index]) else { return nil }
        index = scalars.index(after: index)
        guard index < scalars.endIndex, let low = hexDigit(scalars[index]) else { return nil }
        index = scalars.index(after: index)
        bytes.append(high << 4 | low)
    }
    return strictUtf8(bytes)
}

private nonisolated func hexDigit(_ scalar: Unicode.Scalar) -> UInt8? {
    switch scalar {
    case "0" ... "9": UInt8(scalar.value - 0x30)
    case "a" ... "f": UInt8(scalar.value - 0x61 + 10)
    case "A" ... "F": UInt8(scalar.value - 0x41 + 10)
    default: nil
    }
}

/// `String(decoding:)` replaces malformed bytes with U+FFFD; the decoder here lets the caller reject them instead. Swift's
/// UTF-8 decoder refuses overlong forms and surrogates, as Java's does on Android.
private nonisolated func strictUtf8(_ bytes: [UInt8]) -> String? {
    var view = String.UnicodeScalarView()
    var iterator = bytes.makeIterator()
    var decoder = UTF8()
    while true {
        switch decoder.decode(&iterator) {
        case let .scalarValue(scalar): view.append(scalar)
        case .emptyInput: return String(view)
        case .error: return nil
        }
    }
}

/// The UTF-8 JSON text carried in `data`, or nil when it is empty, longer than base64url of the largest document, not
/// base64url or not UTF-8.
private nonisolated func decodeData(_ value: String) -> String? {
    let scalars = Array(value.unicodeScalars)
    guard !scalars.isEmpty, scalars.count <= maxDataLength, let bytes = base64UrlDecode(scalars) else { return nil }
    return strictUtf8(bytes)
}

/// Base64url with optional padding, all or nothing. The standard alphabet's `+` and `/` are not guessed at, since the
/// contract names base64url only, and bits left over in the last character must be zero, as Kotlin's decoder demands.
private nonisolated func base64UrlDecode(_ scalars: [Unicode.Scalar]) -> [UInt8]? {
    let padding = scalars.reversed().prefix { $0 == "=" }.count
    let body = scalars.dropLast(padding)
    let tail = body.count % 4
    guard tail != 1, padding == 0 || (tail != 0 && padding == 4 - tail) else { return nil }
    var sextets: [UInt8] = []
    sextets.reserveCapacity(body.count)
    for scalar in body {
        guard let sextet = base64UrlValue(scalar) else { return nil }
        sextets.append(sextet)
    }
    var bytes: [UInt8] = []
    bytes.reserveCapacity(body.count / 4 * 3 + 2)
    var start = 0
    while start + 4 <= sextets.count {
        let group = sextets[start ..< start + 4].map { UInt32($0) }
        let bits = group[0] << 18 | group[1] << 12 | group[2] << 6 | group[3]
        bytes += [UInt8(bits >> 16), UInt8(bits >> 8 & 0xFF), UInt8(bits & 0xFF)]
        start += 4
    }
    let rest = Array(sextets[start...])
    if rest.count == 2 {
        guard rest[1] & 0x0F == 0 else { return nil }
        bytes.append(rest[0] << 2 | rest[1] >> 4)
    } else if rest.count == 3 {
        guard rest[2] & 0x03 == 0 else { return nil }
        bytes += [rest[0] << 2 | rest[1] >> 4, rest[1] << 4 | rest[2] >> 2]
    }
    return bytes
}

private nonisolated func base64UrlValue(_ scalar: Unicode.Scalar) -> UInt8? {
    switch scalar {
    case "A" ... "Z": UInt8(scalar.value - 0x41)
    case "a" ... "z": UInt8(scalar.value - 0x61 + 26)
    case "0" ... "9": UInt8(scalar.value - 0x30 + 52)
    case "-": 62
    case "_": 63
    default: nil
    }
}

private nonisolated func configIntake(_ text: String) -> Intake {
    let trimmed = trimInput(text)
    guard !trimmed.isEmpty else { return .rejected(IntakeProblems.data) }
    switch QaConfigJson.decode(trimmed) {
    case let .ok(config): return .config(config)
    case let .invalid(problem): return .rejected(problem)
    }
}

private nonisolated func profileIntake(_ text: String, needsConfirmation: Bool) -> Intake {
    let trimmed = trimInput(text)
    guard !trimmed.isEmpty else { return .rejected(IntakeProblems.data) }
    switch StandProfileJson.decode(trimmed) {
    case let .ok(profile): return .profile(profile, needsConfirmation: needsConfirmation)
    case let .invalid(problem): return .rejected(problem)
    }
}
