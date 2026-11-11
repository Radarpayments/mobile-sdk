import SwiftUI

/// The payer fields the gateway asks for with this card: contacts, the billing address, the sides of a transfer.
struct PayerFieldsSection: View {
    @ObservedObject var store: PayerStore
    let paymentSystems: [String]
    var collapsibleBilling = false

    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        let sections = store.form.sections(paymentSystems: paymentSystems)
        VStack(spacing: 16) {
            ForEach(sections.contact, id: \.field.name) { spec in
                if spec.field == PayerField.mobilePhone {
                    PhoneField(store: store, spec: spec)
                } else {
                    PayerTextField(store: store, spec: spec)
                }
            }
            if !sections.billing.isEmpty {
                if collapsibleBilling && sections.billingCollapsible {
                    CollapsibleBillingGroup(store: store, billing: sections.billing, canAddLine: sections.canAddAddressLine)
                } else {
                    BillingGroup(store: store, billing: sections.billing, canAddLine: sections.canAddAddressLine)
                }
            }
            if !sections.sender.isEmpty {
                TransferGroup(store: store, title: strings?.text(key: "transfer_sender_title") ?? "", specs: sections.sender)
            }
            if !sections.recipient.isEmpty {
                TransferGroup(store: store, title: strings?.text(key: "transfer_recipient_title") ?? "", specs: sections.recipient)
            }
        }
    }
}

/// A payer field on its own line.
struct PayerTextField: View {
    @ObservedObject var store: PayerStore
    let spec: PayerFieldSpec

    @State private var focused = false

    var body: some View {
        let form = store.form
        let field = spec.field
        let error = form.visibleError(spec: spec)
        let editable = form.isEditable(spec: spec)
        FormField(label: form.label(spec: spec), message: error, isError: error != nil) {
            FieldBox(focused: focused, isError: error != nil, readOnly: !editable, valid: editable && form.isVisiblyValid(spec: spec)) {
                FormTextField(
                    text: form.value(field: field),
                    placeholder: field == PayerField.email ? "name@example.com" : form.baseLabel(field: field),
                    content: FieldContent(rawValue: form.contentType(field: field)) ?? .none,
                    enabled: editable,
                    label: form.label(spec: spec),
                    errorMessage: error,
                    identifier: "payer_\(field.apiName)",
                    onChange: { text in store.change { $0.setValue(field: field, text: text) } },
                    kept: { store.form.value(field: field) },
                    onFocus: { gained in
                        focused = gained
                        store.change { $0.focusChanged(field: field, focused: gained) }
                    }
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }
}

/// The phone number: the country's code as a button, then the number in the country's grouping.
struct PhoneField: View {
    @ObservedObject var store: PayerStore
    let spec: PayerFieldSpec

    @State private var focused = false
    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        let form = store.form
        let field = spec.field
        let error = form.visibleError(spec: spec)
        let editable = form.isEditable(spec: spec)
        FormField(label: form.label(spec: spec), message: error, isError: error != nil) {
            FieldBox(focused: focused, isError: error != nil, readOnly: !editable, valid: editable && form.isVisiblyValid(spec: spec)) {
                codeButton(editable: editable)
                theme.colors.outline.frame(width: 1, height: 22).padding(.horizontal, 6)
                FormTextField(
                    text: form.phoneText,
                    placeholder: form.phonePlaceholder,
                    content: .phone,
                    enabled: editable,
                    label: form.label(spec: spec),
                    errorMessage: error,
                    identifier: "payer_\(field.apiName)",
                    onChange: { text in store.change { $0.phoneInput(text: text) } },
                    kept: { store.form.phoneText },
                    onFocus: { gained in
                        focused = gained
                        store.change { $0.focusChanged(field: field, focused: gained) }
                    }
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }

    private func codeButton(editable: Bool) -> some View {
        let form = store.form
        let dialCode = form.phoneDialCode.map { PayerForms.shared.leftToRight(text: "+\($0)") }
        let code = dialCode ?? (strings?.text(key: "phone_code_choose") ?? "")
        return Button(action: {
            dismissKeyboard()
            store.picker = .phoneCode
        }, label: {
            HStack(spacing: 6) {
                if let iso = form.phoneIso {
                    FlagIcon(emoji: form.flag(iso: iso))
                }
                Text(code).line(theme.input.tabular(), color: form.phoneIso == nil ? theme.colors.placeholder : theme.colors.onBackground)
                    .padding(.top, pixelCentredTop(theme.input.singleLineHeight, in: fieldHeight))
                    .frame(height: fieldHeight, alignment: .top)
                if editable {
                    IconView(icon: .chevronDown, size: 16, color: theme.colors.onSurfaceVariant)
                }
            }
            .padding(.trailing, 6)
            .frame(maxHeight: .infinity)
            .contentShape(Rectangle())
        })
        .buttonStyle(PlainPress())
        .disabled(!editable)
        .accessibilityElement(children: .ignore)
        .accessibility(label: Text(strings?.format("phone_code_cd", code) ?? code))
        .accessibility(addTraits: .isButton)
    }
}
