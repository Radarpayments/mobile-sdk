import Foundation

/// One product and how many of it the buyer takes.
struct CartLine: Equatable {
    let product: Product
    var quantity: Int
}

/// Shopping cart. Prices are euro cents.
struct Cart: Equatable {
    private(set) var lines: [CartLine] = []

    init(lines: [CartLine] = []) { self.lines = lines }

    /// Sum of all lines in euro cents.
    var totalMinor: Int64 { lines.reduce(0) { $0 + $1.product.priceMinor * Int64($1.quantity) } }
    /// Number of items, counting quantities.
    var count: Int { lines.reduce(0) { $0 + $1.quantity } }
    /// True when there is nothing to pay for.
    var isEmpty: Bool { lines.isEmpty }

    /// Adds one `product`.
    mutating func add(_ product: Product) {
        if let i = lines.firstIndex(where: { $0.product.id == product.id }) {
            lines[i].quantity += 1
        } else {
            lines.append(CartLine(product: product, quantity: 1))
        }
    }

    /// Sets the quantity of `productId`; 0 or less removes the line.
    mutating func setQuantity(_ productId: String, _ quantity: Int) {
        if quantity <= 0 {
            lines.removeAll { $0.product.id == productId }
        } else if let i = lines.firstIndex(where: { $0.product.id == productId }) {
            lines[i].quantity = quantity
        }
    }

    /// Empties the cart.
    mutating func clear() { lines = [] }
}

/// Formats euro `minor` units for `locale`, e.g. "€1,578.00" or "1 578,00 €".
func formatEur(_ minor: Int64, locale: Locale = AppLanguage.locale) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .currency
    formatter.currencyCode = "EUR"
    formatter.locale = locale
    return formatter.string(from: NSNumber(value: Double(minor) / 100)) ?? ""
}
