import Combine
import SwiftUI

/// Stage Pass: the event with seat categories → checkout with a seat hold → the shared payment bar.
struct TicketsStoreView: View {
    let actions: CoverActions
    @EnvironmentObject private var store: StoreModel
    @State private var order = TicketOrder()
    @State private var checkout = false
    @State private var holdExpired = false

    private let style = coverStyle(.tickets)

    var body: some View {
        // After a paid order the cart is empty: the buyer is back at the event.
        let showCheckout = checkout && !store.cart(.tickets).isEmpty
        Group {
            if showCheckout {
                TicketsCheckoutView(order: order, actions: actions, onBack: { checkout = false }) {
                    holdExpired = true
                    checkout = false
                }
            } else {
                event
            }
        }
        .background(style.background.ignoresSafeArea())
    }

    private var event: some View {
        let seats = order.toCart().lines[0]
        return VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    poster
                    if holdExpired {
                        Text("The hold ran out. Pick your seats again.")
                            .font(style.body(14, .semibold)).foregroundColor(Color(argb: 0xFFC2_410C)).padding(.horizontal, 16)
                    }
                    Text("Tickets").font(style.heading(18, .heavy)).foregroundColor(style.text).padding(.horizontal, 16).padding(.top, 6)
                    ForEach(Catalog.products(.tickets).filter { $0.id != ticketFeeId }) { category in
                        categoryRow(category, selected: category.id == order.categoryId)
                    }
                }
            }
            // The poster runs under the status bar.
            .ignoresSafeArea(edges: .top)
            HStack(spacing: 4) {
                Button { order = order.withQuantity(order.quantity - 1) } label: { Image(systemName: "minus").frame(width: 44, height: 44) }
                    .accessibilityLabel(Text("Less"))
                Text("\(order.quantity)").font(style.body(16, .heavy))
                Button { order = order.withQuantity(order.quantity + 1) } label: { Image(systemName: "plus").frame(width: 44, height: 44) }
                    .accessibilityLabel(Text("More"))
                Spacer()
                Button {
                    holdExpired = false
                    store.replaceCart(.tickets, order.toCart())
                    checkout = true
                } label: {
                    Text("Checkout · \(formatEur(seats.product.priceMinor * Int64(seats.quantity)))")
                        .font(style.body(16, .heavy)).frame(minHeight: 56).padding(.horizontal, 12)
                }
                .accessibilityIdentifier("tickets_next")
            }
            .foregroundColor(style.onPrimary)
            .padding(.leading, 6)
            .background(style.primary)
            .clipShape(RoundedRectangle(cornerRadius: style.radius))
            .padding(16)
        }
        // The poster runs under the status bar: light icons on the dark photo.
        .preferredColorScheme(.dark)
    }

    private var poster: some View {
        ZStack(alignment: .bottomLeading) {
            // Fill the width without letting the photo widen the screen.
            Color.clear.frame(maxWidth: .infinity).frame(height: 340)
                .overlay(Image("tickets_hero").resizable().scaledToFill().allowsHitTesting(false))
                .clipped()
            LinearGradient(colors: [.black.opacity(0.25), .black.opacity(0.8)], startPoint: .top, endPoint: .bottom)
            VStack(alignment: .leading, spacing: 0) {
                Text(verbatim: "Live in Hall A").font(.custom("Playfair Display", size: 18).italic().weight(.semibold))
                Text(verbatim: "NORTHERN\nLIGHTS").font(style.heading(42, .heavy)).lineSpacing(-6)
                Text("Sat, 14 Nov · 20:00 · Hall A").font(style.body(14)).opacity(0.9).padding(.top, 6)
            }
            .foregroundColor(.white)
            .padding(16)
            VStack {
                HStack {
                    Button(action: actions.back) {
                        Image(systemName: "chevron.left").font(.system(size: 17, weight: .semibold)).foregroundColor(.white)
                            .frame(width: 44, height: 44).background(Circle().fill(.black.opacity(0.35)))
                    }
                    .accessibilityLabel(Text("Back"))
                    Spacer()
                }
                Spacer()
            }
            .padding(.top, 54)
            .padding(.leading, 12)
        }
        .frame(height: 340)
        .environment(\.colorScheme, .light)
    }

    private func categoryRow(_ category: Product, selected: Bool) -> some View {
        Button { order.categoryId = category.id } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(category.name).font(style.body(16, .semibold)).foregroundColor(style.text)
                    if let s = category.subtitle { Text(s).font(style.body(13)).foregroundColor(style.muted) }
                }
                Spacer()
                Text(formatEur(category.priceMinor)).font(style.body(16, .bold)).foregroundColor(style.text)
            }
            .padding(.horizontal, 16).padding(.vertical, 14)
            .background(RoundedRectangle(cornerRadius: style.radius).fill(selected ? style.surface : style.background))
            .overlay(RoundedRectangle(cornerRadius: style.radius).strokeBorder(selected ? style.primary : style.border, lineWidth: selected ? 2 : 1))
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 16)
        .environment(\.colorScheme, .light)
    }
}

/// Checkout: the event, the bill, a seat hold that runs out, and the shared payment bar.
struct TicketsCheckoutView: View {
    let order: TicketOrder
    let actions: CoverActions
    let onBack: () -> Void
    let onHoldExpired: () -> Void
    @State private var secondsLeft = 600

    private let style = coverStyle(.tickets)
    private let tick = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        let cart = order.toCart()
        let seats = cart.lines[0], fee = cart.lines[1]
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left").font(.system(size: 18, weight: .semibold)).frame(width: 44, height: 44)
                }
                .accessibilityLabel(Text("Back"))
                Text("Checkout").font(style.heading(24, .heavy))
            }
            .foregroundColor(style.text)
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack(spacing: 12) {
                        Image("tickets_crowd").resizable().scaledToFill().allowsHitTesting(false).frame(width: 96, height: 96).clipped()
                        VStack(alignment: .leading, spacing: 2) {
                            Text(verbatim: "NORTHERN LIGHTS").font(style.heading(15, .heavy)).foregroundColor(style.text)
                            Text("14 Nov · \(seats.product.name)").font(style.body(13)).foregroundColor(style.muted)
                            Text(verbatim: "\(seats.quantity) × \(formatEur(seats.product.priceMinor))").font(style.body(13)).foregroundColor(style.muted)
                        }
                        Spacer()
                    }
                    .clipShape(RoundedRectangle(cornerRadius: style.radius))
                    .overlay(RoundedRectangle(cornerRadius: style.radius).strokeBorder(style.border))
                    VStack(spacing: 6) {
                        line(String(localized: "Tickets", bundle: AppLanguage.bundle), formatEur(seats.product.priceMinor * Int64(seats.quantity)), bold: false)
                        line(fee.product.name, formatEur(fee.product.priceMinor), bold: false)
                        line(String(localized: "Total", bundle: AppLanguage.bundle), formatEur(cart.totalMinor), bold: true)
                    }
                    Label {
                        Text("Seats held for \(String(format: "%d:%02d", secondsLeft / 60, secondsLeft % 60))")
                    } icon: {
                        Image(systemName: "clock")
                    }
                    .font(style.body(14, .semibold))
                    .foregroundColor(Color(argb: 0xFFC2_410C))
                }
                .padding(.top, 8)
            }
            StoreCheckoutBar(cover: .tickets, amountMinor: cart.totalMinor,
                             onOutcome: { actions.outcome($0, cart.totalMinor) }, onOpenSettings: actions.openSettings)
                .padding(.vertical, 12)
        }
        .padding(.horizontal, 16)
        .preferredColorScheme(.light)
        .onReceive(tick) { _ in
            if secondsLeft > 0 { secondsLeft -= 1 } else { onHoldExpired() }
        }
    }

    private func line(_ label: String, _ value: String, bold: Bool) -> some View {
        HStack {
            Text(label)
            Spacer()
            Text(value)
        }
        .font(style.body(bold ? 17 : 14, bold ? .heavy : .regular))
        .foregroundColor(bold ? style.text : style.muted)
    }
}
