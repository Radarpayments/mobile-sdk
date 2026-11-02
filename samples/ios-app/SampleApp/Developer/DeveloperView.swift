import SwiftUI
import UIKit

/// Settings for whoever runs the demo: the gateway and merchant credentials used to register orders,
/// the SDK sheet look, Apple Pay, and the Advanced demo.
struct DeveloperView: View {
    // The same UserDefaults keys StorePrefs reads; the credentials go to the Keychain through StorePrefs.
    @AppStorage(AppPrefsKeys.baseUrl) private var baseUrl = AppPrefsDefaults.baseUrl
    @AppStorage("store_api_version") private var apiVersion = ApiVersion.v1
    @AppStorage("store_order_type") private var orderType = OrderType.purchase
    @State private var login = StorePrefs().merchantLogin
    @State private var password = StorePrefs().merchantPassword
    @State private var apiKey = StorePrefs().apiKey
    @AppStorage("store_sheet_theme") private var sheetTheme = SheetThemeOverride.cover.rawValue
    @AppStorage(AppLanguage.key) private var locale = ""
    @AppStorage(AppPrefsKeys.apayMerchantId) private var applePayMerchantId = AppPrefsDefaults.apayMerchantId
    @State private var lastMdOrder = StorePrefs().lastMdOrder

    var body: some View {
        Form {
            Section {
                TextField("Base URL", text: $baseUrl).keyboardType(.URL).textInputAutocapitalization(.never).autocorrectionDisabled()
                apiSection
            } header: {
                Text("Gateway")
            } footer: {
                Text("Stored only on this device. In your app the order is registered by your backend.")
            }
            Section("SDK appearance") {
                Picker("Sheet theme", selection: $sheetTheme) {
                    Text("As the store").tag(SheetThemeOverride.cover.rawValue)
                    Text("Light").tag(SheetThemeOverride.light.rawValue)
                    Text("Dark").tag(SheetThemeOverride.dark.rawValue)
                    Text("System").tag(SheetThemeOverride.system.rawValue)
                }
                Picker("Language", selection: $locale) {
                    Text("System").tag("")
                    ForEach(AppLanguage.all, id: \.code) { language in
                        Text(verbatim: language.name).tag(language.code)
                    }
                }
            }
            Section("Apple Pay") {
                TextField("Merchant ID", text: $applePayMerchantId).textInputAutocapitalization(.never).autocorrectionDisabled()
            }
            Section("More") {
                NavigationLink(value: SampleRoute.paymentAdvanced) { link("Advanced", "State stream, SSL pinning") }
                HStack {
                    VStack(alignment: .leading) {
                        Text("Last order")
                        Text(verbatim: lastMdOrder ?? "—").font(.system(size: 12, design: .monospaced)).foregroundColor(.secondary)
                    }
                    Spacer()
                    if let md = lastMdOrder {
                        Button("Copy") { UIPasteboard.general.string = md }
                    }
                }
            }
        }
        .navigationTitle("Developer settings")
        .onAppear { lastMdOrder = StorePrefs().lastMdOrder }
        .onChange(of: login) { StorePrefs().merchantLogin = $0 }
        .onChange(of: password) { StorePrefs().merchantPassword = $0 }
        .onChange(of: apiKey) { StorePrefs().apiKey = $0 }
        .onChange(of: locale) { if $0.isEmpty { UserDefaults.standard.removeObject(forKey: AppLanguage.key) } }
        .preferredColorScheme(.light)
    }

    /// API V1 (login and password) or API V2 (API key): how the demo registers its orders.
    @ViewBuilder
    private var apiSection: some View {
        Picker("Merchant API", selection: $apiVersion) {
            Text(verbatim: "API V1").tag(ApiVersion.v1)
            Text(verbatim: "API V2").tag(ApiVersion.v2)
        }
        .accessibilityIdentifier("dev_api_version")
        switch apiVersion {
        case .v1:
            hint("Orders are registered with register.do using the merchant login and password.")
            // Transfers are registered through API V1 (registerP2P.do).
            Picker("Order", selection: $orderType) {
                Text("Payment").tag(OrderType.purchase)
                Text("AFT transfer").tag(OrderType.aft)
            }
            .accessibilityIdentifier("dev_order_type")
            if orderType == .aft {
                hint("Transfers are registered with registerP2P.do (one-leg AFT): the sheet asks for the sender and recipient details.")
                InvestResetRow()
            }
            TextField("Merchant login", text: $login).textInputAutocapitalization(.never).autocorrectionDisabled()
                .accessibilityIdentifier("dev_login")
            SecureField("Password", text: $password).accessibilityIdentifier("dev_password")
        case .v2:
            hint("Sessions are created with POST /api2/sessions using the API key.")
            SecureField("API key", text: $apiKey).accessibilityIdentifier("dev_api_key")
        }
    }

    /// Puts the Northwind Invest demo account back as a fresh install has it.
    private struct InvestResetRow: View {
        @State private var reset = false

        var body: some View {
            Button("Reset Northwind demo account") {
                StorePrefs().resetInvestAccount()
                reset = true
            }
            .accessibilityIdentifier("dev_invest_reset")
            if reset { Text("Demo account reset").font(.footnote).foregroundColor(.secondary) }
        }
    }

    private func hint(_ text: LocalizedStringKey) -> some View {
        Text(text).font(.footnote).foregroundColor(.secondary)
    }

    private func link(_ title: LocalizedStringKey, _ subtitle: LocalizedStringKey) -> some View {
        VStack(alignment: .leading) {
            Text(title)
            Text(subtitle).font(.footnote).foregroundColor(.secondary)
        }
    }
}
