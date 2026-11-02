import SwiftUI

/// First screen: the SDK Payment stores, SDK Core's own card form, and the developer settings.
struct StartView: View {
    let onCover: (CoverId) -> Void
    let onSettings: () -> Void

    // The developer settings: AFT orders swap the stores for Northwind Invest.
    @AppStorage("store_api_version") private var apiVersion = ApiVersion.v1
    @AppStorage("store_order_type") private var orderType = OrderType.purchase

    var body: some View {
        // At least a screen tall: the store tiles share what the rest leaves, so SDK Core sits at the bottom.
        // On a short screen the tiles keep their minimum height and the page scrolls instead.
        GeometryReader { screen in
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    HStack {
                        Text("Payment demo")
                            .font(.custom("Inter", size: 28).weight(.heavy))
                            .foregroundColor(StartColors.text)
                        Spacer()
                        NavigationLink(value: SampleRoute.developer) {
                            Image(systemName: "gearshape")
                                .font(.system(size: 20, weight: .medium))
                                .foregroundColor(StartColors.text)
                                .frame(width: 44, height: 44)
                        }
                        .accessibilityLabel(Text("Developer settings"))
                        .accessibilityIdentifier("start_settings")
                    }
                    .padding(.top, 8)
                    if isAftDemo(apiVersion: apiVersion, orderType: orderType) {
                        // AFT orders top up an account: shown in a business where that is what happens, not in the stores.
                        section("SDK Payment", "AFT: top up an account from a card")
                        investCard
                    } else {
                        section("SDK Payment", "A ready payment sheet in your app — every store has all scenarios")
                        stores
                    }
                    section("SDK Core", "Card encryption for your own form (PCI DSS)")
                    coreCard
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
                .frame(minHeight: screen.size.height)
            }
        }
        .background(StartColors.background.ignoresSafeArea())
        // No preferredColorScheme here: on the root it would override the dark coffee shop pushed on top.
        // iOS 26 keeps an empty navigation bar over the root that swallows taps; the screen has its own header.
        .toolbar(.hidden, for: .navigationBar)
    }

    private var stores: some View {
        VStack(spacing: 10) {
            card(.furniture, image: "furniture_hero", tagline: String(localized: "Furniture · catalogue and cart", bundle: AppLanguage.bundle))
            card(.coffee, image: "coffee_hero", tagline: String(localized: "Coffee shop · order ahead", bundle: AppLanguage.bundle))
            card(.tickets, image: "tickets_hero", tagline: String(localized: "Tickets · concerts and shows", bundle: AppLanguage.bundle))
        }
    }

    /// Northwind Invest's tile: no photo, its own colours and a rising line.
    private var investCard: some View {
        Button { onCover(.invest) } label: {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 8) {
                    Image(systemName: "chart.line.uptrend.xyaxis").foregroundColor(NW.accent)
                    Text(verbatim: "Northwind").font(NW.font(15, .semibold)).foregroundColor(NW.text)
                    Spacer()
                    Text(verbatim: "AFT").font(NW.font(12, .semibold)).foregroundColor(NW.accent)
                        .padding(.horizontal, 10).padding(.vertical, 3)
                        .background(NW.accent.opacity(0.18)).clipShape(Capsule())
                }
                PortfolioChart(period: .all).padding(.vertical, 12)
                Text(verbatim: coverName(.invest)).font(NW.font(24, .bold)).foregroundColor(NW.text)
                Text("Top up your investment account from a card and watch the balance grow")
                    .font(NW.font(13)).foregroundColor(Color(argb: 0xFFCB_D5E1))
            }
            .padding(18)
            .frame(maxWidth: .infinity, minHeight: 224, maxHeight: .infinity, alignment: .topLeading)
            .background(RadialGradient(colors: [Color(argb: 0xFF16_6534), NW.surface], center: .topTrailing, startRadius: 0, endRadius: 420))
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .contentShape(RoundedRectangle(cornerRadius: 20))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("cover_invest")
    }

    /// Heading of one SDK.
    private func section(_ title: String, _ subtitle: LocalizedStringKey) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(verbatim: title).font(.custom("Inter", size: 17).weight(.heavy)).foregroundColor(StartColors.text)
            Text(subtitle).font(.custom("Inter", size: 13)).foregroundColor(StartColors.muted)
        }
        .padding(.top, 20)
        .padding(.bottom, 10)
    }

    /// SDK Core: a photo tile like the stores'.
    private var coreCard: some View {
        NavigationLink(value: SampleRoute.core) {
            photoTile(image: "core_hero", name: Text("Your own card form").font(.custom("Inter", size: 24).weight(.heavy)),
                      tagline: String(localized: "Card data turns into a token for your server", bundle: AppLanguage.bundle))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("start_core")
    }

    private func card(_ cover: CoverId, image: String, tagline: String) -> some View {
        Button { onCover(cover) } label: {
            photoTile(image: image, name: Text(coverName(cover)).font(coverStyle(cover).heading(24, .bold)), tagline: tagline, stretch: true)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("cover_\(cover.rawValue)")
    }

    /// Photo with a dark fade at the bottom, the name and a tagline over it.
    /// With [stretch] the tile takes its share of the free height, at least 112 points.
    private func photoTile(image: String, name: Text, tagline: String, stretch: Bool = false) -> some View {
        ZStack(alignment: .bottomLeading) {
            // The photo fills whatever the tile gets without deciding its size.
            Color.clear.overlay(Image(image).resizable().scaledToFill().allowsHitTesting(false)).clipped()
            LinearGradient(colors: [.clear, .black.opacity(0.68)], startPoint: .init(x: 0.5, y: 0.3), endPoint: .bottom)
            VStack(alignment: .leading, spacing: 2) {
                name.foregroundColor(.white)
                Text(tagline).font(.custom("Inter", size: 13)).foregroundColor(.white.opacity(0.92))
            }
            .padding(16)
        }
        .frame(minHeight: 112, maxHeight: stretch ? .infinity : 112)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        // A filled image reports its unclipped size: keep the tap area to the card.
        .contentShape(RoundedRectangle(cornerRadius: 20))
    }
}

/// Neutral colours of the start and developer screens: they belong to the demo, not to a store.
enum StartColors {
    static let background = Color(argb: 0xFFF7_F7F5)
    static let text = Color(argb: 0xFF11_1827)
    static let muted = Color(argb: 0xFF4B_5563)
}
