/// The password and the API key of a profile, and every spelling of them a gateway may echo back. Whatever text of an
/// exchange reaches the journal or a failure message goes through here.
///
/// Each character of a secret is matched in any of its spellings independently (raw, `+` for a space, percent-encoded,
/// JSON-escaped, an HTML entity), as on Android, so a mix, such as a form encoder that leaves some characters raw next
/// to a JSON writer that escapes others, is masked as well.
nonisolated struct GatewaySecrets: CustomStringConvertible, CustomDebugStringConvertible, CustomReflectable {
    static let mask = "***"

    private let secrets: [[Unicode.Scalar]]

    init(profile: StandProfile) {
        secrets = [profile.password, profile.apiKey].filter { !isBlank($0) }.map { Array($0.unicodeScalars) }
    }

    var description: String {
        "GatewaySecrets(\(secrets.count) secrets)"
    }

    var debugDescription: String {
        description
    }

    var customMirror: Mirror {
        Mirror(self, children: [], displayStyle: .struct)
    }

    func mask(_ text: String) -> String {
        masked(text, limit: nil).text
    }

    /// Masks first and cuts afterwards: a cut through the middle of a secret would leave its first half, which no longer
    /// matches anything to mask. The limit counts UTF-16 units, as Android's `take` does, but never splits a surrogate
    /// pair: where Android would keep the high half, the character is left out.
    func bounded(_ text: String, limit: Int, cutMarker: String = "") -> String {
        let result = masked(text, limit: limit)
        return result.cut ? result.text + cutMarker : result.text
    }

    private func masked(_ text: String, limit: Int?) -> (text: String, cut: Bool) {
        let input = Array(text.unicodeScalars)
        var output = String.UnicodeScalarView()
        var units = 0
        var position = 0
        func append(_ scalar: Unicode.Scalar) -> Bool {
            let width = UTF16.width(scalar)
            if let limit, units + width > limit { return false }
            units += width
            output.append(scalar)
            return true
        }
        while position < input.count {
            if let end = longestMatch(in: input, at: position) {
                for scalar in Self.mask.unicodeScalars where !append(scalar) {
                    return (String(output), true)
                }
                position = end
            } else {
                guard append(input[position]) else { return (String(output), true) }
                position += 1
            }
        }
        return (String(output), false)
    }

    private func longestMatch(in input: [Unicode.Scalar], at start: Int) -> Int? {
        secrets.compactMap { secret -> Int? in
            var ends: Set<Int> = [start]
            for scalar in secret {
                ends = Set(ends.flatMap { spellingEnds(of: scalar, in: input, at: $0) })
                if ends.isEmpty { return nil }
            }
            return ends.max()
        }.max()
    }
}

// Where one character of a secret ends when it starts at [start], in each spelling that fits there.
nonisolated private func spellingEnds(of scalar: Unicode.Scalar, in input: [Unicode.Scalar], at start: Int) -> [Int] {
    guard start < input.count else { return [] }
    var ends: [Int] = []
    if input[start] == scalar { ends.append(start + 1) }
    switch input[start] {
    case "+":
        if scalar == " " { ends.append(start + 1) }
    case "%":
        if let end = percentEnd(of: scalar, in: input, at: start) { ends.append(end) }
    case "\\":
        if let end = jsonEscapeEnd(of: scalar, in: input, at: start) { ends.append(end) }
    case "&":
        if let end = htmlEntityEnd(of: scalar, in: input, at: start) { ends.append(end) }
    default:
        break
    }
    return ends
}

// %HH for each UTF-8 byte, either case of hex digit.
nonisolated private func percentEnd(of scalar: Unicode.Scalar, in input: [Unicode.Scalar], at start: Int) -> Int? {
    var position = start
    for byte in UTF8.encode(scalar)! {
        guard position + 2 < input.count, input[position] == "%",
              let value = hexValue(input[position + 1 ... position + 2]), value == UInt32(byte)
        else { return nil }
        position += 3
    }
    return position
}

// \uHHHH (a surrogate pair beyond the basic plane), or the short escapes JSON writers use.
nonisolated private func jsonEscapeEnd(of scalar: Unicode.Scalar, in input: [Unicode.Scalar], at start: Int) -> Int? {
    guard start + 1 < input.count else { return nil }
    let short: Unicode.Scalar? =
        switch scalar {
        case "\"": "\""
        case "\\": "\\"
        case "/": "/"
        case "\u{08}": "b"
        case "\u{0C}": "f"
        case "\n": "n"
        case "\r": "r"
        case "\t": "t"
        default: nil
        }
    if let short, input[start + 1] == short { return start + 2 }
    var position = start
    for unit in UTF16.encode(scalar)! {
        guard position + 5 < input.count, input[position] == "\\", input[position + 1] == "u",
              let value = hexValue(input[position + 2 ... position + 5]), value == UInt32(unit)
        else { return nil }
        position += 6
    }
    return position
}

// The entities an HTML page escapes with, and a numeric reference to any character.
nonisolated private func htmlEntityEnd(of scalar: Unicode.Scalar, in input: [Unicode.Scalar], at start: Int) -> Int? {
    let named: [Unicode.Scalar: String] = ["&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&apos;"]
    if let entity = named[scalar], matches(entity, in: input, at: start) { return start + entity.unicodeScalars.count }
    guard start + 3 < input.count, input[start + 1] == "#" else { return nil }
    let hex = input[start + 2] == "x" || input[start + 2] == "X"
    let digitsStart = hex ? start + 3 : start + 2
    guard let semicolon = input[digitsStart...].prefix(9).firstIndex(of: ";"), semicolon > digitsStart else { return nil }
    let digits = input[digitsStart ..< semicolon]
    let value = hex ? hexValue(digits) : decimalValue(digits)
    return value == scalar.value ? semicolon + 1 : nil
}

nonisolated private func matches(_ text: String, in input: [Unicode.Scalar], at start: Int) -> Bool {
    let scalars = Array(text.unicodeScalars)
    return start + scalars.count <= input.count && input[start ..< start + scalars.count].elementsEqual(scalars)
}

nonisolated private func hexValue(_ digits: ArraySlice<Unicode.Scalar>) -> UInt32? {
    digits.reduce(UInt32?(0)) { value, digit in
        guard let value, value <= 0x10FFFF else { return nil }
        let digitValue: UInt32
        switch digit {
        case "0" ... "9": digitValue = digit.value - 0x30
        case "a" ... "f": digitValue = digit.value - 0x61 + 10
        case "A" ... "F": digitValue = digit.value - 0x41 + 10
        default: return nil
        }
        return value * 16 + digitValue
    }
}

nonisolated private func decimalValue(_ digits: ArraySlice<Unicode.Scalar>) -> UInt32? {
    digits.reduce(UInt32?(0)) { value, digit in
        guard let value, value <= 0x10FFFF, ("0" ... "9").contains(digit) else { return nil }
        return value * 10 + digit.value - 0x30
    }
}
