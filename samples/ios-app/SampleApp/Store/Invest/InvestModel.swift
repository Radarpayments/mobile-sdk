import Combine
import Foundation

/// Northwind Invest: the account shown (the saved one), the amount in the top-up field and how the last transfer
/// ended. Money is credited only when the SDK reports the transfer completed.
@MainActor
final class InvestModel: ObservableObject {
    @Published private(set) var account: InvestAccount
    @Published private(set) var amount = TopUpAmount()
    @Published private(set) var status: TopUpStatus = .editing

    private let load: () -> InvestAccount
    private let save: (InvestAccount) -> Void
    private let clock: () -> Int64

    init(
        load: @escaping () -> InvestAccount = { StorePrefs().investAccount },
        save: @escaping (InvestAccount) -> Void = { StorePrefs().investAccount = $0 },
        clock: @escaping () -> Int64 = { Int64(Date().timeIntervalSince1970 * 1000) }
    ) {
        self.load = load
        self.save = save
        self.clock = clock
        account = load()
    }

    /// The account as saved on the device (it changes after a reset in the developer settings).
    func reload() { account = load() }

    /// The amount field changed.
    func edit(_ input: String) { amount = TopUpAmount.parse(input) }

    /// A preset chip.
    func preset(_ minor: Int64) { amount = TopUpAmount.preset(minor) }

    /// The sheet closed after a transfer of `amountMinor`.
    func onOutcome(_ outcome: CheckoutOutcome, amountMinor: Int64) {
        switch outcome {
        case let .success(paymentId):
            let before = account
            let after = before.credit(orderId: paymentId, amountMinor: amountMinor, at: clock())
            guard after != before else { return }
            update(after)
            status = .done(orderId: paymentId, amountMinor: amountMinor, freeCashBefore: before.freeCashMinor,
                           freeCashAfter: after.freeCashMinor, at: clock())
        case let .declined(message): status = .declined(message: message)
        case .unconfirmed: status = .unconfirmed
        case .cancelled: status = .editing
        }
    }

    /// The sheet of "Add card" closed: the account does not change, whatever the outcome.
    func onCardOutcome(_ outcome: CheckoutOutcome) {
        switch outcome {
        case .success: status = .cardAdded
        case let .declined(message): status = .cardDeclined(message: message)
        case .unconfirmed: status = .cardUnconfirmed
        case .cancelled: status = .editing
        }
    }

    /// Buys the Global ETF for `amountMinor` (at most the free cash), without a payment.
    func invest(_ amountMinor: Int64) { update(account.invest(amountMinor: amountMinor, at: clock())) }

    /// The buyer left the top-up screen: its banner goes, the amount stays.
    func leaveTopUp() { status = .editing }

    private func update(_ next: InvestAccount) {
        guard next != account else { return }
        account = next
        save(next)
    }
}
