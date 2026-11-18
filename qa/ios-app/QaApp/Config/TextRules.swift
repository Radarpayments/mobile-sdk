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

/// Whether [text] equals the lower-case ASCII word [word] ignoring case the way Java's `equalsIgnoreCase` does on
/// Android: a character matches through its upper case or the lower case of that, so U+017F (long s) matches "s" and
/// U+212A matches "k".
nonisolated func equalsIgnoringCase(_ text: String, _ word: String) -> Bool {
    let scalars = Array(text.unicodeScalars)
    let letters = Array(word.unicodeScalars)
    guard scalars.count == letters.count else { return false }
    return zip(scalars, letters).allSatisfy { scalar, letter in
        let upper = simpleUppercase(scalar)
        return scalar == letter || upper == simpleUppercase(letter) || simpleLowercase(upper) == letter
    }
}

nonisolated func stringOf(_ scalars: some Sequence<Unicode.Scalar>) -> String {
    var view = String.UnicodeScalarView()
    view.append(contentsOf: scalars)
    return String(view)
}

// Java maps one char to one char; where Unicode's full mapping is longer (ß → SS) Java keeps the character.
nonisolated private func simpleUppercase(_ scalar: Unicode.Scalar) -> Unicode.Scalar {
    single(scalar.properties.uppercaseMapping) ?? scalar
}

nonisolated private func simpleLowercase(_ scalar: Unicode.Scalar) -> Unicode.Scalar {
    single(scalar.properties.lowercaseMapping) ?? scalar
}

nonisolated private func single(_ mapping: String) -> Unicode.Scalar? {
    var scalars = mapping.unicodeScalars.makeIterator()
    guard let first = scalars.next(), scalars.next() == nil else { return nil }
    return first
}
