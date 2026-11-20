/// The currencies an order may be in, ISO 4217 alphabetic to numeric (the gateway takes the numeric code), with the
/// digits of their minor unit.
nonisolated enum Currencies {
    private static let numericByAlpha = [
        ("EUR", "978"),
        ("USD", "840"),
        ("GBP", "826"),
        ("CHF", "756"),
        ("RUB", "643"),
        ("KZT", "398"),
        ("BGN", "975"),
        ("RON", "946"),
        ("PLN", "985"),
        ("CZK", "203"),
        ("TRY", "949"),
        ("AED", "784"),
        ("UAH", "980"),
        ("BYN", "933"),
        ("GEL", "981"),
        ("AMD", "051"),
        ("AZN", "944"),
        ("UZS", "860"),
    ]

    static let codes = numericByAlpha.map(\.0)

    static func numeric(_ alpha: String) -> String? {
        numericByAlpha.first { sameText($0.0, alpha) }?.1
    }

    /// Digits after the point in the form's amount. Every listed currency has two; an unknown one gets two as well, so a
    /// typed code still shows an amount.
    static func minorDigits(_ alpha: String) -> Int {
        numericByAlpha.contains { sameText($0.0, alpha) } ? 2 : defaultMinorDigits
    }

    private static let defaultMinorDigits = 2
}
