/// The form shows an amount in major units (`10.50`) while the document keeps minor units (`1050`).
nonisolated func minorToMajor(_ minor: Int64, digits: Int) -> String {
    let text = String(minor)
    if digits <= 0 { return text }
    let padded = String(repeating: "0", count: max(0, digits + 1 - text.count)) + text
    let point = padded.index(padded.endIndex, offsetBy: -digits)
    return "\(padded[..<point]).\(padded[point...])"
}

/// Minor units of [text]: ASCII digits with at most one `.` or `,` and at most [digits] after it. Nil for anything else
/// (signs, spaces, other scripts' digits, a missing whole part) and for more than 18 digits, which no Int64 amount needs.
nonisolated func majorToMinor(_ text: String, digits: Int) -> Int64? {
    let scalars = Array(text.unicodeScalars)
    let separator = scalars.firstIndex { $0 == "." || $0 == "," }
    let whole = separator.map { Array(scalars[..<$0]) } ?? scalars
    let fraction = separator.map { Array(scalars[($0 + 1)...]) } ?? []
    guard !whole.isEmpty, whole.allSatisfy(isAsciiDigit), fraction.allSatisfy(isAsciiDigit) else { return nil }
    guard fraction.count <= digits, separator == nil || digits > 0 else { return nil }
    let all = whole + fraction + Array(repeating: "0", count: digits - fraction.count)
    let significant = all.drop { $0 == "0" }
    guard significant.count <= maxMinorDigits else { return nil }
    return significant.isEmpty ? 0 : Int64(String(String.UnicodeScalarView(significant)))
}

private nonisolated func isAsciiDigit(_ scalar: Unicode.Scalar) -> Bool {
    scalar.value >= 0x30 && scalar.value <= 0x39
}

private nonisolated let maxMinorDigits = 18
