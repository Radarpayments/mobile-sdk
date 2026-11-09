import SwiftUI
import sdk_payment_scanner

/// "Your server" with the merchant API and credentials currently chosen in the developer settings.
struct PrefsRegistrar: OrderRegistrar {
    /// The investor's details a Northwind transfer is registered with; nil for the stores.
    var prefill: TransferPrefill?

    func register(amountMinor: Int64, clientId: String?) async -> OrderRegistration {
        let prefs = StorePrefs()
        let baseUrl = UserDefaults.standard.string(forKey: AppPrefsKeys.baseUrl) ?? AppPrefsDefaults.baseUrl
        return await merchantRegistrar(
            apiVersion: prefs.apiVersion,
            baseUrl: baseUrl,
            login: prefs.merchantLogin,
            password: prefs.merchantPassword,
            apiKey: prefs.apiKey,
            orderType: prefs.orderType,
            prefill: prefill
        ).register(amountMinor: amountMinor, clientId: clientId)
    }
}

/// Holds what the sheet needs when it opens: the latest cover and result handler.
@MainActor
final class PresentBox {
    var present: (_ mdOrder: String, _ sessionId: String?) -> Void = { _, _ in }
}

/// The bottom of every cart in every cover: the demo-panel row, the "Pay" button and a registration
/// error, if any. Registers the order for `amountMinor` and opens the SDK sheet in the cover's brand;
/// with `autoPay` it does so as soon as it is shown.
struct StoreCheckoutBar: View {
    let cover: CoverId
    let amountMinor: Int64
    var autoPay: Binding<Bool> = .constant(false)
    let onOutcome: (CheckoutOutcome) -> Void
    let onOpenSettings: () -> Void

    @State private var box: PresentBox
    @StateObject private var checkout: StoreCheckout
    @State private var scenario = StorePrefs().scenario
    @State private var panelOpen = false

    init(cover: CoverId, amountMinor: Int64, autoPay: Binding<Bool> = .constant(false),
         onOutcome: @escaping (CheckoutOutcome) -> Void, onOpenSettings: @escaping () -> Void) {
        self.cover = cover
        self.amountMinor = amountMinor
        self.autoPay = autoPay
        self.onOutcome = onOutcome
        self.onOpenSettings = onOpenSettings
        let box = PresentBox()
        _box = State(initialValue: box)
        _checkout = StateObject(wrappedValue: StoreCheckout(
            registrar: PrefsRegistrar(),
            present: { box.present($0, $1) },
            onMdOrder: { StorePrefs().lastMdOrder = $0 }
        ))
    }

    var body: some View {
        let style = coverStyle(cover)
        VStack(spacing: 10) {
            if case let .registrationFailed(reason) = checkout.state {
                RegistrationErrorView(cover: cover, reason: reason, onOpenSettings: onOpenSettings)
            }
            ScenarioRow(cover: cover, buyerWithCards: scenario.buyerWithCards) { panelOpen = true }
            PayButton(
                cover: cover,
                label: String(localized: "Pay \(formatEur(amountMinor))", bundle: AppLanguage.bundle),
                busy: checkout.state == .registering || checkout.state == .inSheet,
                enabled: amountMinor > 0
            ) { pay() }
        }
        .onAppear {
            // Back from the settings: the old "not set" error no longer applies.
            checkout.resetError()
            wirePresent(style: style)
            if autoPay.wrappedValue, amountMinor > 0 {
                autoPay.wrappedValue = false
                pay()
            }
        }
        .sheet(isPresented: $panelOpen) {
            ScenarioSheet(scenario: $scenario)
        }
        .onChange(of: scenario) { StorePrefs().scenario = $0 }
    }

    private func pay() {
        let prefs = StorePrefs()
        wirePresent(style: coverStyle(cover))
        checkout.pay(amountMinor: amountMinor, clientId: scenario.buyerWithCards ? prefs.clientId : nil)
    }

    /// Step 3 with the cover's look and the current settings, then step 4.
    private func wirePresent(style: CoverStyle) {
        let checkout = checkout
        let scenario = scenario
        let onOutcome = onOutcome
        box.present = { mdOrder, sessionId in
            let prefs = StorePrefs()
            let config = buildSdkConfig(
                baseUrl: UserDefaults.standard.string(forKey: AppPrefsKeys.baseUrl) ?? AppPrefsDefaults.baseUrl,
                scenario: scenario,
                coverTheme: style.paymentTheme(),
                themeOverride: prefs.sheetTheme,
                locale: prefs.localeOverride,
                applePayMerchantId: UserDefaults.standard.string(forKey: AppPrefsKeys.apayMerchantId) ?? AppPrefsDefaults.apayMerchantId
            )
            presentCheckout(config: config, mdOrder: mdOrder, sessionId: sessionId) { result in
                checkout.sheetClosed()
                onOutcome(outcome(of: result))
            }
        }
    }
}

/// Dashed row above the "Pay" button that opens the demo panel.
struct ScenarioRow: View {
    let cover: CoverId
    let buyerWithCards: Bool
    let onTap: () -> Void

    var body: some View {
        let style = coverStyle(cover)
        let buyer = buyerWithCards ? String(localized: "With cards", bundle: AppLanguage.bundle) : String(localized: "Guest", bundle: AppLanguage.bundle)
        Button(action: onTap) {
            HStack {
                Text("Scenario: \(buyer)").font(style.body(14, .medium)).foregroundColor(style.muted)
                Spacer()
                Image(systemName: "chevron.down").font(.system(size: 13, weight: .semibold)).foregroundColor(style.muted)
            }
            .padding(.horizontal, 14)
            .frame(minHeight: 48)
            .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(style.border, style: StrokeStyle(lineWidth: 1, dash: [5, 4])))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("store_scenario")
    }
}

/// The store's own "Pay" button in the cover's brand; shows progress while the order is registered.
struct PayButton: View {
    let cover: CoverId
    let label: String
    let busy: Bool
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        let style = coverStyle(cover)
        Button(action: action) {
            Group {
                if busy {
                    ProgressView().tint(style.onPrimary)
                } else {
                    Text(label).font(style.body(16, .bold))
                }
            }
            .foregroundColor(style.onPrimary)
            .frame(maxWidth: .infinity, minHeight: 52)
            .background(style.primary.opacity(enabled || busy ? 1 : 0.4))
            .clipShape(style.buttonShape)
        }
        .buttonStyle(.plain)
        .disabled(!enabled || busy)
        .accessibilityIdentifier("store_pay")
    }
}

/// Why the order was not created, with a way to the developer settings.
struct RegistrationErrorView: View {
    let cover: CoverId
    let reason: OrderRegistration
    let onOpenSettings: () -> Void

    var body: some View {
        let style = coverStyle(cover)
        VStack(alignment: .leading, spacing: 4) {
            switch reason {
            case let .failed(message): Text("The order was not created: \(message)")
            case .missingApiKey: Text("The API key is not set.")
            default: Text("Merchant login and password are not set.")
            }
            Button("Open settings", action: onOpenSettings)
                .font(style.body(14, .bold))
                .foregroundColor(style.dark ? style.primary : style.accent)
                .frame(minHeight: 44)
        }
        .font(style.body(14))
        .foregroundColor(style.text)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.top, 10)
        .background(style.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

func coverName(_ cover: CoverId) -> String {
    switch cover {
    case .furniture: return "Hearth & Co"
    case .coffee: return "Roast Lane"
    case .tickets: return "Stage Pass"
    case .invest: return "Northwind Invest"
    }
}
