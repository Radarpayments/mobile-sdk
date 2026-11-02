import Foundation
import Security

/// Small generic-password store in the Keychain. Items stay on this device: they are not synced to
/// iCloud and not restored from a backup onto another device.
struct Keychain {
    let service: String

    func string(_ account: String) -> String? {
        var query = base(account)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var item: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess, let data = item as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    func set(_ value: String, _ account: String) {
        SecItemDelete(base(account) as CFDictionary)
        guard !value.isEmpty else { return }
        var query = base(account)
        query[kSecValueData as String] = Data(value.utf8)
        query[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        SecItemAdd(query as CFDictionary, nil)
    }

    private func base(_ account: String) -> [String: Any] {
        [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service, kSecAttrAccount as String: account]
    }
}
