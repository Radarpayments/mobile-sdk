nonisolated enum DocumentLimits {
    /// The largest document, in bytes of UTF-8.
    static let maxBytes = 65536

    /// The deepest nesting of objects and arrays in a document; the root object is level 1.
    static let maxDepth = 32
}

/// Why [text] is over [maxBytes] bytes of UTF-8 or nests objects and arrays deeper than [maxDepth] levels, or nil when
/// it is within both. Brackets inside strings do not count. The text is not echoed.
///
/// The parser is recursive, and a configuration arrives from any link, QR code or clipboard: the limits are checked in
/// one pass before it runs. The bytes that matter here are ASCII, which never occurs inside a multi-byte sequence.
nonisolated func limitProblem(
    _ text: String,
    maxBytes: Int = DocumentLimits.maxBytes,
    maxDepth: Int = DocumentLimits.maxDepth
) -> String? {
    var bytes = 0
    var depth = 0
    var inString = false
    var escaped = false
    for byte in text.utf8 {
        bytes += 1
        if bytes > maxBytes { return "must be at most \(maxBytes) bytes of UTF-8" }
        if escaped {
            escaped = false
        } else if inString, byte == UInt8(ascii: "\\") {
            escaped = true
        } else if byte == UInt8(ascii: "\"") {
            inString.toggle()
        } else if inString {
            continue
        } else if byte == UInt8(ascii: "{") || byte == UInt8(ascii: "[") {
            depth += 1
            if depth > maxDepth { return "must nest at most \(maxDepth) levels deep" }
        } else if byte == UInt8(ascii: "}") || byte == UInt8(ascii: "]") {
            depth -= 1
        }
    }
    return nil
}
