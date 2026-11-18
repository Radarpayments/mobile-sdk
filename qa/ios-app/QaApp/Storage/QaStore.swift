import Combine
import Foundation

/// The current configuration (its JSON text, in the defaults) and the stand profiles (one JSON array in the Keychain,
/// since they carry the credentials). Nothing stored is trusted: what cannot be read — written by another build, or a
/// Keychain item that cannot be opened — reads as the default configuration and no profiles, and the app keeps working.
@MainActor
final class QaStore: ObservableObject {
    nonisolated static let configKey = "qa.config"

    @Published private(set) var config: QaConfig

    /// Sorted by name; the profiles of a stored list that are invalid are left out, the others are kept.
    @Published private(set) var profiles: [StandProfile]

    private let defaults: UserDefaults
    private let secrets: SecretStore

    init(defaults: UserDefaults, secrets: SecretStore) {
        self.defaults = defaults
        self.secrets = secrets
        config = Self.readConfig(defaults)
        profiles = Self.readProfiles(secrets.read())
    }

    /// Stores [config] as it is: the caller validates it first, since one that is invalid reads back as the default.
    func save(config: QaConfig) {
        defaults.set(QaConfigJson.encode(config), forKey: Self.configKey)
        self.config = Self.readConfig(defaults)
    }

    /// Replaces the profile of the same name; a stored list that cannot be read is started again with this profile. The
    /// caller validates [profile] first: an invalid one is stored and then left out when the list is read. Throws when
    /// the Keychain refuses the write, and the profiles stay as they were.
    func save(profile: StandProfile) throws {
        let stored = Self.readProfiles(secrets.read())
        try write(stored.filter { !sameText($0.name, profile.name) } + [profile])
    }

    /// Leaves the config alone: one that names the deleted profile is reported by the launch check. A list that cannot
    /// be read, or holds no profile of that name, is left as it is stored. Throws when the Keychain refuses the write.
    func deleteProfile(named name: String) throws {
        let stored = Self.readProfiles(secrets.read())
        guard stored.contains(where: { sameText($0.name, name) }) else { return }
        try write(stored.filter { !sameText($0.name, name) })
    }

    private func write(_ list: [StandProfile]) throws {
        if list.isEmpty {
            try secrets.write(nil)
        } else {
            let array = JSONValue.array(list.compactMap { JSONValue.parse(StandProfileJson.encode($0)) })
            try secrets.write(Data(array.text.utf8))
        }
        profiles = Self.readProfiles(secrets.read())
    }

    // The stored copy is the app's own, so it is not held to a document's size: pretty printing makes a configuration
    // that was accepted near the limit longer than the limit. Its depth is what was accepted, so that limit stays.
    private nonisolated static func readConfig(_ defaults: UserDefaults) -> QaConfig {
        guard let text = defaults.string(forKey: configKey),
              case let .ok(config) = QaConfigJson.decode(text, maxBytes: .max)
        else { return QaConfig() }
        return config
    }

    private nonisolated static func readProfiles(_ stored: Data?) -> [StandProfile] {
        guard let stored, let plain = String(data: stored, encoding: .utf8) else { return [] }
        // The stored array holds every profile: one level above a document, and as large as the profiles are many.
        guard limitProblem(plain, maxBytes: .max, maxDepth: DocumentLimits.maxDepth + 1) == nil,
              case let .array(elements) = JSONValue.parse(plain)
        else { return [] }
        var byName: [[Unicode.Scalar]: StandProfile] = [:]
        for element in elements {
            guard case .object = element, case let .ok(profile) = StandProfileJson.decode(element.text, maxBytes: .max)
            else { continue }
            byName[Array(profile.name.unicodeScalars)] = profile
        }
        return byName.sorted { $0.key.lexicographicallyPrecedes($1.key) { $0.value < $1.value } }.map(\.value)
    }
}
