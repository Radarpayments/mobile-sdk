import Foundation
import Security

/// One secret value: the stored stand profiles with their credentials.
nonisolated protocol SecretStore {
    /// The stored value, or nil when there is none or it cannot be read.
    func read() -> Data?

    /// Stores [data], or removes the value when it is nil. Throws when the value could not be stored or removed.
    func write(_ data: Data?) throws
}

/// A failed Keychain call. It carries the status alone, never the value.
nonisolated struct SecretStoreError: Error, CustomStringConvertible {
    let status: OSStatus

    var description: String {
        "SecretStoreError(status: \(status))"
    }
}

/// The Keychain item of the app: a generic password readable only while the device is unlocked, never synced or backed
/// up to another device.
nonisolated struct KeychainSecretStore: SecretStore {
    static let profilesService = "net.payrdr.mobile.payment.sdk.qa.profiles"

    private let service: String

    init(service: String = profilesService) {
        self.service = service
    }

    private var query: [CFString: Any] {
        [kSecClass: kSecClassGenericPassword, kSecAttrService: service]
    }

    // Any failure, a locked device included, reads as no profiles rather than stopping the app.
    func read() -> Data? {
        var request = query
        request[kSecReturnData] = true
        request[kSecMatchLimit] = kSecMatchLimitOne
        var result: CFTypeRef?
        guard SecItemCopyMatching(request as CFDictionary, &result) == errSecSuccess else { return nil }
        return result as? Data
    }

    func write(_ data: Data?) throws {
        guard let data else {
            let status = SecItemDelete(query as CFDictionary)
            if status != errSecSuccess, status != errSecItemNotFound { throw SecretStoreError(status: status) }
            return
        }
        let attributes: [CFString: Any] = [
            kSecValueData: data,
            kSecAttrAccessible: kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
        ]
        var status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound {
            status = SecItemAdd(query.merging(attributes) { _, new in new } as CFDictionary, nil)
        }
        if status != errSecSuccess { throw SecretStoreError(status: status) }
    }
}
