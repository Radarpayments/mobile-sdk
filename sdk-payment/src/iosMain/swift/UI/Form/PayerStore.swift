import SwiftUI

/// A list the payer picks from, raised over the checkout.
enum PayerPicker: Equatable {
    case country(PayerField)
    case phoneCode
    case option(PayerField)

    var id: String {
        switch self {
        case .country(let field): return "country-\(field.name)"
        case .phoneCode: return "phone-code"
        case .option(let field): return "option-\(field.name)"
        }
    }
}

/// The payer fields of a card step. The Kotlin form does not say when it changes, so every change goes through here.
final class PayerStore: ObservableObject {
    let form: PayerForm
    @Published private(set) var revision = 0
    @Published var picker: PayerPicker?

    init(form: PayerForm) {
        self.form = form
    }

    func change(_ body: (PayerForm) -> Void) {
        body(form)
        revision += 1
    }

    func validateAll(_ specs: [PayerFieldSpec]) -> Bool {
        let valid = form.validateAll(specs: specs)
        revision += 1
        return valid
    }
}
