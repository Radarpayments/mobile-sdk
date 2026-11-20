nonisolated extension JSONValue {
    /// The value of [text] when the whole text is one JSON value (RFC 8259), or nil.
    ///
    /// `JSONSerialization` is not used: it turns numbers into `Double` or `NSDecimalNumber` (losing `1e400` and how a
    /// number was spelled), answers `true` and `1` with the same `NSNumber`, and fails a whole document where the
    /// contract wants the problem on one field.
    static func parse(_ text: String) -> JSONValue? {
        var parser = JSONParser(bytes: Array(text.utf8))
        guard let value = parser.value(depth: 1) else { return nil }
        parser.skipWhitespace()
        return parser.atEnd ? value : nil
    }
}

nonisolated private struct JSONParser {
    // The decoders check the depth before parsing (see DocumentLimits); this only keeps any other caller off the end of
    // the stack.
    private static let maxDepth = 512

    let bytes: [UInt8]
    var index = 0

    var atEnd: Bool {
        index == bytes.count
    }

    private var current: UInt8? {
        index < bytes.count ? bytes[index] : nil
    }

    mutating func skipWhitespace() {
        while let byte = current, byte == 0x20 || byte == 0x09 || byte == 0x0A || byte == 0x0D {
            index += 1
        }
    }

    mutating func value(depth: Int) -> JSONValue? {
        guard depth <= Self.maxDepth else { return nil }
        skipWhitespace()
        switch current {
        case UInt8(ascii: "{"): return object(depth: depth)
        case UInt8(ascii: "["): return array(depth: depth)
        case UInt8(ascii: "\""): return string().map(JSONValue.string)
        case UInt8(ascii: "t"): return literal("true", .bool(true))
        case UInt8(ascii: "f"): return literal("false", .bool(false))
        case UInt8(ascii: "n"): return literal("null", .null)
        default: return number()
        }
    }

    private mutating func consume(_ byte: UInt8) -> Bool {
        guard current == byte else { return false }
        index += 1
        return true
    }

    private mutating func literal(_ word: String, _ value: JSONValue) -> JSONValue? {
        let expected = Array(word.utf8)
        guard bytes[index...].starts(with: expected) else { return nil }
        index += expected.count
        return value
    }

    private mutating func object(depth: Int) -> JSONValue? {
        index += 1
        var members = JSONObject()
        skipWhitespace()
        if consume(UInt8(ascii: "}")) { return .object(members) }
        while true {
            skipWhitespace()
            guard current == UInt8(ascii: "\""), let key = string() else { return nil }
            skipWhitespace()
            guard consume(UInt8(ascii: ":")), let value = value(depth: depth + 1) else { return nil }
            members[key] = value
            skipWhitespace()
            if consume(UInt8(ascii: "}")) { return .object(members) }
            guard consume(UInt8(ascii: ",")) else { return nil }
        }
    }

    private mutating func array(depth: Int) -> JSONValue? {
        index += 1
        var items: [JSONValue] = []
        skipWhitespace()
        if consume(UInt8(ascii: "]")) { return .array(items) }
        while true {
            guard let item = value(depth: depth + 1) else { return nil }
            items.append(item)
            skipWhitespace()
            if consume(UInt8(ascii: "]")) { return .array(items) }
            guard consume(UInt8(ascii: ",")) else { return nil }
        }
    }

    private mutating func string() -> String? {
        index += 1
        var text: [UInt8] = []
        while let byte = current {
            index += 1
            switch byte {
            case UInt8(ascii: "\""):
                return String(decoding: text, as: UTF8.self)
            case UInt8(ascii: "\\"):
                guard let scalar = escape() else { return nil }
                text.append(contentsOf: Array(String(scalar).utf8))
            case 0x00 ..< 0x20:
                return nil
            default:
                text.append(byte)
            }
        }
        return nil
    }

    private mutating func escape() -> Unicode.Scalar? {
        guard let byte = current else { return nil }
        index += 1
        switch byte {
        case UInt8(ascii: "\""): return "\""
        case UInt8(ascii: "\\"): return "\\"
        case UInt8(ascii: "/"): return "/"
        case UInt8(ascii: "b"): return "\u{08}"
        case UInt8(ascii: "f"): return "\u{0C}"
        case UInt8(ascii: "n"): return "\n"
        case UInt8(ascii: "r"): return "\r"
        case UInt8(ascii: "t"): return "\t"
        case UInt8(ascii: "u"): break
        default: return nil
        }
        guard let unit = hexUnit() else { return nil }
        switch unit {
        case 0xD800 ..< 0xDC00:
            guard consume(UInt8(ascii: "\\")), consume(UInt8(ascii: "u")), let low = hexUnit(), (0xDC00 ..< 0xE000).contains(low)
            else { return nil }
            return Unicode.Scalar(0x10000 + ((unit - 0xD800) << 10) + (low - 0xDC00))
        case 0xDC00 ..< 0xE000:
            return nil
        default:
            return Unicode.Scalar(unit)
        }
    }

    private mutating func hexUnit() -> UInt32? {
        guard index + 4 <= bytes.count else { return nil }
        var unit: UInt32 = 0
        for _ in 0 ..< 4 {
            guard let digit = Character(Unicode.Scalar(bytes[index])).hexDigitValue else { return nil }
            unit = unit << 4 | UInt32(digit)
            index += 1
        }
        return unit
    }

    private mutating func number() -> JSONValue? {
        let start = index
        _ = consume(UInt8(ascii: "-"))
        if !consume(UInt8(ascii: "0")) {
            guard digits() > 0 else { return nil }
        }
        if consume(UInt8(ascii: ".")) {
            guard digits() > 0 else { return nil }
        }
        if consume(UInt8(ascii: "e")) || consume(UInt8(ascii: "E")) {
            if !consume(UInt8(ascii: "+")) { _ = consume(UInt8(ascii: "-")) }
            guard digits() > 0 else { return nil }
        }
        return .number(String(decoding: bytes[start ..< index], as: UTF8.self))
    }

    private mutating func digits() -> Int {
        let start = index
        while let byte = current, (UInt8(ascii: "0") ... UInt8(ascii: "9")).contains(byte) {
            index += 1
        }
        return index - start
    }
}
