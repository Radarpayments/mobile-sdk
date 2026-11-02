import Foundation

/// The developer settings ask for AFT transfers: the start screen then shows Northwind Invest, where a transfer tops up
/// an account, instead of the stores, where it would look like buying goods. Transfers go through API V1 only.
func isAftDemo(apiVersion: ApiVersion, orderType: OrderType) -> Bool { apiVersion == .v1 && orderType == .aft }

/// The businesses on the start screen.
func visibleCovers(aftDemo: Bool) -> [CoverId] { aftDemo ? [.invest] : [.furniture, .coffee, .tickets] }

/// Whether `cover` belongs to the mode: a business left open while the settings switched modes would register the
/// other kind of order (a purchase in Northwind, a transfer for a cart).
func coverFits(_ cover: CoverId, aftDemo: Bool) -> Bool { visibleCovers(aftDemo: aftDemo).contains(cover) }

/// The business to reopen at launch: the last one, unless the settings have since hidden it.
func reopenCover(_ last: CoverId?, aftDemo: Bool) -> CoverId? {
    guard let last, coverFits(last, aftDemo: aftDemo) else { return nil }
    return last
}

/// The screens to stay on once the settings close: back on a business the mode no longer shows, the start screen
/// (which shows the ones it does). The settings themselves stay while open.
func pathFitting(_ path: [SampleRoute], aftDemo: Bool) -> [SampleRoute] {
    switch path.last {
    case let .cover(cover)?, let .result(cover)?: return coverFits(cover, aftDemo: aftDemo) ? path : []
    default: return path
    }
}
