import Foundation

/// Seats picked for the concert: a category, how many (1–6), plus one service fee per order.
struct TicketOrder: Equatable {
    static let maxTickets = 6

    var categoryId = "balcony"
    var quantity = 2

    /// Tickets and the fee, in euro cents.
    var totalMinor: Int64 { toCart().totalMinor }

    /// The same order with `quantity` kept between 1 and `maxTickets`.
    func withQuantity(_ quantity: Int) -> TicketOrder {
        TicketOrder(categoryId: categoryId, quantity: min(max(quantity, 1), Self.maxTickets))
    }

    /// Cart lines the store pays for: the seats and the fee.
    func toCart() -> Cart {
        let products = Catalog.products(.tickets)
        return Cart(lines: [
            CartLine(product: products.first { $0.id == categoryId }!, quantity: quantity),
            CartLine(product: products.first { $0.id == ticketFeeId }!, quantity: 1),
        ])
    }
}
