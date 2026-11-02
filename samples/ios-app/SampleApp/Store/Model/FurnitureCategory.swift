import Foundation

/// Furniture categories behind the catalogue chips.
enum FurnitureCategory: String, CaseIterable {
    case all, chairs, sofas, light

    var label: String {
        switch self {
        case .all: return String(localized: "All", bundle: AppLanguage.bundle)
        case .chairs: return String(localized: "Chairs", bundle: AppLanguage.bundle)
        case .sofas: return String(localized: "Sofas", bundle: AppLanguage.bundle)
        case .light: return String(localized: "Light", bundle: AppLanguage.bundle)
        }
    }
}

/// Products of `category`; `.all` keeps every product.
func furnitureFilter(_ category: FurnitureCategory) -> [Product] {
    Catalog.products(.furniture).filter { category == .all || categoryOf($0.id) == category }
}

private func categoryOf(_ id: String) -> FurnitureCategory {
    switch id {
    case "velvet_sofa": return .sofas
    case "arc_lamp": return .light
    default: return .chairs
    }
}
