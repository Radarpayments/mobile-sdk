import Foundation

/// The demo businesses. Every cover runs the same payment path and scenarios.
enum CoverId: String, CaseIterable, Codable {
    case furniture, coffee, tickets, invest
}

/// A thing the buyer can put in the cart; `priceMinor` is in euro cents. Names are localization keys.
struct Product: Identifiable, Equatable {
    let id: String
    let nameKey: String
    let subtitleKey: String?
    let priceMinor: Int64
    let image: String?

    var name: String { String(localized: String.LocalizationValue(nameKey), bundle: AppLanguage.bundle) }
    var subtitle: String? { subtitleKey.map { String(localized: String.LocalizationValue($0), bundle: AppLanguage.bundle) } }
}

/// Id of the per-order service fee line in the tickets cover.
let ticketFeeId = "fee"

/// What each demo business sells. Prices are euro cents, the same as the Android sample.
enum Catalog {
    static func products(_ cover: CoverId) -> [Product] {
        switch cover {
        case .furniture: return furniture
        case .coffee: return coffee
        case .tickets: return tickets
        // Northwind Invest sells nothing: the buyer tops up an account.
        case .invest: return []
        }
    }

    private static let furniture = [
        Product(id: "velvet_sofa", nameKey: "Velvet sofa", subtitleKey: "Emerald velvet, oak legs", priceMinor: 124_900, image: "furniture_sofa"),
        Product(id: "cloud_chair", nameKey: "Cloud chair", subtitleKey: "Milk-white bouclé", priceMinor: 32_900, image: "furniture_cloud_chair"),
        Product(id: "ono_chair", nameKey: "Ono chair", subtitleKey: "Moulded shell, beech legs", priceMinor: 18_900, image: "furniture_ono_chair"),
        Product(id: "birch_stool", nameKey: "Birch stool", subtitleKey: "Solid birch", priceMinor: 9_900, image: "furniture_stool"),
        Product(id: "arc_lamp", nameKey: "Arc floor lamp", subtitleKey: "Matte graphite steel", priceMinor: 21_900, image: "furniture_lamp"),
        Product(id: "lume_armchair", nameKey: "Lume armchair", subtitleKey: "Mustard wool", priceMinor: 45_900, image: "furniture_hero"),
    ]

    private static let coffee = [
        Product(id: "flat_white", nameKey: "Flat white", subtitleKey: "Double shot, steamed milk", priceMinor: 390, image: "coffee_flat_white"),
        Product(id: "cappuccino", nameKey: "Cappuccino", subtitleKey: "Classic, with foam", priceMinor: 360, image: "coffee_cappuccino"),
        Product(id: "espresso", nameKey: "Espresso", subtitleKey: "Single origin", priceMinor: 260, image: "coffee_hero"),
        Product(id: "iced_latte", nameKey: "Iced latte", subtitleKey: "Cold milk, ice", priceMinor: 420, image: "coffee_hero"),
        Product(id: "croissant", nameKey: "Butter croissant", subtitleKey: "Baked this morning", priceMinor: 350, image: "coffee_croissant"),
    ]

    private static let tickets = [
        Product(id: "floor", nameKey: "Standing floor", subtitleKey: "General admission", priceMinor: 4_500, image: nil),
        Product(id: "balcony", nameKey: "Balcony", subtitleKey: "Row 2 · seated", priceMinor: 6_500, image: nil),
        Product(id: ticketFeeId, nameKey: "Service fee", subtitleKey: nil, priceMinor: 350, image: nil),
    ]
}
