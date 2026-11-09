import SwiftUI

/// Hearth & Co: catalogue → product → cart → the shared payment bar.
struct FurnitureStoreView: View {
    let actions: CoverActions
    @EnvironmentObject private var store: StoreModel
    /// nil = catalogue, "cart" = the cart, otherwise a product id.
    @State private var screen: String?
    @State private var category = FurnitureCategory.all
    /// The product photo flies from its tile to the top of the product page and back.
    @Namespace private var photos

    private let style = coverStyle(.furniture)
    private static let flight = Animation.spring(response: 0.42, dampingFraction: 0.86)

    var body: some View {
        let cart = store.cart(.furniture)
        // An empty cart is never shown: after a paid order the buyer is back at the catalogue.
        let shown = screen == "cart" && cart.isEmpty ? nil : screen
        Group {
            switch shown {
            case nil: catalog(cart)
            case "cart": cartScreen(cart)
            case let id?: product(Catalog.products(.furniture).first { $0.id == id }!).zIndex(1)
            }
        }
        .animation(Self.flight, value: shown)
        .background(style.background.ignoresSafeArea())
        .preferredColorScheme(.light)
    }

    // MARK: Catalogue

    private func catalog(_ cart: Cart) -> some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        backButton(actions.back)
                        Text(coverName(.furniture)).font(style.heading(28)).foregroundColor(style.text)
                        Spacer()
                        Button { screen = "cart" } label: {
                            ZStack(alignment: .topTrailing) {
                                Image(systemName: "basket").font(.system(size: 18)).foregroundColor(style.text)
                                    .frame(width: 42, height: 42).overlay(Circle().strokeBorder(style.border))
                                if cart.count > 0 {
                                    Text("\(cart.count)").font(style.body(11, .bold)).foregroundColor(.white)
                                        .padding(.horizontal, 6).background(Capsule().fill(style.accent))
                                }
                            }
                            .frame(width: 48, height: 48)
                        }
                        .accessibilityLabel(Text("Cart · \(cart.count)"))
                        .accessibilityIdentifier("open_cart")
                    }
                    ZStack(alignment: .bottomLeading) {
                        Image("furniture_hero").resizable().scaledToFill().allowsHitTesting(false).frame(height: 184).frame(maxWidth: .infinity).clipped()
                        LinearGradient(colors: [.clear, .black.opacity(0.5)], startPoint: .init(x: 0.5, y: 0.45), endPoint: .bottom)
                        Text("Autumn\ncollection").font(style.heading(28, .semibold)).foregroundColor(.white).padding(14)
                    }
                    .frame(height: 184)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(FurnitureCategory.allCases, id: \.self) { c in chip(c) }
                        }
                    }
                    LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 16) {
                        ForEach(furnitureFilter(category)) { p in tile(p) }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, cart.isEmpty ? 16 : 88)
            }
            if !cart.isEmpty {
                Button { screen = "cart" } label: {
                    HStack {
                        Text("Cart · \(cart.count)")
                        Spacer()
                        Text(formatEur(cart.totalMinor))
                    }
                    .font(style.body(15, .semibold))
                    .foregroundColor(style.onPrimary)
                    .padding(.horizontal, 18)
                    .frame(minHeight: 56)
                    .background(style.primary)
                    .clipShape(RoundedRectangle(cornerRadius: style.radius))
                }
                .buttonStyle(.plain)
                .padding(16)
            }
        }
    }

    private func chip(_ c: FurnitureCategory) -> some View {
        let on = c == category
        return Button { category = c } label: {
            Text(c.label).font(style.body(13, .semibold))
                .foregroundColor(on ? style.onPrimary : style.text)
                .padding(.horizontal, 16).frame(minHeight: 40)
                .background(Capsule().fill(on ? style.primary : .clear))
                .overlay(Capsule().strokeBorder(on ? style.primary : style.border))
        }
        .buttonStyle(.plain)
    }

    private func tile(_ p: Product) -> some View {
        Button { withAnimation(Self.flight) { screen = p.id } } label: {
            VStack(alignment: .leading, spacing: 2) {
                Color.clear
                    .overlay(Image(p.image ?? "").resizable().scaledToFill().allowsHitTesting(false))
                    .clipShape(RoundedRectangle(cornerRadius: style.radius))
                    .matchedGeometryEffect(id: p.id, in: photos)
                    .aspectRatio(1, contentMode: .fit)
                Text(p.name).font(style.body(14, .semibold)).foregroundColor(style.text).padding(.top, 6)
                Text(formatEur(p.priceMinor)).font(style.body(13)).foregroundColor(style.muted)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("product_\(p.id)")
    }

    // MARK: Product

    private func product(_ p: Product) -> some View {
        // Read outside the full-bleed photo: inside it the top inset is already ignored.
        GeometryReader { geo in
            productPage(p, topInset: geo.safeAreaInsets.top)
        }
    }

    private func productPage(_ p: Product, topInset: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .topLeading) {
                Color.clear.overlay(Image(p.image ?? "").resizable().scaledToFill().allowsHitTesting(false)).clipped()
                    .matchedGeometryEffect(id: p.id, in: photos)
                Button { withAnimation(Self.flight) { screen = nil } } label: {
                    Image(systemName: "chevron.left").font(.system(size: 17, weight: .semibold)).foregroundColor(style.text)
                        .frame(width: 44, height: 44).background(Circle().fill(.white.opacity(0.92)))
                }
                .accessibilityLabel(Text("Back"))
                // Below the status bar, not under the clock.
                .padding(.leading, 12)
                .padding(.top, topInset + 8)
                .transition(.opacity)
            }
            .frame(maxHeight: UIScreen.main.bounds.height * 0.46 + topInset)
            VStack(alignment: .leading, spacing: 6) {
                Text(p.name).font(style.heading(34, .semibold)).foregroundColor(style.text)
                if let s = p.subtitle { Text(s).font(style.body(14)).foregroundColor(style.muted) }
                Text("Colour").font(style.body(13, .semibold)).foregroundColor(style.text).padding(.top, 10)
                HStack(spacing: 10) {
                    ForEach(Array([Color(argb: 0xFF1C_1917), Color(argb: 0xFFE7_E5E4), style.accent].enumerated()), id: \.offset) { i, c in
                        Circle().fill(c).padding(4).frame(width: 28, height: 28)
                            .overlay(Circle().strokeBorder(i == 0 ? style.text : style.border, lineWidth: i == 0 ? 2 : 1))
                    }
                }
                Text("Free delivery in 3–5 days. 30 days to return.").font(style.body(14)).foregroundColor(style.muted).padding(.top, 10)
                Spacer()
                PayButton(cover: .furniture, label: String(localized: "Add to cart · \(formatEur(p.priceMinor))", bundle: AppLanguage.bundle), busy: false, enabled: true) {
                    store.add(.furniture, p)
                    screen = nil
                }
                .accessibilityIdentifier("add_to_cart")
            }
            .padding(20)
        }
        .ignoresSafeArea(edges: .top)
    }

    // MARK: Cart

    private func cartScreen(_ cart: Cart) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                backButton { screen = nil }
                Text("Cart").font(style.heading(28)).foregroundColor(style.text)
            }
            ScrollView {
                VStack(spacing: 0) {
                    ForEach(cart.lines, id: \.product.id) { line in
                        HStack(spacing: 12) {
                            Image(line.product.image ?? "").resizable().scaledToFill().allowsHitTesting(false).frame(width: 64, height: 64)
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                            VStack(alignment: .leading) {
                                Text(line.product.name).font(style.body(14, .semibold)).foregroundColor(style.text)
                                if let s = line.product.subtitle { Text(s).font(style.body(12)).foregroundColor(style.muted).lineLimit(1) }
                            }
                            Spacer()
                            stepper(line)
                        }
                        .padding(.vertical, 10)
                        Divider()
                    }
                    VStack(spacing: 6) {
                        sum(String(localized: "Items", bundle: AppLanguage.bundle), formatEur(cart.totalMinor), bold: false)
                        sum(String(localized: "Delivery", bundle: AppLanguage.bundle), String(localized: "Free", bundle: AppLanguage.bundle), bold: false)
                        sum(String(localized: "Total", bundle: AppLanguage.bundle), formatEur(cart.totalMinor), bold: true)
                    }
                    .padding(.vertical, 14)
                }
            }
            StoreCheckoutBar(cover: .furniture, amountMinor: cart.totalMinor,
                             onOutcome: { actions.outcome($0, cart.totalMinor) }, onOpenSettings: actions.openSettings)
                .padding(.vertical, 12)
        }
        .padding(.horizontal, 16)
    }

    private func stepper(_ line: CartLine) -> some View {
        HStack(spacing: 0) {
            Button { store.setQuantity(.furniture, line.product.id, line.quantity - 1) } label: {
                Image(systemName: "minus").frame(width: 40, height: 40)
            }
            .accessibilityLabel(Text("Less"))
            Text("\(line.quantity)").font(style.body(14, .semibold))
            Button { store.setQuantity(.furniture, line.product.id, line.quantity + 1) } label: {
                Image(systemName: "plus").frame(width: 40, height: 40)
            }
            .accessibilityLabel(Text("More"))
        }
        .foregroundColor(style.text)
        .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(style.border))
    }

    private func sum(_ label: String, _ value: String, bold: Bool) -> some View {
        HStack {
            Text(label)
            Spacer()
            Text(value)
        }
        .font(style.body(bold ? 16 : 14, bold ? .bold : .regular))
        .foregroundColor(bold ? style.text : style.muted)
    }

    private func backButton(_ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: "chevron.left").font(.system(size: 18, weight: .semibold)).foregroundColor(style.text)
                .frame(width: 44, height: 44)
        }
        .accessibilityLabel(Text("Back"))
    }
}
