import SwiftUI

extension PayerSummaryLine {
    /// A line as the card draws it.
    struct Shown {
        let icon: PaymentIcon
        let text: String
    }

    var shown: Shown {
        Shown(icon: icon == "mail" ? .mail : icon == "phone" ? .phone : .pin, text: text)
    }
}

/// The payer data known for a saved card as a few lines; the fields open on Edit, or at once when something is wrong.
struct PayerSummarySection: View {
    @ObservedObject var store: PayerStore
    let paymentSystems: [String]

    @State private var expanded: Bool?
    @Environment(\.checkoutStrings) private var strings
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let form = store.form
        let specs = form.sections(paymentSystems: paymentSystems).all
        let lines = form.summaryLines(specs: specs)
        if lines.isEmpty {
            PayerFieldsSection(store: store, paymentSystems: paymentSystems)
        } else {
            let open = expanded ?? form.hasError(specs: specs)
            Group {
                if open {
                    VStack(spacing: 10) {
                        FieldGroupHeader(title: strings?.text(key: "payer_summary_title") ?? "") {
                            TextAction(text: strings?.text(key: "payer_summary_collapse") ?? "") {
                                dismissKeyboard()
                                if store.validateAll(specs) { expanded = false }
                            }
                            .offset(x: 8)
                        }
                        PayerFieldsSection(store: store, paymentSystems: paymentSystems)
                    }
                } else {
                    PayerSummaryCard(
                        title: strings?.text(key: "payer_summary_title") ?? "",
                        lines: lines.map { $0.shown },
                        identifier: "payer_summary",
                        onEdit: { expanded = true }
                    )
                }
            }
            .animation(reduceMotion ? nil : .linear(duration: summaryFade), value: open)
            .onAppear { if expanded == nil { expanded = open } }
        }
    }
}

/// Filled-in payer data folded into a card: an icon and a line each, and Edit.
struct PayerSummaryCard: View {
    let title: String
    let lines: [PayerSummaryLine.Shown]
    var identifier: String?
    let onEdit: () -> Void

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            FieldGroupHeader(title: title) {
                TextAction(text: strings?.text(key: "action_edit") ?? "", action: onEdit).offset(x: 8)
            }
            .padding(.top, 8)
            ForEach(lines.indices, id: \.self) { index in
                HStack(spacing: 10) {
                    IconView(icon: lines[index].icon, size: 18, color: theme.colors.onSurfaceVariant)
                    Text(lines[index].text).line(theme.body, color: theme.colors.onBackground).truncationMode(.tail)
                }
            }
        }
        .padding(EdgeInsets(top: 4, leading: 14, bottom: 12, trailing: 14))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: theme.fieldCorner).fill(theme.colors.background))
        .overlay(RoundedRectangle(cornerRadius: theme.fieldCorner).strokeBorder(theme.colors.outline, lineWidth: 1))
        .accessibility(identifier: identifier ?? "")
    }
}
