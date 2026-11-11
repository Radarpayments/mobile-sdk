import SwiftUI

/// The main button of a payment step: the pay label with a lock.
struct PayButton: View {
    let label: String
    var enabled = true
    var loading = false
    var showLock = true
    let action: () -> Void

    var body: some View {
        CheckoutButton(text: label, icon: showLock ? .lockBold : nil, enabled: enabled, loading: loading, primary: true, action: action)
    }
}

extension PaymentStrings {
    func format(_ key: String, _ args: String...) -> String {
        format(key: key, args: KotlinArray<NSString>(size: Int32(args.count)) { index in args[index.intValue] as NSString })
    }

    /// What the main button says: the amount when it is known, and what the checkout does with the card.
    func payLabel(order: OrderSummary?, transfer: Bool, saveCardOnly: Bool) -> String {
        if saveCardOnly { return text(key: "card_form_save_card_only") }
        if let amount = order?.formattedAmount {
            return format(transfer ? "transfer_amount" : "pay_amount", SavedCardSpecs.shared.leftToRight(text: amount))
        }
        return text(key: transfer ? "card_form_transfer" : "card_form_pay")
    }
}
