import SwiftUI
import sdk_payment_scanner

/// Northwind Invest, the AFT demo: portfolio → top-up amount → SDK sheet ("Transfer") → done. The transfer is
/// registered with `registerP2P.do` and the investor's details; the account is credited only on a completed transfer
/// and kept on the device.
struct InvestStoreView: View {
    let back: () -> Void
    let openSettings: () -> Void

    private enum Screen { case portfolio, topUp, done }

    @StateObject private var model = InvestModel()
    @StateObject private var checkout: StoreCheckout
    @State private var box: PresentBox
    @State private var screen = Screen.portfolio
    @State private var scenario = StorePrefs().scenario
    @State private var panelOpen = false
    /// The amount of the transfer in the sheet.
    @State private var sentMinor: Int64 = 0
    @State private var now = Date()

    init(back: @escaping () -> Void, openSettings: @escaping () -> Void) {
        self.back = back
        self.openSettings = openSettings
        let box = PresentBox()
        _box = State(initialValue: box)
        _checkout = StateObject(wrappedValue: StoreCheckout(
            registrar: PrefsRegistrar(prefill: NorthwindProfile.prefill),
            present: { box.present($0, $1) },
            onMdOrder: { StorePrefs().lastMdOrder = $0 }
        ))
    }

    var body: some View {
        ZStack {
            NW.background.ignoresSafeArea()
            switch screen {
            case .portfolio:
                PortfolioView(account: model.account, now: now, onBack: back, onTopUp: { screen = .topUp },
                              onInvest: { model.invest(model.account.freeCashMinor) })
            case .topUp:
                TopUpView(model: model, checkout: checkout, onBack: toPortfolio, onContinue: pay) {
                    if case let .registrationFailed(reason) = checkout.state {
                        RegistrationErrorView(cover: .invest, reason: reason, onOpenSettings: openSettings)
                    }
                    ScenarioRow(cover: .invest, buyerWithCards: scenario.buyerWithCards) { panelOpen = true }
                }
            case .done:
                if case let .done(orderId, amountMinor, before, after, at) = model.status {
                    TopUpDoneView(orderId: orderId, amountMinor: amountMinor, freeCashBefore: before, freeCashAfter: after, at: at,
                                  canInvest: model.account.canInvest,
                                  onInvest: {
                                      model.invest(amountMinor)
                                      toPortfolio()
                                  },
                                  onPortfolio: toPortfolio)
                }
            }
        }
        .animation(.easeInOut(duration: 0.18), value: screen)
        .preferredColorScheme(.dark)
        .onAppear {
            model.reload()
            checkout.resetError()
            wirePresent()
        }
        .sheet(isPresented: $panelOpen) { ScenarioSheet(scenario: $scenario) }
        .onChange(of: scenario) { StorePrefs().scenario = $0 }
    }

    private func toPortfolio() {
        model.leaveTopUp()
        now = Date()
        screen = .portfolio
    }

    private func pay() {
        guard let minor = model.amount.minor else { return }
        sentMinor = minor
        wirePresent()
        checkout.pay(amountMinor: minor, clientId: scenario.buyerWithCards ? StorePrefs().clientId : nil)
    }

    /// Step 3 in Northwind's look, then step 4: credit on a completed transfer.
    private func wirePresent() {
        let checkout = checkout
        let scenario = scenario
        let model = model
        box.present = { mdOrder, sessionId in
            let prefs = StorePrefs()
            let config = buildSdkConfig(
                baseUrl: UserDefaults.standard.string(forKey: AppPrefsKeys.baseUrl) ?? AppPrefsDefaults.baseUrl,
                scenario: scenario,
                coverTheme: coverStyle(.invest).paymentTheme(),
                themeOverride: prefs.sheetTheme,
                locale: prefs.localeOverride,
                applePayMerchantId: UserDefaults.standard.string(forKey: AppPrefsKeys.apayMerchantId) ?? AppPrefsDefaults.apayMerchantId
            )
            presentCheckout(config: config, mdOrder: mdOrder, sessionId: sessionId) { result in
                checkout.sheetClosed()
                model.onOutcome(outcome(of: result), amountMinor: sentMinor)
                if case .done = model.status { screen = .done }
            }
        }
    }
}

/// Northwind's home: the portfolio, its free cash and the latest operations. After a top-up the amounts count up to
/// the new values and the free-cash card is outlined while the credit is fresh.
struct PortfolioView: View {
    let account: InvestAccount
    let now: Date
    let onBack: () -> Void
    let onTopUp: () -> Void
    let onInvest: () -> Void

    @State private var period = ChartPeriod.month

    private var justCredited: InvestOperation? {
        guard let first = account.operations.first, first.kind == .topUp,
              Int64(now.timeIntervalSince1970 * 1000) - first.atEpochMillis < 60_000 else { return nil }
        return first
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                header
                Text("Portfolio value").font(NW.font(13)).foregroundColor(NW.muted).padding(.top, 18)
                Text(eur(account.totalMinor))
                    .font(NW.font(36, .bold).monospacedDigit())
                    .foregroundColor(NW.text)
                    .contentTransition(.numericText())
                    .animation(.easeOut(duration: 0.6), value: account.totalMinor)
                HStack(spacing: 8) {
                    Text(verbatim: "▲ \(eur(14920)) · \(percent(0.032))")
                        .font(NW.font(13, .semibold).monospacedDigit())
                        .foregroundColor(NW.accent)
                        .padding(.horizontal, 10).padding(.vertical, 4)
                        .background(NW.accentSoft).clipShape(Capsule())
                    Text("this month").font(NW.font(13)).foregroundColor(NW.muted)
                }
                .padding(.top, 6)
                PortfolioChart(period: period).frame(height: 96).padding(.top, 10)
                periods.padding(.top, 6)
                HStack(spacing: 10) {
                    NWPrimaryButton(label: String(localized: "Top up", bundle: AppLanguage.bundle), icon: "plus", action: onTopUp)
                    NWOutlineButton(label: String(localized: "Invest", bundle: AppLanguage.bundle), icon: "chart.line.uptrend.xyaxis",
                                    enabled: account.canInvest, action: onInvest)
                }
                .padding(.top, 16)
                freeCash.padding(.top, 12)
                section(String(localized: "My assets", bundle: AppLanguage.bundle), trailing: "\(account.holdings.count)")
                NWCard {
                    ForEach(Array(account.holdings.enumerated()), id: \.offset) { index, holding in
                        assetRow(holding, divider: index > 0)
                    }
                }
                section(String(localized: "Recent operations", bundle: AppLanguage.bundle))
                NWCard {
                    ForEach(Array(account.recent().enumerated()), id: \.offset) { index, op in
                        operationRow(op, divider: index > 0)
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 24)
        }
    }

    private var header: some View {
        HStack(spacing: 12) {
            NWBackButton(action: onBack)
            Text(verbatim: NorthwindProfile.initials)
                .font(NW.font(14, .bold)).foregroundColor(NW.text)
                .frame(width: 40, height: 40)
                .background(LinearGradient(colors: [Color(argb: 0xFF16_6534), Color(argb: 0xFF0F_766E)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 0) {
                Text("Investment account •• \(NorthwindProfile.accountLast4)").font(NW.font(12)).foregroundColor(NW.muted)
                Text(verbatim: NorthwindProfile.name).font(NW.font(15, .semibold)).foregroundColor(NW.text)
            }
            Spacer()
            // Decorative: the demo has no notifications, so it is not a button.
            Image(systemName: "bell").foregroundColor(NW.muted).accessibilityHidden(true)
        }
        .padding(.top, 8)
    }

    private var periods: some View {
        HStack(spacing: 0) {
            ForEach(ChartPeriod.allCases, id: \.self) { p in
                Button { period = p } label: {
                    Text(p.label)
                        .font(NW.font(13, p == period ? .semibold : .medium))
                        .foregroundColor(p == period ? NW.text : NW.muted)
                        .frame(maxWidth: .infinity, minHeight: 36)
                        .background(p == period ? NW.surface2 : NW.surface)
                        .clipShape(RoundedRectangle(cornerRadius: 9))
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(p == period ? .isSelected : [])
            }
        }
        .padding(3)
        .background(NW.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(NW.divider, lineWidth: 1))
    }

    private var freeCash: some View {
        NWCard(highlight: justCredited != nil) {
            HStack(spacing: 12) {
                NWBadge(background: NW.accentSoft) { Image(systemName: "wallet.pass").foregroundColor(NW.accent) }
                VStack(alignment: .leading, spacing: 0) {
                    Text("Free cash").font(NW.font(13)).foregroundColor(NW.muted)
                    if let credited = justCredited {
                        Text("+\(eur(credited.amountMinor)) just now").font(NW.font(12).monospacedDigit()).foregroundColor(NW.accent)
                    } else {
                        Text("Ready to invest").font(NW.font(12)).foregroundColor(NW.muted)
                    }
                }
                Spacer()
                Text(eur(account.freeCashMinor))
                    .font(NW.font(18, .bold).monospacedDigit())
                    .foregroundColor(NW.text)
                    .contentTransition(.numericText())
                    .animation(.easeOut(duration: 0.6), value: account.freeCashMinor)
            }
        }
    }

    private func section(_ title: String, trailing: String? = nil) -> some View {
        HStack {
            Text(verbatim: title).font(NW.font(16, .bold)).foregroundColor(NW.text).accessibilityAddTraits(.isHeader)
            Spacer()
            if let trailing { Text(verbatim: trailing).font(NW.font(13)).foregroundColor(NW.muted) }
        }
        .padding(.top, 20)
        .padding(.bottom, 8)
    }

    private func assetRow(_ holding: Holding, divider: Bool) -> some View {
        listRow(divider: divider) {
            TickerBadge(ticker: holding.ticker)
            VStack(alignment: .leading, spacing: 0) {
                Text(verbatim: assetName(holding.ticker)).font(NW.font(15, .semibold)).foregroundColor(NW.text)
                Text("\(number(holding.quantity)) units · \(eur(holding.unitPriceMinor))")
                    .font(NW.font(13).monospacedDigit()).foregroundColor(NW.muted)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 0) {
                Text(eur(holding.valueMinor)).font(NW.font(15, .semibold).monospacedDigit()).foregroundColor(NW.text)
                Text(verbatim: "+" + percent(Double(holding.changeBasisPoints) / 10000))
                    .font(NW.font(13).monospacedDigit()).foregroundColor(NW.accent)
            }
        }
    }

    private func operationRow(_ op: InvestOperation, divider: Bool) -> some View {
        listRow(divider: divider) {
            switch op.kind {
            case .topUp: NWBadge(background: NW.accentSoft) { Image(systemName: "arrow.up").foregroundColor(NW.accent) }
            case .buy: TickerBadge(ticker: op.ticker ?? InvestAccount.globalEtf)
            }
            VStack(alignment: .leading, spacing: 0) {
                Group {
                    switch op.kind {
                    case .topUp: Text("Deposit")
                    case .buy: Text("Buy \(assetName(op.ticker ?? InvestAccount.globalEtf))")
                    }
                }
                .font(NW.font(15, .semibold)).foregroundColor(NW.text)
                Text(verbatim: operationTime(op.atEpochMillis)).font(NW.font(13)).foregroundColor(NW.muted)
            }
            Spacer()
            Text(verbatim: (op.kind == .topUp ? "+" : "−") + eur(op.amountMinor))
                .font(NW.font(15, .semibold).monospacedDigit())
                .foregroundColor(op.kind == .topUp ? NW.accent : NW.text)
        }
    }

    private func listRow<Content: View>(divider: Bool, @ViewBuilder content: () -> Content) -> some View {
        VStack(spacing: 0) {
            if divider { NW.divider.frame(height: 1) }
            HStack(spacing: 12) { content() }.padding(.vertical, 11)
        }
    }

    private func number(_ value: Double) -> String {
        value.formatted(.number.precision(.fractionLength(2)).locale(AppLanguage.locale))
    }

    private func percent(_ value: Double) -> String {
        value.formatted(.percent.precision(.fractionLength(1)).locale(AppLanguage.locale))
    }
}

/// The amount to top up the account with, typed on the system keyboard. "Continue" registers the transfer and the
/// SDK sheet takes it from there; `footer` is the demo panel row and a registration error, if any.
struct TopUpView<Footer: View>: View {
    @ObservedObject var model: InvestModel
    @ObservedObject var checkout: StoreCheckout
    let onBack: () -> Void
    let onContinue: () -> Void
    @ViewBuilder let footer: Footer

    @State private var field = ""
    @FocusState private var focused: Bool

    private var busy: Bool { checkout.state == .registering || checkout.state == .inSheet }
    private var separator: String { AppLanguage.locale.decimalSeparator ?? "." }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 0) {
                    ZStack {
                        HStack { NWBackButton(action: onBack); Spacer() }
                        Text("Top up").font(NW.font(17, .semibold)).foregroundColor(NW.text)
                    }
                    .padding(.top, 4)
                    banner
                    destination.padding(.top, 12)
                    Text("Amount").font(NW.font(13)).foregroundColor(NW.muted).padding(.top, 22)
                    amountField
                    Text("from \(eur(TopUpAmount.min)) to \(eur(TopUpAmount.max))")
                        .font(NW.font(13).monospacedDigit())
                        .foregroundColor(model.amount.valid || model.amount.text.isEmpty ? NW.muted : NW.error)
                    presets.padding(.top, 16)
                }
                .padding(.horizontal, 20)
            }
            .scrollDismissesKeyboard(.interactively)
            VStack(spacing: 10) {
                footer
                NWPrimaryButton(label: continueLabel, enabled: model.status.canContinue(model.amount), busy: busy, action: onContinue)
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 12)
        }
        .onAppear {
            field = shown(model.amount)
            focused = true
        }
        // A preset chip: show it.
        .onChange(of: model.amount) { amount in
            if TopUpAmount.parse(field) != amount { field = shown(amount) }
        }
    }

    private var continueLabel: String {
        if busy { return String(localized: "Creating transfer…", bundle: AppLanguage.bundle) }
        if case .declined = model.status { return String(localized: "Try again", bundle: AppLanguage.bundle) }
        let amount = model.amount.minor.map { eur($0) } ?? "—"
        return String(localized: "Continue · \(amount)", bundle: AppLanguage.bundle)
    }

    private func shown(_ amount: TopUpAmount) -> String { amount.shown(separator: separator) }

    private var amountField: some View {
        HStack(spacing: 0) {
            Text(verbatim: "€")
            TextField("0", text: $field)
                .keyboardType(.decimalPad)
                .focused($focused)
                .fixedSize()
                // What the amount kept, not what was typed: a third decimal, a leading zero or a second separator goes.
                .onChange(of: field) { input in
                    model.edit(input)
                    let kept = shown(TopUpAmount.parse(input))
                    if kept != input { field = kept }
                }
                .accessibilityIdentifier("invest_amount")
        }
        .font(NW.font(52, .bold).monospacedDigit())
        .foregroundColor(NW.text)
        .tint(NW.accent)
        .frame(maxWidth: .infinity)
        .contentShape(Rectangle())
        .onTapGesture { focused = true }
    }

    @ViewBuilder private var banner: some View {
        let texts: (String, String)? = {
            switch model.status {
            case .declined:
                return (String(localized: "Transfer failed", bundle: AppLanguage.bundle),
                        String(localized: "The card's bank declined the operation. No money was taken; try another card.", bundle: AppLanguage.bundle))
            case .unconfirmed:
                return (String(localized: "Status unknown", bundle: AppLanguage.bundle),
                        String(localized: "Check the order in the merchant portal before trying again.", bundle: AppLanguage.bundle))
            default: return nil
            }
        }()
        if let (title, body) = texts {
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: "exclamationmark.circle").foregroundColor(NW.error).accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    Text(verbatim: title).font(NW.font(15, .semibold)).foregroundColor(NW.text)
                    Text(verbatim: body).font(NW.font(13)).foregroundColor(NW.muted)
                    if case let .declined(message?) = model.status {
                        Text("Gateway: \(message)").font(NW.font(12)).foregroundColor(NW.muted).padding(.top, 4)
                    }
                }
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 14).padding(.vertical, 12)
            .background(NW.error.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(NW.error.opacity(0.45), lineWidth: 1))
            .padding(.top, 12)
        }
    }

    private var destination: some View {
        NWCard {
            HStack(spacing: 12) {
                NWBadge(background: NW.accentSoft) { Image(systemName: "chart.line.uptrend.xyaxis").foregroundColor(NW.accent) }
                VStack(alignment: .leading, spacing: 0) {
                    Text("To").font(NW.font(12)).foregroundColor(NW.muted)
                    Text("Investment account •• \(NorthwindProfile.accountLast4)").font(NW.font(15, .semibold)).foregroundColor(NW.text)
                        .lineLimit(1).minimumScaleFactor(0.75)
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 0) {
                    Text("Available").font(NW.font(12)).foregroundColor(NW.muted)
                    Text(eur(model.account.freeCashMinor)).font(NW.font(15, .semibold).monospacedDigit()).foregroundColor(NW.text)
                }
            }
        }
    }

    private var presets: some View {
        HStack(spacing: 8) {
            ForEach(TopUpAmount.presets, id: \.self) { preset in
                let on = model.amount.minor == preset
                Button { model.preset(preset) } label: {
                    Text(verbatim: eurWhole(preset))
                        .font(NW.font(14, .semibold).monospacedDigit())
                        .foregroundColor(on ? NW.accent : NW.text)
                        .frame(maxWidth: .infinity, minHeight: 44)
                        .background(on ? NW.accentSoft : .clear)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(on ? NW.accent : NW.fieldBorder, lineWidth: 1))
                }
                .buttonStyle(.plain)
            }
        }
    }
}

/// The transfer went through: what was credited, to where, and the order to find it by at the gateway.
struct TopUpDoneView: View {
    let orderId: String
    let amountMinor: Int64
    let freeCashBefore: Int64
    let freeCashAfter: Int64
    let at: Int64
    let canInvest: Bool
    let onInvest: () -> Void
    let onPortfolio: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 0) {
                    ZStack {
                        Circle().fill(NW.accentSoft).frame(width: 88, height: 88)
                        Circle().fill(NW.accent).frame(width: 60, height: 60)
                        Image(systemName: "checkmark").font(.system(size: 26, weight: .bold)).foregroundColor(NW.onAccent)
                    }
                    .padding(.top, 64)
                    .accessibilityHidden(true)
                    Text(verbatim: "+" + eur(amountMinor)).font(NW.font(40, .bold).monospacedDigit()).foregroundColor(NW.text).padding(.top, 22)
                    Text("credited to investment account •• \(NorthwindProfile.accountLast4)")
                        .font(NW.font(15)).foregroundColor(NW.muted).multilineTextAlignment(.center)
                    NWCard {
                        DetailRow(label: String(localized: "Operation", bundle: AppLanguage.bundle), divider: false) {
                            Text("AFT · to own account").font(NW.font(14)).foregroundColor(NW.text)
                        }
                        DetailRow(label: String(localized: "Free cash", bundle: AppLanguage.bundle)) {
                            Text(verbatim: "\(eur(freeCashBefore)) → \(eur(freeCashAfter))")
                                .font(NW.font(14, .semibold).monospacedDigit()).foregroundColor(NW.accent)
                        }
                        DetailRow(label: String(localized: "Order", bundle: AppLanguage.bundle)) {
                            Text(verbatim: middleEllipsis(orderId)).font(NW.font(14).monospacedDigit()).foregroundColor(NW.text).lineLimit(1)
                        }
                        DetailRow(label: String(localized: "Time", bundle: AppLanguage.bundle)) {
                            Text(verbatim: operationTime(at)).font(NW.font(14)).foregroundColor(NW.text)
                        }
                    }
                    .padding(.top, 28)
                }
                .padding(.horizontal, 20)
            }
            VStack(spacing: 10) {
                NWPrimaryButton(label: String(localized: "Invest \(eur(amountMinor))", bundle: AppLanguage.bundle), enabled: canInvest, action: onInvest)
                NWOutlineButton(label: String(localized: "To portfolio", bundle: AppLanguage.bundle), action: onPortfolio)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
    }
}

/// "2cd7e4de…2ef4095f": both ends of an order id, which is what people compare.
func middleEllipsis(_ id: String) -> String { id.count <= 20 ? id : id.prefix(8) + "…" + id.suffix(8) }
