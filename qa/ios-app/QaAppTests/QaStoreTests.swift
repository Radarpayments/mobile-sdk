@testable import QaApp
import Security
import XCTest

final class InMemorySecretStore: SecretStore {
    var data: Data?
    var failsWrites = false

    init(_ data: Data? = nil) {
        self.data = data
    }

    convenience init(text: String) {
        self.init(Data(text.utf8))
    }

    var text: String? {
        data.map { String(decoding: $0, as: UTF8.self) }
    }

    func read() -> Data? {
        data
    }

    func write(_ data: Data?) throws {
        if failsWrites { throw SecretStoreError(status: errSecInteractionNotAllowed) }
        self.data = data
    }
}

final class QaStoreTests: XCTestCase {
    private let suite = "qa-store-tests-\(UUID().uuidString)"
    private let sandbox = StandProfile(
        name: "sandbox",
        gatewayUrl: "https://dev.bpcbt.com/payment",
        login: "login-1",
        password: "password-1",
        apiKey: "key-1"
    )

    private var defaults: UserDefaults {
        UserDefaults(suiteName: suite)!
    }

    override func tearDown() {
        UserDefaults().removePersistentDomain(forName: suite)
        super.tearDown()
    }

    @MainActor
    private func store(_ secrets: SecretStore = InMemorySecretStore()) -> QaStore {
        QaStore(defaults: defaults, secrets: secrets)
    }

    private func storedConfig(_ text: String) {
        defaults.set(text, forKey: QaStore.configKey)
    }

    @MainActor
    func test_a_fresh_store_has_the_default_config_and_no_profiles() {
        let store = store()
        XCTAssertEqual(store.config, QaConfig())
        XCTAssertEqual(store.profiles, [])
    }

    @MainActor
    func test_a_saved_config_is_read_back() {
        var config = QaConfig(profile: "sandbox")
        config.order.amount = 4200
        config.order.features = ["VERIFY"]
        store().save(config: config)
        XCTAssertEqual(store().config, config)
    }

    @MainActor
    func test_the_saved_config_is_published() {
        let store = store()
        store.save(config: QaConfig(profile: "sandbox"))
        XCTAssertEqual(store.config, QaConfig(profile: "sandbox"))
    }

    @MainActor
    func test_saving_a_profile_of_the_same_name_replaces_it() throws {
        let secrets = InMemorySecretStore()
        let store = store(secrets)
        try store.save(profile: sandbox)
        var second = sandbox
        second.gatewayUrl = "https://dev.bpcbt.com/second"
        second.api = .v2
        try store.save(profile: second)
        XCTAssertEqual(store.profiles, [second])
        XCTAssertEqual(self.store(secrets).profiles, [second])
    }

    @MainActor
    func test_profiles_are_sorted_by_name_and_keep_their_credentials() throws {
        let secrets = InMemorySecretStore()
        var zeta = sandbox
        zeta.name = "zeta"
        var alpha = sandbox
        alpha.name = "alpha"
        try store(secrets).save(profile: zeta)
        try store(secrets).save(profile: alpha)
        let profiles = store(secrets).profiles
        XCTAssertEqual(profiles, [alpha, zeta])
        XCTAssertEqual(profiles.map(\.password), ["password-1", "password-1"])
        XCTAssertEqual(profiles.map(\.apiKey), ["key-1", "key-1"])
    }

    @MainActor
    func test_a_deleted_profile_is_gone_and_the_config_still_names_it() throws {
        let store = store()
        try store.save(profile: sandbox)
        store.save(config: QaConfig(profile: "sandbox"))
        try store.deleteProfile(named: "sandbox")
        XCTAssertEqual(store.profiles, [])
        XCTAssertEqual(store.config.profile, "sandbox")
    }

    @MainActor
    func test_deleting_from_a_list_that_cannot_be_read_leaves_it_stored() throws {
        let secrets = InMemorySecretStore(text: "not json")
        try store(secrets).deleteProfile(named: "sandbox")
        XCTAssertEqual(secrets.text, "not json")
    }

    @MainActor
    func test_deleting_an_unknown_name_leaves_the_stored_list_as_it_is() throws {
        let raw = "[ \(StandProfileJson.encode(sandbox)) ]"
        let secrets = InMemorySecretStore(text: raw)
        try store(secrets).deleteProfile(named: "other")
        XCTAssertEqual(secrets.text, raw)
    }

    @MainActor
    func test_deleting_the_last_profile_removes_the_secret() throws {
        let secrets = InMemorySecretStore()
        let store = store(secrets)
        try store.save(profile: sandbox)
        try store.deleteProfile(named: "sandbox")
        XCTAssertNil(secrets.data)
    }

    @MainActor
    func test_the_credentials_are_stored_as_secrets_and_never_in_the_defaults() throws {
        let secrets = InMemorySecretStore()
        let store = store(secrets)
        try store.save(profile: sandbox)
        store.save(config: QaConfig(profile: "sandbox"))
        XCTAssertTrue(secrets.text?.contains("password-1") == true)
        let stored = String(describing: defaults.persistentDomain(forName: suite) ?? [:])
        for secret in ["login-1", "password-1", "key-1"] {
            XCTAssertFalse(stored.contains(secret), "the defaults hold a credential")
        }
    }

    @MainActor
    func test_a_stored_config_of_another_version_reads_as_the_default() {
        storedConfig(#"{"version":2,"profile":"sandbox"}"#)
        XCTAssertEqual(store().config, QaConfig())
    }

    @MainActor
    func test_a_stored_config_that_is_not_json_reads_as_the_default() {
        storedConfig("<<<")
        XCTAssertEqual(store().config, QaConfig())
    }

    @MainActor
    func test_a_stored_config_that_is_not_text_reads_as_the_default() {
        defaults.set(7, forKey: QaStore.configKey)
        XCTAssertEqual(store().config, QaConfig())
    }

    @MainActor
    func test_profiles_that_cannot_be_opened_read_as_none() {
        XCTAssertEqual(store(InMemorySecretStore(text: "<<<")).profiles, [])
        XCTAssertEqual(store(InMemorySecretStore(Data([0xFF, 0xFE, 0x5B]))).profiles, [])
    }

    @MainActor
    func test_a_deeply_nested_store_reads_as_defaults() {
        let deep = String(repeating: "[", count: 100_000)
        storedConfig(deep)
        let store = store(InMemorySecretStore(text: deep))
        XCTAssertEqual(store.config, QaConfig())
        XCTAssertEqual(store.profiles, [])
    }

    @MainActor
    func test_a_stored_profile_list_that_is_not_an_array_reads_as_none() {
        XCTAssertEqual(store(InMemorySecretStore(text: #"{"name":"sandbox"}"#)).profiles, [])
    }

    @MainActor
    func test_an_invalid_profile_is_left_out_and_the_others_are_kept() {
        let broken = #"{"name":"Not A Name","gatewayUrl":"https://dev.bpcbt.com/payment"}"#
        let secrets = InMemorySecretStore(text: "[\(broken),7,\(StandProfileJson.encode(sandbox))]")
        XCTAssertEqual(store(secrets).profiles, [sandbox])
    }

    @MainActor
    func test_of_two_stored_profiles_of_one_name_the_later_is_kept() {
        var later = sandbox
        later.api = .v2
        let secrets = InMemorySecretStore(text: "[\(StandProfileJson.encode(sandbox)),\(StandProfileJson.encode(later))]")
        XCTAssertEqual(store(secrets).profiles, [later])
    }

    @MainActor
    func test_a_profile_list_that_is_not_strict_json_reads_as_none() {
        let profile = StandProfileJson.encode(sandbox)
        for text in ["[\(profile),abc]", "[\(profile),01]", "[\(profile)] x"] {
            XCTAssertEqual(store(InMemorySecretStore(text: text)).profiles, [], text)
        }
    }

    @MainActor
    func test_saving_a_profile_over_a_list_that_cannot_be_read_starts_the_list_again() throws {
        let secrets = InMemorySecretStore(text: "<<<")
        try store(secrets).save(profile: sandbox)
        XCTAssertEqual(store(secrets).profiles, [sandbox])
    }

    @MainActor
    func test_the_stored_config_is_its_document() {
        let config = QaConfig(profile: "sandbox")
        store().save(config: config)
        XCTAssertEqual(defaults.string(forKey: QaStore.configKey), QaConfigJson.encode(config))
    }

    // Pretty printing indents every member, so a document near the size limit grows past it when the app writes it.
    @MainActor
    func test_a_saved_config_larger_than_a_document_is_read_back() {
        var config = QaConfig(profile: "sandbox")
        for index in 0 ..< 4000 {
            config.order.extra["key\(index)"] = .string("value")
        }
        XCTAssertGreaterThan(QaConfigJson.encode(config).utf8.count, DocumentLimits.maxBytes)
        store().save(config: config)
        XCTAssertEqual(store().config, config)
    }

    @MainActor
    func test_a_failed_profile_write_is_thrown_and_the_stored_profiles_stay() throws {
        let secrets = InMemorySecretStore()
        let store = store(secrets)
        try store.save(profile: sandbox)
        secrets.failsWrites = true
        var other = sandbox
        other.name = "other"
        XCTAssertThrowsError(try store.save(profile: other)) { error in
            XCTAssertFalse(String(describing: error).contains("password-1"))
        }
        XCTAssertThrowsError(try store.deleteProfile(named: "sandbox"))
        XCTAssertEqual(store.profiles, [sandbox])
    }

    func test_the_keychain_keeps_a_secret_until_it_is_removed() throws {
        let keychain = KeychainSecretStore(service: "net.payrdr.mobile.payment.sdk.qa.tests")
        defer { try? keychain.write(nil) }
        try keychain.write(Data("first".utf8))
        try keychain.write(Data("second".utf8))
        XCTAssertEqual(keychain.read(), Data("second".utf8))
        try keychain.write(nil)
        XCTAssertNil(keychain.read())
        try keychain.write(nil)
    }
}
