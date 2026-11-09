import SwiftUI

/// The screens of one business; each cover's own flow plugs in here.
struct CoverRoot: View {
    let cover: CoverId
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var pathHolder: PathHolder

    var body: some View {
        content
            .navigationBarBackButtonHidden()
            .toolbar(.hidden, for: .navigationBar)
    }

    @ViewBuilder private var content: some View {
        let actions = CoverActions(
            back: {
                StorePrefs().lastCover = nil
                pathHolder.path = []
            },
            outcome: { outcome, amount in
                store.onOutcome(cover, outcome, amountMinor: amount)
                if outcome != .cancelled { pathHolder.push(.result(cover)) }
            },
            openSettings: { pathHolder.push(.developer) }
        )
        switch cover {
        case .furniture: FurnitureStoreView(actions: actions)
        case .coffee: CoffeeStoreView(actions: actions)
        case .tickets: TicketsStoreView(actions: actions)
        case .invest: InvestStoreView(back: actions.back, openSettings: actions.openSettings)
        }
    }
}

/// What every cover can do outside itself.
struct CoverActions {
    let back: () -> Void
    let outcome: (CheckoutOutcome, Int64) -> Void
    let openSettings: () -> Void
}

/// The store's result screen for `cover`.
struct CoverResult: View {
    let cover: CoverId
    @EnvironmentObject private var store: StoreModel
    @EnvironmentObject private var pathHolder: PathHolder

    var body: some View {
        if let receipt = store.lastReceipt {
            StoreResultView(
                cover: cover,
                receipt: receipt,
                orderNumber: store.lastOrderNumber,
                onDone: { pathHolder.popToPaymentRoot() },
                onRetry: { pathHolder.popToPaymentRoot() }
            )
            .toolbar(.hidden, for: .navigationBar)
        }
    }
}
