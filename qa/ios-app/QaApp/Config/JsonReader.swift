nonisolated struct Problem: Equatable {
    var field: String
    var message: String
}

nonisolated enum Decoded<T> {
    case ok(T)
    case invalid(Problem)
}

nonisolated extension Decoded: Equatable where T: Equatable {}

nonisolated extension Problem {
    static let notAnObject = Problem(field: "$", message: "the document is not a JSON object")
}

/// The first problem of [problems] in the order of [fields]; a field that is not listed comes last.
nonisolated func firstInOrder(_ problems: [Problem], _ fields: [String]) -> Problem? {
    problems.enumerated().min { left, right in
        let leftRank = fields.firstIndex(of: left.element.field) ?? Int.max
        let rightRank = fields.firstIndex(of: right.element.field) ?? Int.max
        return (leftRank, left.offset) < (rightRank, right.offset)
    }?.element
}

/// Typed reads over a JSON object. A value of the wrong JSON type is recorded as a problem named by its path and the
/// default is used, so that one pass collects every problem; the caller reports the first in table order.
nonisolated final class JsonReader {
    // The most digits an accepted integral number can have: amounts end at 999999999999.
    private static let maxIntegerDigits = 13

    private(set) var problems: [Problem] = []

    func fail(_ field: String, _ message: String) {
        problems.append(Problem(field: field, message: message))
    }

    private func present(_ source: JSONObject, _ key: String) -> JSONValue? {
        guard let value = source[key], value != .null else { return nil }
        return value
    }

    func group(_ source: JSONObject, _ key: String, _ path: String) -> JSONObject {
        guard let value = present(source, key) else { return [:] }
        if case let .object(object) = value { return object }
        fail(path, "must be an object")
        return [:]
    }

    func text(_ source: JSONObject, _ key: String, _ path: String) -> String? {
        guard let value = present(source, key) else { return nil }
        if case let .string(text) = value { return text }
        fail(path, "must be a string")
        return nil
    }

    func nullableText(_ source: JSONObject, _ key: String, _ path: String) -> String? {
        text(source, key, path).flatMap { isBlank($0) ? nil : $0 }
    }

    func flag(_ source: JSONObject, _ key: String, _ path: String, _ fallback: Bool) -> Bool {
        guard let value = present(source, key) else { return fallback }
        if case let .bool(flag) = value { return flag }
        fail(path, "must be true or false")
        return fallback
    }

    /// An integral JSON number by value (`100`, `100.0` and `1e2` are all 100). A number of more than 13 integer digits
    /// is returned as the largest or smallest Int64 without being expanded, so an exponent like `1e999999999` costs
    /// nothing and fails the range check of the caller.
    func integral(_ source: JSONObject, _ key: String, _ path: String) -> Int64? {
        guard let value = present(source, key) else { return nil }
        var result: Int64?
        if case let .number(text) = value { result = Self.integralValue(text) }
        if result == nil { fail(path, "must be an integer") }
        return result
    }

    func names(_ source: JSONObject, _ key: String, _ path: String) -> [String]? {
        guard let value = present(source, key) else { return nil }
        if case let .array(items) = value {
            let names = items.compactMap { item -> String? in
                if case let .string(text) = item { return text }
                return nil
            }
            if names.count == items.count { return names }
        }
        fail(path, "must be a list of strings")
        return nil
    }

    func choice<E: RawRepresentable & CaseIterable>(
        _ source: JSONObject,
        _ key: String,
        _ path: String,
        _ fallback: E
    ) -> E where E.RawValue == String {
        guard let name = text(source, key, path) else { return fallback }
        if let value = E.allCases.first(where: { sameText($0.rawValue, name) }) { return value }
        fail(path, "must be one of \(E.allCases.map(\.rawValue).joined(separator: ", "))")
        return fallback
    }

    /// The value of a JSON number as Java's `BigDecimal` judges it on Android: an exponent of more than ten digits or a
    /// scale outside Int32 is not a number at all, zero is 0 whatever its exponent, and a fraction is not integral.
    static func integralValue(_ text: String) -> Int64? {
        let bytes = Array(text.utf8)
        var index = 0
        let negative = bytes.first == UInt8(ascii: "-")
        if negative { index += 1 }
        var digits: [UInt8] = []
        var fractionDigits = 0
        var inFraction = false
        while index < bytes.count, bytes[index] != UInt8(ascii: "e"), bytes[index] != UInt8(ascii: "E") {
            if bytes[index] == UInt8(ascii: ".") {
                inFraction = true
            } else {
                digits.append(bytes[index] - UInt8(ascii: "0"))
                if inFraction { fractionDigits += 1 }
            }
            index += 1
        }
        var exponent: Int64 = 0
        if index < bytes.count {
            index += 1
            var exponentNegative = false
            if bytes[index] == UInt8(ascii: "+") || bytes[index] == UInt8(ascii: "-") {
                exponentNegative = bytes[index] == UInt8(ascii: "-")
                index += 1
            }
            let exponentDigits = bytes[index...].drop { $0 == UInt8(ascii: "0") }
            guard exponentDigits.count <= 10 else { return nil }
            for digit in exponentDigits {
                exponent = exponent * 10 + Int64(digit - UInt8(ascii: "0"))
            }
            if exponentNegative { exponent = -exponent }
        }
        let scale = Int64(fractionDigits) - exponent
        guard scale >= Int64(Int32.min), scale <= Int64(Int32.max) else { return nil }
        let significant = digits.drop { $0 == 0 }
        if significant.isEmpty { return 0 }
        if Int64(significant.count) - scale > Int64(maxIntegerDigits) { return negative ? Int64.min : Int64.max }
        let trailingZeros = significant.reversed().prefix { $0 == 0 }.count
        let strippedScale = scale - Int64(trailingZeros)
        guard strippedScale <= 0 else { return nil }
        var value: Int64 = 0
        for digit in significant.dropLast(trailingZeros) {
            value = value * 10 + Int64(digit)
        }
        for _ in 0 ..< -strippedScale {
            value *= 10
        }
        return negative ? -value : value
    }
}
