import XCTest
@testable import SampleApp

final class StorePrefsTests: XCTestCase {
    private var defaults: UserDefaults!
    private var prefs: StorePrefs!

    override func setUp() {
        defaults = UserDefaults(suiteName: "StorePrefsTests")
        defaults.removePersistentDomain(forName: "StorePrefsTests")
        prefs = StorePrefs(defaults: defaults, keychainService: "StorePrefsTests")
        prefs.merchantLogin = ""
        prefs.merchantPassword = ""
        prefs.apiKey = ""
    }

    func test_credentials_live_in_the_keychain_not_in_user_defaults() {
        prefs.merchantLogin = "ui-login"
        prefs.merchantPassword = "ui-password"
        XCTAssertEqual(StorePrefs(defaults: defaults, keychainService: "StorePrefsTests").merchantLogin, "ui-login")
        XCTAssertEqual(StorePrefs(defaults: defaults, keychainService: "StorePrefsTests").merchantPassword, "ui-password")
        XCTAssertNil(defaults.object(forKey: "store_login"))
        XCTAssertNil(defaults.object(forKey: "store_password"))
        prefs.merchantLogin = ""
        prefs.merchantPassword = ""
        XCTAssertEqual(StorePrefs(defaults: defaults, keychainService: "StorePrefsTests").merchantPassword, "")
    }

    func test_credentials_default_empty() {
        XCTAssertEqual(prefs.merchantLogin, "")
        XCTAssertEqual(prefs.merchantPassword, "")
    }

    func test_api_version_defaults_to_v1_and_round_trips() {
        XCTAssertEqual(prefs.apiVersion, .v1)
        prefs.apiVersion = .v2
        XCTAssertEqual(StorePrefs(defaults: defaults, keychainService: "StorePrefsTests").apiVersion, .v2)
        XCTAssertEqual(defaults.string(forKey: "store_api_version"), "V2")
    }

    func test_order_type_defaults_to_payment_and_round_trips() {
        XCTAssertEqual(prefs.orderType, .purchase)
        prefs.orderType = .aft
        XCTAssertEqual(StorePrefs(defaults: defaults, keychainService: "StorePrefsTests").orderType, .aft)
        XCTAssertEqual(defaults.string(forKey: "store_order_type"), "AFT")
    }

    func test_api_key_lives_in_the_keychain_not_in_user_defaults() {
        XCTAssertEqual(prefs.apiKey, "")
        prefs.apiKey = "key-1"
        XCTAssertEqual(StorePrefs(defaults: defaults, keychainService: "StorePrefsTests").apiKey, "key-1")
        XCTAssertNil(defaults.object(forKey: "store_api_key"))
    }

    func test_clientId_is_stable_and_short() {
        let first = prefs.clientId
        XCTAssertFalse(first.isEmpty)
        XCTAssertLessThanOrEqual(first.count, 24)
        XCTAssertEqual(StorePrefs(defaults: defaults, keychainService: "StorePrefsTests").clientId, first)
    }

    func test_resetClientId_persists_a_new_id() {
        let before = prefs.clientId
        let fresh = prefs.resetClientId()
        XCTAssertNotEqual(before, fresh)
        XCTAssertEqual(StorePrefs(defaults: defaults, keychainService: "StorePrefsTests").clientId, fresh)
    }

    func test_scenario_round_trips() {
        let scenario = Scenario(buyerWithCards: true, wallet: false, scanner: false, sdkResultScreen: true)
        prefs.scenario = scenario
        XCTAssertEqual(StorePrefs(defaults: defaults, keychainService: "StorePrefsTests").scenario, scenario)
    }

    func test_last_cover_round_trips() {
        prefs.lastCover = .coffee
        XCTAssertEqual(StorePrefs(defaults: defaults, keychainService: "StorePrefsTests").lastCover, .coffee)
        prefs.lastCover = nil
        XCTAssertNil(StorePrefs(defaults: defaults, keychainService: "StorePrefsTests").lastCover)
    }
}
