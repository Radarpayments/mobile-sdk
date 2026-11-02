import Foundation
import Combine
import SwiftUI
import sdk_payment_scanner

enum SdkCoreVariant: String, CaseIterable {
    case newCard = "new"
    case instant = "instant"
    case stored = "stored"
}

/// Inputs of the SDK Core form; each sdk-core `ParamField` error is shown next to one of them.
enum CoreField: String, CaseIterable {
    case pan = "PAN"
    case expiry = "EXPIRY"
    case cvc = "CVC"
    case holder = "CARDHOLDER"
    case bindingId = "BINDING_ID"
    case mdOrder = "MD_ORDER"
    case pubKey = "PUB_KEY"
}

/// Typed expiry as `MM/YY`: digits only, the slash added after the month.
func formatExpiryInput(_ raw: String) -> String {
    let digits = String(raw.filter(\.isNumber).prefix(4))
    return digits.count > 2 ? String(digits.prefix(2)) + "/" + String(digits.dropFirst(2)) : digits
}

/// The SDK Core form. Card data stay in memory; the view remembers mdOrder, key, bindingId and the variant.
final class SdkCoreFormModel: ObservableObject {
    @Published var variant: SdkCoreVariant = .newCard
    @Published var pan: String = ""
    @Published var expiry: String = ""
    @Published var cvc: String = ""
    @Published var holder: String = ""
    @Published var mdOrder: String = ""
    @Published var pubKeyPem: String = ""
    @Published var bindingId: String = ""
    /// The token of the last successful [submit], nil while the form is shown.
    @Published var token: String?
    /// sdk-core's error codes from the last [submit], by the field they belong to.
    @Published var errors: [CoreField: String] = [:]

    func buildParams() -> Sdk_corePaymentCardParams {
        switch variant {
        case .newCard:
            return IosSdkCoreSurfaceKt.iosCardParams(
                mdOrder: mdOrder,
                pan: pan,
                cvc: cvc,
                expiryMMYY: expiry,
                cardHolder: holder.isEmpty ? nil : holder,
                pubKey: pubKeyPem
            )
        case .instant:
            return IosSdkCoreSurfaceKt.iosCardInstantParams(
                pan: pan,
                cvc: cvc,
                expiryMMYY: expiry,
                cardHolder: holder.isEmpty ? nil : holder,
                pubKey: pubKeyPem
            )
        case .stored:
            return IosSdkCoreSurfaceKt.iosBindingParams(
                mdOrder: mdOrder,
                bindingID: bindingId,
                cvc: cvc.isEmpty ? nil : cvc,
                pubKey: pubKeyPem
            )
        }
    }

    func generate() -> Sdk_coreTokenResult {
        let cfg = IosSdkCoreSurfaceKt.iosSdkCoreConfig(
            cardParams: buildParams(),
            timestamp: Int64(Date().timeIntervalSince1970 * 1000)
        )
        return IosSdkCoreSurfaceKt.iosSdkCore().generate(config: cfg)
    }

    /// Encrypts the form: a token, or the errors next to their fields. An error no field explains
    /// (e.g. an unusable key) is shown at the key.
    func submit() {
        let result = generate()
        var found: [CoreField: String] = [:]
        for (field, code) in result.errors {
            if let f = CoreField(rawValue: field.name) { found[f] = code as String }
        }
        if !result.isSuccessful && found.isEmpty { found[.pubKey] = "invalid" }
        errors = found
        token = result.isSuccessful ? result.token : nil
    }
}
