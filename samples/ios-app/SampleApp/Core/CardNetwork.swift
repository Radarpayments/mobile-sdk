/// Card network by the number's leading digits, with the ranges, lengths and grouping the SDK's own card form uses.
enum CardNetwork: String {
    case visa = "VISA"
    case mastercard = "MASTERCARD"
    case amex = "AMEX"
    case unionPay = "UNIONPAY"
    case jcb = "JCB"
    case discover = "DISCOVER"
    case diners = "DINERS"
    case maestro = "MAESTRO"
    case unknown = "UNKNOWN"

    init(cardNumber: String) {
        let digits = cardNumber.filter(\.isNumber)
        func prefix(_ count: Int) -> Int? { digits.count >= count ? Int(digits.prefix(count)) : nil }
        let two = prefix(2), three = prefix(3), four = prefix(4)
        switch true {
        case digits.hasPrefix("4"): self = .visa
        case two == 34 || two == 37: self = .amex
        case (51...55).contains(two ?? 0) || (2221...2720).contains(four ?? 0): self = .mastercard
        case two == 62: self = .unionPay
        case two == 36 || two == 38 || two == 39 || (300...305).contains(three ?? 0) || four == 3095: self = .diners
        case (3528...3589).contains(four ?? 0): self = .jcb
        case four == 6011 || (644...649).contains(three ?? 0) || two == 65: self = .discover
        case two == 50 || (56...58).contains(two ?? 0) || four == 6304 || four == 6759 || (6761...6763).contains(four ?? 0):
            self = .maestro
        default: self = .unknown
        }
    }

    var displayName: String {
        switch self {
        case .visa: "Visa"
        case .mastercard: "Mastercard"
        case .amex: "American Express"
        case .unionPay: "UnionPay"
        case .jcb: "JCB"
        case .discover: "Discover"
        case .diners: "Diners Club"
        case .maestro: "Maestro"
        case .unknown: ""
        }
    }

    /// Longest number the network issues: input beyond it is dropped.
    var maxPanLength: Int {
        switch self {
        case .amex: 15
        case .mastercard: 16
        default: 19
        }
    }

    /// 4 digits on the front for Amex, 3 on the back otherwise.
    var cvcLength: Int { self == .amex ? 4 : 3 }

    /// `digits` in the network's groups: "4111 1111 1111 1111", Amex 4-6-5.
    func format(_ digits: String) -> String {
        let groups = self == .amex ? [4, 6, 5] : [4, 4, 4, 4, 3]
        var rest = Substring(digits.filter(\.isNumber))
        var parts: [Substring] = []
        for size in groups where !rest.isEmpty {
            parts.append(rest.prefix(size))
            rest = rest.dropFirst(size)
        }
        return parts.joined(separator: " ")
    }
}
