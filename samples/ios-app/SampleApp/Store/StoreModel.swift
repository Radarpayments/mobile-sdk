import Combine
import Foundation

/// Carts of the demo businesses and the result of the last payment, shared by all store screens.
@MainActor
final class StoreModel: ObservableObject {
    /// What the store's result screen shows.
    struct Receipt: Equatable {
        let outcome: CheckoutOutcome
        let amountMinor: Int64
    }

    @Published private(set) var carts: [CoverId: Cart] = Dictionary(uniqueKeysWithValues: CoverId.allCases.map { ($0, Cart()) })
    @Published private(set) var paid: [CoverId: Cart] = [:]
    /// The last payment that ended in success or decline; nil after a cancel.
    @Published private(set) var lastReceipt: Receipt?
    /// Number of the last paid order, as the coffee shop calls it out.
    @Published private(set) var lastOrderNumber = firstOrderNumber - 1

    func cart(_ cover: CoverId) -> Cart { carts[cover] ?? Cart() }

    /// The last paid order of `cover`, or nil when nothing was paid there yet.
    func lastPaid(_ cover: CoverId) -> Cart? { paid[cover] }

    func add(_ cover: CoverId, _ product: Product) { edit(cover) { $0.add(product) } }

    func setQuantity(_ cover: CoverId, _ productId: String, _ quantity: Int) { edit(cover) { $0.setQuantity(productId, quantity) } }

    /// Replaces the cart of `cover`, e.g. with the seats picked for a concert.
    func replaceCart(_ cover: CoverId, _ cart: Cart) { carts[cover] = cart }

    /// Puts the last paid order of `cover` back into its cart, for "Order again".
    func repeatLast(_ cover: CoverId) {
        guard let last = paid[cover] else { return }
        carts[cover] = last
    }

    /// Records how the payment of `amountMinor` for `cover` ended: a success empties the cart.
    func onOutcome(_ cover: CoverId, _ outcome: CheckoutOutcome, amountMinor: Int64) {
        lastReceipt = outcome == .cancelled ? nil : Receipt(outcome: outcome, amountMinor: amountMinor)
        if case .success = outcome {
            paid[cover] = cart(cover)
            lastOrderNumber += 1
            carts[cover] = Cart()
        }
    }

    private func edit(_ cover: CoverId, _ change: (inout Cart) -> Void) {
        var cart = cart(cover)
        change(&cart)
        carts[cover] = cart
    }
}

private let firstOrderNumber = 42
