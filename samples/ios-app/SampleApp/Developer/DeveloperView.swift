import SwiftUI
import UIKit

/// Settings for whoever runs the demo: the gateway and merchant credentials used to register orders,
/// the SDK sheet look, Apple Pay, and the Advanced demo. The four sample apps lay it out the same way.
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
            gatewaySection
            credentialsSection
            Section("SDK appearance") {
                Picker("Sheet theme", selection: $sheetTheme) {
                    Text("As the store").tag(SheetThemeOverride.cover.rawValue)
                    Text("theme_light").tag(SheetThemeOverride.light.rawValue)
                    Text("Dark").tag(SheetThemeOverride.dark.rawValue)
                    Text("theme_system").tag(SheetThemeOverride.system.rawValue)
                }
                Picker("Language", selection: $locale) {
                    Text("System").tag("")
                    ForEach(AppLanguage.all, id: \.code) { language in
                        Text(verbatim: language.name).tag(language.code)
                    }
                }
            }
            Section("Apple Pay") {
                LabeledField(label: "Merchant ID", text: $applePayMerchantId)
            }
            Section("More") {
                NavigationLink(value: SampleRoute.paymentAdvanced) {
                    VStack(alignment: .leading) {
                        Text("Advanced")
                        Text("State stream, SSL pinning").font(.footnote).foregroundColor(.secondary)
                    }
                }
                LastOrderRow(mdOrder: lastMdOrder)
            }
        }
        .navigationTitle("Developer settings")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { lastMdOrder = StorePrefs().lastMdOrder }
        .onChange(of: login) { StorePrefs().merchantLogin = $0 }
        .onChange(of: password) { StorePrefs().merchantPassword = $0 }
        .onChange(of: apiKey) { StorePrefs().apiKey = $0 }
        .onChange(of: locale) { if $0.isEmpty { UserDefaults.standard.removeObject(forKey: AppLanguage.key) } }
        .preferredColorScheme(.light)
    }

    /// Where orders go and which merchant API registers them.
    private var gatewaySection: some View {
        Section {
            LabeledField(
                label: "Base URL",
                text: $baseUrl,
                keyboard: .URL,
                error: Self.isGatewayUrl(baseUrl) ? nil : "Enter an address that starts with https://"
            )
            SegmentRow(title: "Merchant API", id: "dev_api_version", selection: $apiVersion) {
                Text(verbatim: "API V1").tag(ApiVersion.v1)
                Text(verbatim: "API V2").tag(ApiVersion.v2)
            }
        } header: {
            Text("Gateway")
        } footer: {
            Text(apiVersion == .v1
                ? "Orders are registered with register.do using the merchant login and password."
                : "Sessions are created with POST /api2/sessions using the API key.")
        }
    }

    /// API V1: the order type, login and password; API V2: the API key.
    private var credentialsSection: some View {
        Section {
            switch apiVersion {
            case .v1:
                // Transfers are registered through API V1 (registerP2P.do).
                SegmentRow(title: "Order", id: "dev_order_type", selection: $orderType) {
                    Text("Payment").tag(OrderType.purchase)
                    Text("AFT transfer").tag(OrderType.aft)
                }
                LabeledField(label: "Merchant login", id: "dev_login", text: $login)
                LabeledField(label: "Password", id: "dev_password", text: $password, secret: true)
            case .v2:
                LabeledField(label: "API key", id: "dev_api_key", text: $apiKey, secret: true)
            }
        } header: {
            Text("Credentials")
        } footer: {
            VStack(alignment: .leading, spacing: 6) {
                if apiVersion == .v1 && orderType == .aft {
                    Text("Transfers are registered with registerP2P.do (one-leg AFT): the sheet asks for the sender and recipient details.")
                }
                Text("Stored only on this device. In your app the order is registered by your backend.")
            }
        }
    }

    /// The gateway is reached over HTTPS only; anything else would fail on the first order with a less telling error.
    static func isGatewayUrl(_ text: String) -> Bool {
        guard let url = URL(string: text.trimmingCharacters(in: .whitespaces)) else { return false }
        return url.scheme == "https" && !(url.host ?? "").isEmpty
    }
}

/// A text field with its name always above the value. A secret one masks the value and has a button to show it, since a typo
/// in a merchant password is otherwise impossible to spot.
private struct LabeledField: View {
    let label: LocalizedStringKey
    var id = ""
    @Binding var text: String
    var keyboard: UIKeyboardType = .default
    var secret = false
    var error: LocalizedStringKey?
    @State private var shown = false

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(.caption).foregroundColor(.secondary).accessibilityHidden(true)
                Group {
                    // An empty prompt: without it the label turns into a placeholder that repeats the caption above.
                    if secret && !shown {
                        SecureField(text: $text, prompt: Text(verbatim: "")) { Text(label) }
                    } else {
                        TextField(text: $text, prompt: Text(verbatim: "")) { Text(label) }
                            .keyboardType(keyboard)
                    }
                }
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .accessibilityLabel(Text(label))
                .accessibilityIdentifier(id)
                if let error {
                    Text(error).font(.caption).foregroundColor(.red)
                }
            }
            if secret {
                Button {
                    shown.toggle()
                } label: {
                    Image(systemName: shown ? "eye.slash" : "eye").foregroundColor(.secondary)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(shown ? Text("Hide") : Text("Show"))
            }
        }
        .padding(.vertical, 2)
    }
}

/// A choice between two values, as a segmented control next to its name.
private struct SegmentRow<Value: Hashable, Options: View>: View {
    let title: LocalizedStringKey
    let id: String
    @Binding var selection: Value
    @ViewBuilder let options: () -> Options

    var body: some View {
        HStack(spacing: 12) {
            // The segments keep their natural width, so without this a longer name breaks mid-word.
            Text(title).fixedSize()
            Spacer(minLength: 0)
            Picker(title, selection: $selection, content: options)
                .pickerStyle(.segmented)
                .fixedSize()
                .accessibilityIdentifier(id)
        }
    }
}

/// The last registered order, monospaced, with Copy that turns into "Copied" for a moment.
private struct LastOrderRow: View {
    let mdOrder: String?
    @State private var copied = false

    var body: some View {
        HStack {
            VStack(alignment: .leading) {
                Text("Last order")
                Text(verbatim: mdOrder ?? "—").font(.system(size: 12, design: .monospaced)).foregroundColor(.secondary)
            }
            Spacer()
            if let mdOrder {
                if copied {
                    DoneMark(text: "Copied")
                } else {
                    Button("Copy") {
                        UIPasteboard.general.string = mdOrder
                        flash($copied)
                    }
                    .buttonStyle(.borderless)
                }
            }
        }
    }
}

private struct DoneMark: View {
    let text: LocalizedStringKey

    var body: some View {
        HStack(spacing: 4) {
            Text(text)
            Image(systemName: "checkmark").accessibilityHidden(true)
        }
        .foregroundColor(Color(red: 0.08, green: 0.5, blue: 0.24))
    }
}

/// Sets the flag for as long as "Copied" stays.
private func flash(_ flag: Binding<Bool>) {
    flag.wrappedValue = true
    DispatchQueue.main.asyncAfter(deadline: .now() + 2) { flag.wrappedValue = false }
}
