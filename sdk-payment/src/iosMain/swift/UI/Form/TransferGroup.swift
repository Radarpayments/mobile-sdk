import SwiftUI

/// One side of a transfer as a frame of fields, a field per row.
struct TransferGroup: View {
    @ObservedObject var store: PayerStore
    let title: String
    let specs: [PayerFieldSpec]

    @State private var focused: PayerField?
    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        let form = store.form
        let someOptional = specs.contains { !$0.mandatory }
        let error = specs.compactMap { form.visibleError(spec: $0) }.first
        VStack(alignment: .leading, spacing: 6) {
            FieldGroupHeader(title: title) {
                if !specs.contains(where: { $0.mandatory }) {
                    Text(strings?.text(key: "payer_optional_note") ?? "").line(theme.caption, color: theme.colors.onSurfaceVariant)
                }
            }
            FieldGroupFrame {
                ForEach(specs.indices, id: \.self) { row in
                    if row > 0 { GroupRowDivider() }
                    GroupRow(highlighted: highlight(specs[row]) != nil) {
                        cell(specs[row], edges: .of(row: row, rows: specs.count), markMandatory: someOptional)
                    }
                }
            }
            if let error = error {
                FieldMessage(text: error)
            }
        }
    }

    private func highlight(_ spec: PayerFieldSpec) -> Color? {
        if store.form.visibleError(spec: spec) != nil { return theme.colors.error }
        return focused == spec.field ? theme.colors.primary : nil
    }

    @ViewBuilder
    private func cell(_ spec: PayerFieldSpec, edges: GroupCellEdges, markMandatory: Bool) -> some View {
        let form = store.form
        let field = spec.field
        let kind = form.kind(field: field)
        let error = form.visibleError(spec: spec)
        switch kind {
        case "country":
            CountryCell(store: store, spec: spec, highlight: highlight(spec), edges: edges, markMandatory: markMandatory)
        case "accountType", "idType":
            optionCell(spec, edges: edges, markMandatory: markMandatory)
        default:
            GroupTextCell(
                text: form.value(field: field),
                placeholder: form.groupLabel(spec: spec, markMandatory: markMandatory),
                hint: form.hint(field: field),
                floating: true,
                content: FieldContent(rawValue: form.contentType(field: field)) ?? .none,
                numberPad: kind == "date",
                status: GroupFieldStatus(isError: error != nil, message: error),
                highlight: highlight(spec),
                edges: edges,
                enabled: form.isEditable(spec: spec),
                identifier: "payer_\(field.apiName)",
                onChange: { text in store.change { $0.setValue(field: field, text: text) } },
                kept: { store.form.value(field: field) },
                onFocus: { gained in
                    if gained {
                        focused = field
                    } else if focused == field {
                        focused = nil
                    }
                    store.change { $0.focusChanged(field: field, focused: gained) }
                }
            )
        }
    }

    private func optionCell(_ spec: PayerFieldSpec, edges: GroupCellEdges, markMandatory: Bool) -> some View {
        let form = store.form
        let code = form.value(field: spec.field)
        let editable = form.isEditable(spec: spec)
        let label = form.groupLabel(spec: spec, markMandatory: markMandatory)
        return GroupButtonCell(
            highlight: highlight(spec), edges: edges, enabled: editable, identifier: "payer_\(spec.field.apiName)",
            action: {
                dismissKeyboard()
                store.picker = .option(spec.field)
            },
            content: {
                GroupPickerContent(label: label, isError: form.visibleError(spec: spec) != nil, hasValue: !code.isEmpty) {
                    Text(form.optionLabel(code: code)).line(theme.input, color: theme.colors.onBackground).truncationMode(.tail)
                }
                if editable {
                    IconView(icon: .chevronDown, size: 20, color: theme.colors.onSurfaceVariant).frame(width: 28, height: 28)
                }
            }
        )
        .accessibilityElement(children: .ignore)
        .accessibility(label: Text(code.isEmpty ? label : "\(label), \(form.optionLabel(code: code))"))
        .accessibility(addTraits: .isButton)
    }
}
