import SwiftUI

enum BrandGradient {
    case ctaPrimary, indigo, emerald, amber, rose, sky, violet, hotPink, success

    var stops: [Color] {
        switch self {
        case .ctaPrimary:  return [BrandColors.indigo, BrandColors.violet]
        case .indigo:      return [BrandColors.indigoLight, BrandColors.indigo]
        case .emerald:     return [BrandColors.emeraldLight, BrandColors.emerald]
        case .amber:       return [BrandColors.amberLight, BrandColors.amber]
        case .rose:        return [BrandColors.roseLight, BrandColors.rose]
        case .sky:         return [BrandColors.skyLight, BrandColors.sky]
        case .violet:      return [BrandColors.violetLight, BrandColors.violetDark]
        case .hotPink:     return [BrandColors.hotPink, BrandColors.hotPinkDeep]
        case .success:     return [BrandColors.success, BrandColors.emerald]
        }
    }

    func linear() -> LinearGradient {
        LinearGradient(colors: stops, startPoint: .topLeading, endPoint: .bottomTrailing)
    }
}
