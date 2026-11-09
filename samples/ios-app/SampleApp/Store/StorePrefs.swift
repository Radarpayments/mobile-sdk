import Foundation

/// The store's settings on this device: the merchant API and its credentials typed in the developer settings (in the
/// Keychain, this device only), the demo buyer, the demo panel and the SDK look. Nothing here ships with the app.
final class StorePrefs {
    private let defaults: UserDefaults
    private let keychain: Keychain

    init(defaults: UserDefaults = .standard, keychainService: String = "net.payrdr.samples.store") {
        self.defaults = defaults
        keychain = Keychain(service: keychainService)
    }

    var merchantLogin: String {
        get { launchOverride(Keys.login) ?? keychain.string(Keys.login) ?? "" }
        set { keychain.set(newValue, Keys.login) }
    }

    var merchantPassword: String {
        get { launchOverride(Keys.password) ?? keychain.string(Keys.password) ?? "" }
        set { keychain.set(newValue, Keys.password) }
    }

    /// The merchant API demo orders are registered through; API V1 until changed.
    var apiVersion: ApiVersion {
        get { defaults.string(forKey: Keys.apiVersion).flatMap(ApiVersion.init(rawValue:)) ?? .v1 }
        set { defaults.set(newValue.rawValue, forKey: Keys.apiVersion) }
    }

    /// What demo orders are when registered through API V1: payments until changed.
    var orderType: OrderType {
        get { defaults.string(forKey: Keys.orderType).flatMap(OrderType.init(rawValue:)) ?? .purchase }
        set { defaults.set(newValue.rawValue, forKey: Keys.orderType) }
    }

    /// The API V2 key typed in the developer settings; like the password, it is kept only in this device's Keychain.
    var apiKey: String {
        get { launchOverride(Keys.apiKey) ?? keychain.string(Keys.apiKey) ?? "" }
        set { keychain.set(newValue, Keys.apiKey) }
    }

    /// A value passed as a launch argument (`-store_login …`), e.g. by UI tests; it wins over the Keychain.
    private func launchOverride(_ key: String) -> String? {
        UserDefaults.standard.volatileDomain(forName: UserDefaults.argumentDomain)[key] as? String
    }

    /// The demo buyer's clientId; created on first use.
    var clientId: String {
        if let id = defaults.string(forKey: Keys.clientId), !id.isEmpty { return id }
        return resetClientId()
    }

    /// Replaces the demo buyer with a new one (no saved cards yet) and returns its clientId.
    @discardableResult
    func resetClientId() -> String {
        // Short on purpose: a 41-character id made the test gateway decline payments that save the card.
        let id = "demo-" + UUID().uuidString.lowercased().prefix(13)
        defaults.set(id, forKey: Keys.clientId)
        return id
    }

    var scenario: Scenario {
        get { defaults.data(forKey: Keys.scenario).flatMap { try? JSONDecoder().decode(Scenario.self, from: $0) } ?? Scenario() }
        set { defaults.set(try? JSONEncoder().encode(newValue), forKey: Keys.scenario) }
    }

    /// The cover to open on the next launch; nil opens the start screen.
    var lastCover: CoverId? {
        get { defaults.string(forKey: Keys.lastCover).flatMap(CoverId.init(rawValue:)) }
        set { defaults.set(newValue?.rawValue, forKey: Keys.lastCover) }
    }

    var lastMdOrder: String? {
        get { defaults.string(forKey: Keys.lastMdOrder) }
        set { defaults.set(newValue, forKey: Keys.lastMdOrder) }
    }

    var sheetTheme: SheetThemeOverride {
        get { defaults.string(forKey: Keys.sheetTheme).flatMap(SheetThemeOverride.init(rawValue:)) ?? .cover }
        set { defaults.set(newValue.rawValue, forKey: Keys.sheetTheme) }
    }

    /// SDK language; nil follows the system.
    var localeOverride: String? {
        get { defaults.string(forKey: Keys.locale).flatMap { $0.isEmpty ? nil : $0 } }
        set { defaults.set(newValue, forKey: Keys.locale) }
    }

    /// The Northwind Invest demo account, the seed until a transfer or an investment changes it.
    var investAccount: InvestAccount {
        get { InvestAccount.fromJson(defaults.string(forKey: Keys.investAccount)) }
        set { defaults.set(newValue.toJson(), forKey: Keys.investAccount) }
    }

    private enum Keys {
        static let login = "store_login"
        static let password = "store_password"
        static let apiVersion = "store_api_version"
        static let apiKey = "store_api_key"
        static let orderType = "store_order_type"
        static let clientId = "store_client_id"
        static let scenario = "store_scenario"
        static let lastCover = "store_last_cover"
        static let lastMdOrder = "store_last_md_order"
        static let sheetTheme = "store_sheet_theme"
        static let locale = "store_locale"
        static let investAccount = "invest_account"
    }
}
