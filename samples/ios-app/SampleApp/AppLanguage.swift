import Foundation
import SwiftUI

/// The language the demo shows its own screens in; the SDK gets the same one through `SdkPaymentConfig.locale`.
/// Chosen in the developer settings; empty follows the system.
enum AppLanguage {
    /// Languages the demo and the SDK are translated into, each named in itself, in the settings' order.
    static let all: [(code: String, name: String)] = [
        ("en", "English"),
        ("ru", "Русский"),
        ("ro", "Română"),
        ("el", "Ελληνικά"),
        ("bg", "Български"),
        ("de", "Deutsch"),
        ("fr", "Français"),
        ("es", "Español"),
        ("ar", "العربية"),
    ]

    /// The UserDefaults key the setting is stored under (also read by `StorePrefs.localeOverride`).
    static let key = "store_locale"

    /// The chosen language code, or nil for the system language.
    static var code: String? {
        UserDefaults.standard.string(forKey: key).flatMap { $0.isEmpty ? nil : $0 }
    }

    /// Strings built in code (`String(localized:bundle:)`): SwiftUI's `Text` follows the environment locale, these
    /// do not.
    static var bundle: Bundle {
        guard let code, let path = Bundle.main.path(forResource: code, ofType: "lproj"),
              let bundle = Bundle(path: path) else { return .main }
        return bundle
    }

    /// For formatting amounts and dates in the chosen language.
    /// Latin digits in Arabic too: card data and the SDK's texts use them, prices should match.
    static var locale: Locale {
        guard let code else { return .current }
        return Locale(identifier: code == "ar" ? "ar@numbers=latn" : code)
    }

    static var layoutDirection: LayoutDirection {
        Locale.Language(identifier: code ?? Locale.preferredLanguages.first ?? "en").characterDirection == .rightToLeft
            ? .rightToLeft : .leftToRight
    }
}

extension View {
    /// Shows the content in the chosen language [code] (nil follows the system), right to left for Arabic. The id
    /// rebuilds the screens on a change, so strings built in code are read again too.
    func appLanguage(_ code: String?) -> some View {
        environment(\.locale, AppLanguage.locale)
            .environment(\.layoutDirection, AppLanguage.layoutDirection)
            .id(code ?? "")
    }
}
