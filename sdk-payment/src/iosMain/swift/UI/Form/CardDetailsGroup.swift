import SwiftUI

/// The card's fields as one frame: the number over the expiry and the security code.
struct CardDetailsGroup: View {
    enum Cell { case pan, expiry, cvc }

    @Binding var pan: String
    @Binding var expiry: String
    @Binding var cvc: String
    let number: CardNumberSpec
    let status: (Cell) -> GroupFieldStatus
    @Binding var hintShown: Bool
    /// Opens the card scanner; without it the group offers no scanning.
    var onScan: (() -> Void)?

    @State private var focused: Cell?
    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        let message = [Cell.pan, .expiry, .cvc].compactMap { status($0).message }.joined(separator: "\n")
        VStack(alignment: .leading, spacing: 6) {
            FieldGroupHeader(title: text("card_form_card_details_label")) {
                if let onScan = onScan {
                    TextAction(text: text("card_form_scan"), icon: .scan, action: onScan).offset(x: 8)
                }
            }
            FieldGroupFrame {
                GroupRow(highlighted: highlight(.pan) != nil) {
                    GroupTextCell(
                        text: CardEntry.shared.formatPan(digits: pan),
                        placeholder: text("card_form_pan_label"),
                        content: .pan,
                        status: status(.pan),
                        highlight: highlight(.pan),
                        edges: .of(row: 0, rows: 2),
                        onChange: { pan = CardEntry.shared.panDigits(text: $0) },
                        kept: { CardEntry.shared.formatPan(digits: pan) },
                        onFocus: { focus(.pan, $0) },
                        trailing: {
                            if number.logo != nil { CardNetworkIcon(logo: number.logo) }
                        }
                    )
                }
                GroupRowDivider()
                GroupSplitRow(highlighted: highlight(.expiry) != nil || highlight(.cvc) != nil, leading: {
                    GroupTextCell(
                        text: CardEntry.shared.formatExpiry(digits: expiry),
                        placeholder: text("card_form_expiry_placeholder"),
                        content: .expiry,
                        status: status(.expiry),
                        highlight: highlight(.expiry),
                        edges: .of(row: 1, rows: 2, column: 0, columns: 2),
                        label: text("card_form_expiry_label"),
                        onChange: { expiry = CardEntry.shared.expiryDigits(text: $0) },
                        kept: { CardEntry.shared.formatExpiry(digits: expiry) },
                        onFocus: { focus(.expiry, $0) }
                    )
                }, trailing: {
                    GroupTextCell(
                        text: cvc,
                        placeholder: text("card_form_cvv_label"),
                        content: .cvc,
                        status: status(.cvc),
                        highlight: highlight(.cvc),
                        edges: .of(row: 1, rows: 2, column: 1, columns: 2),
                        onChange: { cvc = CardEntry.shared.cvcDigits(text: $0, length: number.cvvLength) },
                        kept: { cvc },
                        onFocus: { focus(.cvc, $0) },
                        trailing: { CvcHintButton(shown: $hintShown) }
                    )
                })
            }
            if !message.isEmpty {
                FieldMessage(text: message)
            }
        }
    }

    private func text(_ key: String) -> String { strings?.text(key: key) ?? "" }

    private func focus(_ cell: Cell, _ gained: Bool) {
        if gained {
            focused = cell
        } else if focused == cell {
            focused = nil
        }
    }

    private func highlight(_ cell: Cell) -> Color? {
        if status(cell).isError { return theme.colors.error }
        return focused == cell ? theme.colors.primary : nil
    }
}
