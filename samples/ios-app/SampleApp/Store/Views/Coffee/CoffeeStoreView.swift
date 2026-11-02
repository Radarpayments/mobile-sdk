import SwiftUI

/// Roast Lane: menu → order → the shared payment bar; "Order again" pays the last order right away.
struct CoffeeStoreView: View {
    let actions: CoverActions
    @EnvironmentObject private var store: StoreModel
    @State private var onOrder = false
    @State private var payNow = false
    @State private var tab = Tab.coffee

    private let style = coverStyle(.coffee)

    enum Tab: CaseIterable {
        case coffee, cold, bakery

        var label: LocalizedStringKey {
            switch self {
            case .coffee: return "Coffee"
            case .cold: return "Cold"
            case .bakery: return "Bakery"
            }
        }
    }

    private func tabOf(_ id: String) -> Tab {
        switch id {
        case "iced_latte": return .cold
        case "croissant": return .bakery
        default: return .coffee
        }
    }

    var body: some View {
        let cart = store.cart(.coffee)
        // An empty order is never shown: after a paid order the buyer is back at the menu, with "Order again".
        Group {
            if onOrder && !cart.isEmpty { order(cart) } else { menu(cart) }
        }
        .background(style.background.ignoresSafeArea())
        .preferredColorScheme(.dark)
    }

    // MARK: Menu

    private func menu(_ cart: Cart) -> some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        backButton(actions.back)
                        Text(coverName(.coffee)).font(style.heading(26, .heavy)).foregroundColor(style.text)
                        Spacer()
                        Label { Text(verbatim: "King St.") } icon: { Image(systemName: "mappin") }
                            .font(style.body(13)).foregroundColor(style.muted)
                    }
                    if let last = store.lastPaid(.coffee), !last.isEmpty { again(last) }
                    HStack(spacing: 20) {
                        ForEach(Tab.allCases, id: \.self) { t in
                            Button { tab = t } label: {
                                VStack(spacing: 4) {
                                    Text(t.label).font(style.body(15, .bold)).foregroundColor(t == tab ? style.text : style.muted)
                                    Capsule().fill(t == tab ? style.primary : .clear).frame(width: 28, height: 2)
                                }
                                .frame(minHeight: 44)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    ForEach(Catalog.products(.coffee).filter { tabOf($0.id) == tab }) { p in row(p) }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 96)
            }
            if !cart.isEmpty {
                Button { onOrder = true } label: {
                    HStack {
                        Text("Cart · \(cart.count)")
                        Spacer()
                        Text(formatEur(cart.totalMinor))
                    }
                    .font(style.body(15, .heavy))
                    .foregroundColor(style.onPrimary)
                    .padding(.horizontal, 22)
                    .frame(minHeight: 56)
                    .background(Capsule().fill(style.primary))
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("open_order")
                .padding(16)
            }
        }
    }

    private func again(_ last: Cart) -> some View {
        HStack(spacing: 0) {
            Image("coffee_flat_white").resizable().scaledToFill().allowsHitTesting(false).frame(width: 104).clipped()
            VStack(alignment: .leading, spacing: 2) {
                Text("Same as last time").font(style.body(12)).foregroundColor(style.muted)
                Text(last.lines.map(\.product.name).joined(separator: ", ")).font(style.body(15, .bold)).foregroundColor(style.text).lineLimit(1)
                Text(formatEur(last.totalMinor)).font(style.body(13)).foregroundColor(style.muted)
                Button {
                    store.repeatLast(.coffee)
                    payNow = true
                    onOrder = true
                } label: {
                    Text("Order again").font(style.body(14, .heavy)).foregroundColor(style.onPrimary)
                        .padding(.horizontal, 18).frame(minHeight: 40)
                        .background(RoundedRectangle(cornerRadius: 12).fill(style.primary))
                }
                .buttonStyle(.plain)
                .padding(.top, 6)
            }
            .padding(12)
            Spacer(minLength: 0)
        }
        .frame(height: 132)
        .background(style.surface)
        .clipShape(RoundedRectangle(cornerRadius: style.radius))
    }

    private func row(_ p: Product) -> some View {
        HStack(spacing: 12) {
            Image(p.image ?? "").resizable().scaledToFill().allowsHitTesting(false).frame(width: 64, height: 64).clipShape(RoundedRectangle(cornerRadius: 16))
            VStack(alignment: .leading, spacing: 1) {
                Text(p.name).font(style.body(15, .bold)).foregroundColor(style.text)
                if let s = p.subtitle { Text(s).font(style.body(13)).foregroundColor(style.muted).lineLimit(1) }
                Text(formatEur(p.priceMinor)).font(style.body(13)).foregroundColor(style.muted)
            }
            Spacer()
            Button { store.add(.coffee, p) } label: {
                Image(systemName: "plus").font(.system(size: 16, weight: .bold)).foregroundColor(style.onPrimary)
                    .frame(width: 36, height: 36).background(Circle().fill(style.primary))
                    .frame(width: 44, height: 44)
            }
            .accessibilityLabel(Text("Add \(p.name)"))
            .accessibilityIdentifier("add_\(p.id)")
        }
        .padding(.vertical, 8)
    }

    // MARK: Order

    private func order(_ cart: Cart) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                backButton { onOrder = false }
                Text("Your order").font(style.heading(26, .heavy)).foregroundColor(style.text)
            }
            ScrollView {
                VStack(spacing: 0) {
                    ForEach(cart.lines, id: \.product.id) { line in
                        HStack(spacing: 12) {
                            Image(line.product.image ?? "").resizable().scaledToFill().allowsHitTesting(false).frame(width: 56, height: 56)
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                            VStack(alignment: .leading) {
                                Text(line.product.name).font(style.body(15, .bold)).foregroundColor(style.text)
                                Text(formatEur(line.product.priceMinor * Int64(line.quantity))).font(style.body(13)).foregroundColor(style.muted)
                            }
                            Spacer()
                            Button { store.setQuantity(.coffee, line.product.id, line.quantity - 1) } label: {
                                Image(systemName: "minus").frame(width: 44, height: 44)
                            }.accessibilityLabel(Text("Less"))
                            Text("\(line.quantity)").font(style.body(15, .bold))
                            Button { store.setQuantity(.coffee, line.product.id, line.quantity + 1) } label: {
                                Image(systemName: "plus").frame(width: 44, height: 44)
                            }.accessibilityLabel(Text("More"))
                        }
                        .foregroundColor(style.text)
                        .padding(.vertical, 8)
                    }
                    HStack(spacing: 10) {
                        Text("Pick up").foregroundColor(style.muted)
                        Text("King St. · in 10 min").fontWeight(.bold).foregroundColor(style.text)
                        Spacer()
                    }
                    .font(style.body(14))
                    .padding(14)
                    .background(style.surface)
                    .clipShape(RoundedRectangle(cornerRadius: style.radius))
                    .padding(.top, 12)
                }
            }
            StoreCheckoutBar(cover: .coffee, amountMinor: cart.totalMinor, autoPay: $payNow,
                             onOutcome: { actions.outcome($0, cart.totalMinor) }, onOpenSettings: actions.openSettings)
                .padding(.vertical, 12)
        }
        .padding(.horizontal, 16)
    }

    private func backButton(_ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: "chevron.left").font(.system(size: 18, weight: .semibold)).foregroundColor(style.text)
                .frame(width: 44, height: 44)
        }
        .accessibilityLabel(Text("Back"))
    }
}
