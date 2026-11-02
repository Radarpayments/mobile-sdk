import Foundation

/// A fund in the demo portfolio; prices are fixed, `changeBasisPoints` is the month's change (410 = +4.1%).
struct Holding: Codable, Equatable {
    let ticker: String
    var valueMinor: Int64
    let unitPriceMinor: Int64
    let changeBasisPoints: Int

    /// Units of the fund the holding is worth.
    var quantity: Double { Double(valueMinor) / Double(unitPriceMinor) }
}

/// One line of the account history; `orderId` is the gateway order of a top-up.
struct InvestOperation: Codable, Equatable {
    enum Kind: String, Codable { case topUp = "TOP_UP", buy = "BUY" }

    let kind: Kind
    let amountMinor: Int64
    let atEpochMillis: Int64
    var orderId: String?
    var ticker: String?
}

/// The Northwind Invest demo account, kept on the device. Only `credit` brings money in, and only after the SDK reports
/// a completed transfer; `invest` moves it into a fund locally, without a payment.
struct InvestAccount: Codable, Equatable {
    var freeCashMinor: Int64
    var holdings: [Holding]
    var operations: [InvestOperation]

    static let globalEtf = "GLB"
    static let techLeaders = "TCH"
    private static let minInvestMinor: Int64 = 100

    var totalMinor: Int64 { freeCashMinor + holdings.reduce(0) { $0 + $1.valueMinor } }

    var canInvest: Bool { freeCashMinor >= Self.minInvestMinor }

    /// Adds a completed top-up of order `orderId`; a second result of the same order changes nothing.
    func credit(orderId: String, amountMinor: Int64, at: Int64) -> InvestAccount {
        if operations.contains(where: { $0.kind == .topUp && $0.orderId == orderId }) { return self }
        var account = self
        account.freeCashMinor += amountMinor
        account.operations.insert(InvestOperation(kind: .topUp, amountMinor: amountMinor, atEpochMillis: at, orderId: orderId), at: 0)
        return account
    }

    /// Buys `ticker` for `amountMinor`, at most the free cash; nothing when there is less than a euro to invest.
    func invest(amountMinor: Int64, at: Int64, ticker: String = InvestAccount.globalEtf) -> InvestAccount {
        guard canInvest else { return self }
        let amount = min(amountMinor, freeCashMinor)
        guard amount > 0 else { return self }
        var account = self
        account.freeCashMinor -= amount
        account.holdings = holdings.map { holding in
            var h = holding
            if h.ticker == ticker { h.valueMinor += amount }
            return h
        }
        account.operations.insert(InvestOperation(kind: .buy, amountMinor: amount, atEpochMillis: at, ticker: ticker), at: 0)
        return account
    }

    /// The latest `limit` operations, newest first.
    func recent(_ limit: Int = 5) -> [InvestOperation] { Array(operations.prefix(limit)) }

    /// The account as kept in the app settings.
    func toJson() -> String { (try? String(decoding: JSONEncoder().encode(self), as: UTF8.self)) ?? "" }

    /// The account saved with `toJson`; the seed when there is none or it cannot be read.
    static func fromJson(_ json: String?) -> InvestAccount {
        guard let json, !json.isEmpty, let account = try? JSONDecoder().decode(InvestAccount.self, from: Data(json.utf8)) else {
            return seed
        }
        return account
    }

    /// The account a fresh install starts with: €4,812.40 in total. Seed operations: 12 Oct 2026.
    static let seed = InvestAccount(
        freeCashMinor: 1240,
        holdings: [
            Holding(ticker: globalEtf, valueMinor: 290_000, unitPriceMinor: 23387, changeBasisPoints: 410),
            Holding(ticker: techLeaders, valueMinor: 190_000, unitPriceMinor: 37255, changeBasisPoints: 200),
        ],
        operations: [
            InvestOperation(kind: .buy, amountMinor: 25000, atEpochMillis: 1_791_800_400_000, ticker: globalEtf),
            InvestOperation(kind: .topUp, amountMinor: 25000, atEpochMillis: 1_791_800_100_000, orderId: "seed"),
        ]
    )
}
