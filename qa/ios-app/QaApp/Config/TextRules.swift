import Foundation

// What both apps call a character, a blank and the same text. The contract counts Unicode scalars without
// normalisation, while Swift's String compares by canonical equivalence (U+212A KELVIN SIGN equals "K", so "\u{212A}ZT"
// would pass for KZT) and its Character merges "\r\n" and a comma with a combining mark. These helpers work on scalars.

/// Blank strings, trimming and whitespace in features follow Android, where Kotlin's `isWhitespace` decides: Unicode's
/// White_Space without U+0085, plus the separators U+001C–U+001F.
nonisolated func isBlankSpace(_ scalar: Unicode.Scalar) -> Bool {
    switch scalar.value {
    case 0x09 ... 0x0D, 0x1C ... 0x20, 0xA0, 0x1680, 0x2000 ... 0x200A, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000: true
    default: false
    }
}

nonisolated func isBlank(_ text: String) -> Bool {
    text.unicodeScalars.allSatisfy(isBlankSpace)
}

nonisolated func trimmed(_ text: String) -> String {
    let scalars = text.unicodeScalars
    guard let first = scalars.firstIndex(where: { !isBlankSpace($0) }),
          let last = scalars.lastIndex(where: { !isBlankSpace($0) })
    else { return "" }
    return stringOf(scalars[first ... last])
}

/// Whether two strings are the same scalars.
nonisolated func sameText(_ left: String, _ right: String) -> Bool {
    left.unicodeScalars.elementsEqual(right.unicodeScalars)
}

nonisolated func stringOf(_ scalars: some Sequence<Unicode.Scalar>) -> String {
    var view = String.UnicodeScalarView()
    view.append(contentsOf: scalars)
    return String(view)
}

/// Whether [text] equals [word] ignoring case the way Java's `equalsIgnoreCase` does on Android (see `caseKey`).
nonisolated func equalsIgnoringCase(_ text: String, _ word: String) -> Bool {
    text.unicodeScalars.map(caseKey).elementsEqual(word.unicodeScalars.map(caseKey))
}

/// Whether [text] contains [query] ignoring case the way Kotlin's `contains(ignoreCase = true)` does on Android (see
/// `caseKey`). An empty query is contained in any text.
nonisolated func containsIgnoringCase(_ text: String, _ query: String) -> Bool {
    FoldedText(text).contains(FoldedText(query))
}

/// A text with every character replaced by its `caseKey`, in UTF-8: two texts match ignoring case where their folded
/// bytes are equal, and UTF-8 never matches inside a character, so a search is a byte search. The journal folds each
/// entry once, as it is recorded: filtering thousands of entries on every keystroke then runs `memmem` per entry, which
/// stays fast in the unoptimised builds the app runs as.
nonisolated struct FoldedText: Equatable {
    let bytes: [UInt8]

    init(_ text: String) {
        var bytes: [UInt8] = []
        bytes.reserveCapacity(text.utf8.count)
        for scalar in text.unicodeScalars {
            let key = caseKey(scalar)
            if key < 0x80 {
                bytes.append(UInt8(key))
            } else {
                bytes.append(contentsOf: String(Unicode.Scalar(key)!).utf8)
            }
        }
        self.bytes = bytes
    }

    func contains(_ other: FoldedText) -> Bool {
        if other.bytes.isEmpty { return true }
        return bytes.withUnsafeBytes { text in
            other.bytes.withUnsafeBytes { wanted in
                memmem(text.baseAddress, text.count, wanted.baseAddress, wanted.count) != nil
            }
        }
    }
}

/// The search by keys one character at a time, as Java does it; kept to check `FoldedText` against.
nonisolated func containsIgnoringCaseScalarByScalar(_ text: String, _ query: String) -> Bool {
    let textKeys = text.unicodeScalars.map(caseKey)
    let keys = query.unicodeScalars.map(caseKey)
    guard keys.count <= textKeys.count else { return false }
    return (0 ... textKeys.count - keys.count).contains { start in
        textKeys[start ..< start + keys.count].elementsEqual(keys)
    }
}

/// What Java compares when it ignores case, one UTF-16 char at a time: two chars match when they are equal, when their
/// upper cases are, or when the lower cases of those are, all with Java's one-to-one mappings. That is equality of this
/// key, the lower case of the upper case. A character beyond the basic plane is two surrogate chars to Java, which no
/// mapping changes, so it matches only itself.
nonisolated func caseKey(_ scalar: Unicode.Scalar) -> UInt32 {
    switch scalar.value {
    case 0x41 ... 0x5A: scalar.value + 0x20
    case 0 ..< 0x80: scalar.value
    case 0x80 ... 0xFFFF: UInt32(basicPlaneKeys[Int(scalar.value)])
    default: scalar.value
    }
}

// Built once: asking Unicode for the mappings costs far more than a look-up.
nonisolated private let basicPlaneKeys: [UInt16] = (0 ... 0xFFFF).map { value in
    guard let scalar = Unicode.Scalar(UInt32(value)) else { return UInt16(value) }
    return UInt16(javaLowercase(javaUppercase(scalar)).value)
}

// Java maps one char to one char. Where Unicode's full mapping is longer (ß → SS) Java keeps the character; where the
// simple one differs from that (U+1F80 → U+1F88), the key does not change, as U+1F88 lowers back to U+1F80.
nonisolated private func javaUppercase(_ scalar: Unicode.Scalar) -> Unicode.Scalar {
    basicPlane(single(scalar.properties.uppercaseMapping)) ?? scalar
}

nonisolated private func javaLowercase(_ scalar: Unicode.Scalar) -> Unicode.Scalar {
    // The one character whose full lower case is longer: U+0130 is "i" plus a combining dot, Java's is "i".
    if scalar.value == 0x130 { return "i" }
    return basicPlane(single(scalar.properties.lowercaseMapping)) ?? scalar
}

nonisolated private func basicPlane(_ scalar: Unicode.Scalar?) -> Unicode.Scalar? {
    scalar.flatMap { $0.value <= 0xFFFF ? $0 : nil }
}

nonisolated private func single(_ mapping: String) -> Unicode.Scalar? {
    var scalars = mapping.unicodeScalars.makeIterator()
    guard let first = scalars.next(), scalars.next() == nil else { return nil }
    return first
}
