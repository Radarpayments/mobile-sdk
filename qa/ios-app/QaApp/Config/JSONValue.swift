/// A JSON value as the document spells it.
///
/// A number keeps its text: `Decimal` and `Double` cannot hold `1e400` or tell `1.50` from `1.5`, and Android's
/// kotlinx keeps the text too, so `order.extra` comes back as it was written. Strings and keys compare by Unicode
/// scalars, not by Swift's canonical equivalence, as the contract compares characters.
nonisolated indirect enum JSONValue: Equatable {
    case null
    case bool(Bool)
    case number(String)
    case string(String)
    case array([JSONValue])
    case object(JSONObject)

    static func == (lhs: JSONValue, rhs: JSONValue) -> Bool {
        switch (lhs, rhs) {
        case (.null, .null): true
        case let (.bool(left), .bool(right)): left == right
        case let (.number(left), .number(right)): left.unicodeScalars.elementsEqual(right.unicodeScalars)
        case let (.string(left), .string(right)): left.unicodeScalars.elementsEqual(right.unicodeScalars)
        case let (.array(left), .array(right)): left == right
        case let (.object(left), .object(right)): left == right
        default: false
        }
    }
}

/// A JSON object in reading order. A repeated key keeps its first place and its last value, as on Android.
///
/// Keys are told apart by their scalars: a Swift dictionary would merge `"apiKey"` with one spelled with the Kelvin
/// sign, which Android keeps as a different, unknown member.
nonisolated struct JSONObject: Equatable, ExpressibleByDictionaryLiteral, Sequence {
    private(set) var members: [(key: String, value: JSONValue)] = []
    private var positions: [[UInt8]: Int] = [:]

    init() {}

    init(dictionaryLiteral elements: (String, JSONValue)...) {
        for (key, value) in elements {
            self[key] = value
        }
    }

    var count: Int {
        members.count
    }

    var isEmpty: Bool {
        members.isEmpty
    }

    var keys: [String] {
        members.map(\.key)
    }

    subscript(key: String) -> JSONValue? {
        get {
            positions[Array(key.utf8)].map { members[$0].value }
        }
        set {
            let bytes = Array(key.utf8)
            if let position = positions[bytes] {
                if let newValue {
                    members[position].value = newValue
                } else {
                    members.remove(at: position)
                    positions = [:]
                    for (index, member) in members.enumerated() {
                        positions[Array(member.key.utf8)] = index
                    }
                }
            } else if let newValue {
                positions[bytes] = members.count
                members.append((key, newValue))
            }
        }
    }

    func makeIterator() -> IndexingIterator<[(key: String, value: JSONValue)]> {
        members.makeIterator()
    }

    static func == (lhs: JSONObject, rhs: JSONObject) -> Bool {
        lhs.count == rhs.count && lhs.members.allSatisfy { rhs[$0.key] == $0.value }
    }
}
