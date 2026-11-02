import Foundation

/// The amount typed in the top-up field, in euros with `.` as the separator whatever the locale shows: at most six
/// whole digits and two decimals, no leading zeros.
struct TopUpAmount: Equatable {
    static let min: Int64 = 1000
    static let max: Int64 = 100_000
    static let presets: [Int64] = [5000, 10000, 25000, 50000]
    private static let decimals = 2
    private static let wholeDigits = 6

    var text: String = "100"

    /// Euro cents, or nil while nothing is typed.
    var minor: Int64? {
        guard !text.isEmpty else { return nil }
        let parts = text.split(separator: ".", maxSplits: 1, omittingEmptySubsequences: false)
        let whole = Int64(parts[0]) ?? 0
        let cents = parts.count > 1 ? Int64(String(parts[1]).padding(toLength: 2, withPad: "0", startingAt: 0)) ?? 0 : 0
        return whole * 100 + cents
    }

    var valid: Bool { minor.map { (Self.min...Self.max).contains($0) } ?? false }

    /// `c` typed at the end: a digit or `.`; anything that would break the format is ignored.
    func type(_ c: Character) -> TopUpAmount {
        if c == "." { return text.contains(".") ? self : TopUpAmount(text: (text.isEmpty ? "0" : text) + ".") }
        guard c.isASCII, c.isNumber else { return self }
        if let dot = text.firstIndex(of: ".") {
            return text.distance(from: dot, to: text.endIndex) > Self.decimals ? self : TopUpAmount(text: text + String(c))
        }
        if text == "0" { return TopUpAmount(text: String(c)) }
        return text.count >= Self.wholeDigits ? self : TopUpAmount(text: text + String(c))
    }

    /// What the system keyboard left in the field, cleaned as if typed key by key: the locale's separator (`,` or the
    /// Arabic `٫`) becomes `.`, Arabic-Indic and Persian digits become ASCII ones, anything else that does not fit the
    /// format is dropped.
    static func parse(_ input: String) -> TopUpAmount {
        input.reduce(TopUpAmount(text: "")) { $0.type(ascii($1)) }
    }

    /// The amount as the field shows it, with the locale's `separator`.
    func shown(separator: String) -> String { text.replacingOccurrences(of: ".", with: separator) }

    private static func ascii(_ c: Character) -> Character {
        if c == "," || c == "\u{066B}" { return "." }
        guard let scalar = c.unicodeScalars.first, c.unicodeScalars.count == 1 else { return c }
        for zero in [0x0660, 0x06F0] where (zero...zero + 9).contains(Int(scalar.value)) {
            return Character(String(Int(scalar.value) - zero))
        }
        return c
    }

    /// The amount of a preset chip.
    static func preset(_ minor: Int64) -> TopUpAmount {
        let cents = minor % 100
        let whole = String(minor / 100)
        guard cents != 0 else { return TopUpAmount(text: whole) }
        var fraction = String(format: "%02d", cents)
        while fraction.hasSuffix("0") { fraction.removeLast() }
        return TopUpAmount(text: whole + "." + fraction)
    }
}

/// Where the top-up screen is in the transfer.
enum TopUpStatus: Equatable {
    /// Typing the amount.
    case editing
    /// The bank or the gateway refused; `message` is the SDK's text, when it has one.
    case declined(message: String?)
    /// How the transfer ended is not known: the money is not credited and the app does not offer to pay again.
    case unconfirmed
    /// Credited: `amountMinor` of order `orderId` took free cash from `freeCashBefore` to `freeCashAfter`.
    case done(orderId: String, amountMinor: Int64, freeCashBefore: Int64, freeCashAfter: Int64, at: Int64)

    /// Whether "Continue" may start a transfer of `amount` now.
    func canContinue(_ amount: TopUpAmount) -> Bool {
        guard amount.valid else { return false }
        switch self {
        case .editing, .declined: return true
        case .unconfirmed, .done: return false
        }
    }
}
