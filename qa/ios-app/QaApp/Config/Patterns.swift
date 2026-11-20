// The contract's patterns, matched by hand over scalars rather than with NSRegularExpression: its `$` also matches
// before a final "\n", its `\s` is not the White_Space set, and its case-insensitive option folds U+017F into "s".

/// `^https?://[^\s/?#]+\S*$` with the scheme in any ASCII case and `\s` being Unicode's White_Space.
nonisolated func isHttpUrl(_ text: String) -> Bool {
    let scalars = Array(text.unicodeScalars)
    var index = 0
    for letter in "http".unicodeScalars {
        guard index < scalars.count, asciiLowercase(scalars[index]) == letter else { return false }
        index += 1
    }
    if index < scalars.count, asciiLowercase(scalars[index]) == "s" { index += 1 }
    for letter in "://".unicodeScalars {
        guard index < scalars.count, scalars[index] == letter else { return false }
        index += 1
    }
    guard index < scalars.count, !"/?#".unicodeScalars.contains(scalars[index]) else { return false }
    return !scalars[index...].contains(where: isWhiteSpace)
}

/// `[a-z0-9_-]{1,32}`: a profile name, also used in file names and links.
nonisolated func isProfileName(_ text: String) -> Bool {
    let bytes = Array(text.utf8)
    return (1 ... 32).contains(bytes.count) && bytes.allSatisfy { byte in
        (UInt8(ascii: "a") ... UInt8(ascii: "z")).contains(byte) || isDigit(byte) || byte == UInt8(ascii: "_") ||
            byte == UInt8(ascii: "-")
    }
}

/// `(\*\*?\.)?[A-Za-z0-9-]{1,63}(\.[A-Za-z0-9-]{1,63})*`, at most 253 characters: what OkHttp's CertificatePinner
/// accepts and the iOS delegate matches — no scheme, port or path, labels of at most 63 characters, a host of at most 253,
/// an optional leading `*.` (one label) or `**.` (any depth).
nonisolated func isPinHost(_ text: String) -> Bool {
    var bytes = Array(text.utf8)[...]
    guard bytes.count <= 253 else { return false }
    if bytes.starts(with: Array("**.".utf8)) {
        bytes = bytes.dropFirst(3)
    } else if bytes.starts(with: Array("*.".utf8)) {
        bytes = bytes.dropFirst(2)
    }
    return bytes.split(separator: UInt8(ascii: "."), omittingEmptySubsequences: false).allSatisfy { label in
        (1 ... 63).contains(label.count) && label.allSatisfy { isLetterOrDigit($0) || $0 == UInt8(ascii: "-") }
    }
}

/// `[A-Za-z0-9+/]{43}=`: the base64 of a SHA-256 hash.
nonisolated func isPinValue(_ text: String) -> Bool {
    let bytes = Array(text.utf8)
    return bytes.count == 44 && bytes.last == UInt8(ascii: "=") && bytes.dropLast().allSatisfy { byte in
        isLetterOrDigit(byte) || byte == UInt8(ascii: "+") || byte == UInt8(ascii: "/")
    }
}

/// The White_Space set the contract lists for URLs.
nonisolated func isWhiteSpace(_ scalar: Unicode.Scalar) -> Bool {
    switch scalar.value {
    case 0x09 ... 0x0D, 0x20, 0x85, 0xA0, 0x1680, 0x2000 ... 0x200A, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000: true
    default: false
    }
}

nonisolated private func asciiLowercase(_ scalar: Unicode.Scalar) -> Unicode.Scalar {
    ("A" ... "Z").contains(scalar) ? Unicode.Scalar(scalar.value + 0x20)! : scalar
}

nonisolated private func isDigit(_ byte: UInt8) -> Bool {
    (UInt8(ascii: "0") ... UInt8(ascii: "9")).contains(byte)
}

nonisolated private func isLetterOrDigit(_ byte: UInt8) -> Bool {
    isDigit(byte) || (UInt8(ascii: "a") ... UInt8(ascii: "z")).contains(byte) ||
        (UInt8(ascii: "A") ... UInt8(ascii: "Z")).contains(byte)
}
