import SwiftUI

/// A billing address nobody has to give: a row that offers it, the fields, or what was filled in as one line.
struct CollapsibleBillingGroup: View {
    @ObservedObject var store: PayerStore
    let billing: [PayerFieldSpec]
    let canAddLine: Bool

    @State private var view: String?
    @State private var focusOnOpen: PayerField?
    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let hasError = billing.contains { store.form.visibleError(spec: $0) != nil }
        let shown = hasError ? "fields" : view ?? store.form.billingView(billing: billing)
        Group {
            switch shown {
            case "add":
                addRow
            case "summary":
                PayerSummaryCard(
                    title: strings?.text(key: "payer_billing_title") ?? "",
                    lines: [PayerSummaryLine.Shown(icon: .pin, text: store.form.billingSummary(specs: billing))],
                    identifier: "payer_billing_summary",
                    onEdit: { view = "fields" }
                )
            default:
                BillingGroup(store: store, billing: billing, canAddLine: canAddLine, initialFocus: focusOnOpen, onHide: {
                    focusOnOpen = nil
                    dismissKeyboard()
                    if store.validateAll(billing) { view = store.form.billingView(billing: billing) }
                })
            }
        }
        .animation(reduceMotion ? nil : .linear(duration: summaryFade), value: shown)
    }

    private var addRow: some View {
        FieldGroupFrame {
            GroupRow {
                GroupButtonCell(identifier: "payer_billing_add", action: {
                    focusOnOpen = billing.first { $0.field != PayerField.billingCountry }?.field
                    view = "fields"
                }, content: {
                    IconView(icon: .plus, size: 20, color: theme.colors.primary)
                    Text(strings?.text(key: "payer_billing_add") ?? "")
                        .line(theme.input, color: theme.colors.primary)
                        .truncationMode(.tail)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.leading, 8)
                    Text(strings?.text(key: "payer_optional_note") ?? "")
                        .line(theme.caption, color: theme.colors.onSurfaceVariant)
                        .padding(.leading, 8)
                })
            }
        }
    }
}

/// The billing address as one frame: the country, the address lines, the city beside the postal code, the state.
struct BillingGroup: View {
    @ObservedObject var store: PayerStore
    let billing: [PayerFieldSpec]
    let canAddLine: Bool
    var initialFocus: PayerField?
    var onHide: (() -> Void)?

    @State private var focused: PayerField?
    @State private var lineToFocus: PayerField?
    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings

    private enum Cell {
        case field(PayerFieldSpec)
        case addLine

        var id: String {
            switch self {
            case .field(let spec): return spec.field.name
            case .addLine: return "add-line"
            }
        }
    }

    var body: some View {
        let form = store.form
        let rows = self.rows
        let error = billing.compactMap { form.visibleError(spec: $0) }.first
        let someOptional = billing.contains { !$0.mandatory }
        VStack(alignment: .leading, spacing: 6) {
            FieldGroupHeader(title: text("payer_billing_title")) {
                if let onHide = onHide {
                    TextAction(text: text("payer_summary_collapse"), action: onHide).offset(x: 8)
                } else if !billing.contains(where: { $0.mandatory }) {
                    Text(text("payer_optional_note")).line(theme.caption, color: theme.colors.onSurfaceVariant)
                }
            }
            LeadingParagraph(text: text("payer_billing_help"), style: theme.caption, color: theme.colors.onSurfaceVariant)
            FieldGroupFrame {
                ForEach(rows.indices, id: \.self) { row in
                    if row > 0 { GroupRowDivider() }
                    let cells = rows[row]
                    let highlighted = cells.contains { highlight($0) != nil }
                    if cells.count == 2 {
                        GroupSplitRow(highlighted: highlighted, leading: {
                            cell(cells[0], edges: .of(row: row, rows: rows.count, column: 0, columns: 2), markMandatory: someOptional)
                        }, trailing: {
                            cell(cells[1], edges: .of(row: row, rows: rows.count, column: 1, columns: 2), markMandatory: someOptional)
                        })
                    } else {
                        GroupRow(highlighted: highlighted) {
                            cell(cells[0], edges: .of(row: row, rows: rows.count), markMandatory: someOptional).id(cells[0].id)
                        }
                    }
                }
            }
            if let error = error {
                FieldMessage(text: error)
            }
        }
        .onAppear { lineToFocus = initialFocus }
    }

    private var rows: [[Cell]] {
        var rows: [[Cell]] = []
        func spec(_ field: PayerField) -> PayerFieldSpec? { billing.first { $0.field == field } }
        if let country = spec(.billingCountry) { rows.append([.field(country)]) }
        for line in billing where store.form.isAddressLine(field: line.field) { rows.append([.field(line)]) }
        if canAddLine { rows.append([.addLine]) }
        let pair = [spec(.billingCity), spec(.billingPostalCode)].compactMap { $0 }.map { Cell.field($0) }
        if !pair.isEmpty { rows.append(pair) }
        if let state = spec(.billingState) { rows.append([.field(state)]) }
        return rows
    }

    private func text(_ key: String) -> String { strings?.text(key: key) ?? "" }

    private func highlight(_ cell: Cell) -> Color? {
        guard case .field(let spec) = cell else { return nil }
        if store.form.visibleError(spec: spec) != nil { return theme.colors.error }
        return focused == spec.field ? theme.colors.primary : nil
    }

    @ViewBuilder
    private func cell(_ cell: Cell, edges: GroupCellEdges, markMandatory: Bool) -> some View {
        switch cell {
        case .addLine:
            GroupButtonCell(edges: edges, action: {
                let shown = Set(billing.map { $0.field })
                lineToFocus = [PayerField.billingAddressLine2, .billingAddressLine3].first { !shown.contains($0) }
                store.change { $0.addAddressLine() }
            }, content: {
                Text(text("payer_add_address_line"))
                    .line(theme.input, color: theme.colors.primary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                IconView(icon: .plus, size: 18, color: theme.colors.primary).frame(width: 28, height: 28)
            })
        case .field(let spec):
            if spec.field == PayerField.billingCountry {
                CountryCell(store: store, spec: spec, highlight: highlight(cell), edges: edges, markMandatory: markMandatory)
            } else {
                textCell(spec, edges: edges, highlight: highlight(cell), markMandatory: markMandatory)
            }
        }
    }

    private func textCell(_ spec: PayerFieldSpec, edges: GroupCellEdges, highlight: Color?, markMandatory: Bool) -> some View {
        let form = store.form
        let field = spec.field
        let editable = form.isEditable(spec: spec)
        let error = form.visibleError(spec: spec)
        let removable = field != PayerField.billingAddressLine1 && form.isAddressLine(field: field) && !spec.mandatory && editable
        return GroupTextCell(
            text: form.value(field: field),
            placeholder: form.groupLabel(spec: spec, markMandatory: markMandatory),
            floating: true,
            content: FieldContent(rawValue: form.contentType(field: field)) ?? .none,
            status: GroupFieldStatus(isError: error != nil, message: error),
            highlight: highlight,
            edges: edges,
            enabled: editable,
            autoFocus: field == lineToFocus,
            identifier: "payer_\(field.apiName)",
            onChange: { text in store.change { $0.setValue(field: field, text: text) } },
            kept: { store.form.value(field: field) },
            onFocus: { gained in
                if gained {
                    focused = field
                    if lineToFocus == field { lineToFocus = nil }
                } else if focused == field {
                    focused = nil
                }
                store.change { $0.focusChanged(field: field, focused: gained) }
            },
            trailing: {
                if removable {
                    Button(action: { store.change { $0.removeAddressLine(field: field) } }, label: {
                        IconView(icon: .close, size: 16, color: theme.colors.onSurfaceVariant)
                            .frame(width: 28, height: 28)
                            .contentShape(Rectangle())
                    })
                    .buttonStyle(PlainPress())
                    .accessibility(label: Text(text("payer_remove_address_line")))
                }
            }
        )
    }
}

/// A country as a cell of a group: it opens the list of countries.
struct CountryCell: View {
    @ObservedObject var store: PayerStore
    let spec: PayerFieldSpec
    var highlight: Color?
    var edges = GroupCellEdges()
    var markMandatory = false

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        let form = store.form
        let code = form.value(field: spec.field)
        let editable = form.isEditable(spec: spec)
        let label = form.groupLabel(spec: spec, markMandatory: markMandatory)
        GroupButtonCell(highlight: highlight, edges: edges, enabled: editable, identifier: "payer_\(spec.field.apiName)", action: {
            dismissKeyboard()
            store.picker = .country(spec.field)
        }, content: {
            GroupPickerContent(label: label, isError: form.visibleError(spec: spec) != nil, hasValue: !code.isEmpty) {
                FlagIcon(emoji: form.flag(iso: code)).pickerValueIcon().padding(.trailing, 4)
                Text(form.countryName(code: code)).line(theme.input, color: theme.colors.onBackground).truncationMode(.tail)
            }
            if editable {
                IconView(icon: .chevronDown, size: 20, color: theme.colors.onSurfaceVariant).frame(width: 28, height: 28)
            }
        })
        .accessibilityElement(children: .ignore)
        .accessibility(label: Text(code.isEmpty ? label : "\(label), \(form.countryName(code: code))"))
        .accessibility(addTraits: .isButton)
    }
}

/// How long one view of a foldable block fades for the other, as the Compose checkout.
let summaryFade = 0.14
