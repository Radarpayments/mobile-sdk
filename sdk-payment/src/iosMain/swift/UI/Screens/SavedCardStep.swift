import SwiftUI

/// The step of a saved card that still needs something from the payer: its security code, or the payer data.
struct SavedCardStep: View {
    let model: CheckoutModel
    let ui: CheckoutUi
    let form: PaymentStateCardFormShown
    let card: SavedCardInfo

    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        if let strings = strings {
            SavedCardContent(model: model, ui: ui, form: form, card: card, strings: strings)
        }
    }
}

private struct SavedCardContent: View {
    let model: CheckoutModel
    let ui: CheckoutUi
    let form: PaymentStateCardFormShown
    let card: SavedCardInfo
    let strings: PaymentStrings

    @StateObject private var payer: PayerStore
    @State private var cvc = ""
    @State private var touched = false
    @State private var focused = false
    @State private var hintShown = false
    @Environment(\.checkoutTheme) private var theme

    init(model: CheckoutModel, ui: CheckoutUi, form: PaymentStateCardFormShown, card: SavedCardInfo, strings: PaymentStrings) {
        self.model = model
        self.ui = ui
        self.form = form
        self.card = card
        self.strings = strings
        _payer = StateObject(wrappedValue: PayerStore(form: PayerForms.shared.of(request: form.payerData, card: card, strings: strings)))
    }

    var body: some View {
        let number = CardEntry.shared.savedNumber(card: card, dark: theme.logosAreDark)
        let error = touched ? errorKey(number).map { strings.text(key: $0) } : nil
        FormScaffold(
            footerOverKeyboard: true,
            payLabel: strings.payLabel(order: ui.order, transfer: false, saveCardOnly: false),
            onPay: { pay(number) },
            hintText: strings.text(key: number.cvvHintKey),
            hintShown: $hintShown,
            header: {
                StepHeader(
                    title: strings.text(key: "saved_card_title"),
                    onBack: { leave(model.pop) },
                    onClose: { leave { model.handle(intent: PaymentIntentCancel.shared) } }
                )
            },
            content: {
                VStack(alignment: .leading, spacing: 6) {
                    FieldGroupFrame {
                        if form.cvcRequired {
                            cvcRow(number, error: error)
                        } else {
                            cardRow(number)
                        }
                    }
                    if let error = error {
                        FieldMessage(text: error)
                    }
                }
                PayerSummarySection(store: payer, paymentSystems: number.paymentSystems)
            }
        )
        .accessibility(identifier: "SavedCardScreen")
        .coveredFromVoiceOver(payer.picker != nil)
        .payerPickers(store: payer)
    }

    private func cvcRow(_ number: CardNumberSpec, error: String?) -> some View {
        let highlight: Color? = error != nil ? theme.colors.error : focused ? theme.colors.primary : nil
        return GroupSplitRow(highlighted: highlight != nil, weights: (3, 2), leading: {
                GroupCell(highlight: error != nil ? theme.colors.error : nil, edges: .of(row: 0, rows: 1, column: 0, columns: 2)) {
                    CardNetworkIcon(logo: number.logo)
                    Color.clear.frame(width: 6, height: 1)
                    cardLines.frame(maxWidth: .infinity, alignment: .leading)
                }
        }, trailing: {
                GroupTextCell(
                    text: cvc,
                    placeholder: strings.text(key: "card_form_cvv_label"),
                    content: .cvc,
                    status: GroupFieldStatus(isError: error != nil, isValid: touched && error == nil, message: error),
                    highlight: highlight,
                    edges: .of(row: 0, rows: 1, column: 1, columns: 2),
                    autoFocus: true,
                    onChange: { text in
                        cvc = CardEntry.shared.cvcDigits(text: text, length: number.cvvLength)
                        if cvc.count == Int(number.cvvLength) { touched = true }
                    },
                    kept: { cvc },
                    onFocus: { focused = $0 },
                    trailing: { CvcHintButton(shown: $hintShown) }
                )
        })
    }

    private var cardLines: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(PayerForms.shared.leftToRight(text: CardEntry.shared.shortMask(maskedPan: card.maskedPan)))
                .line(theme.body.weight(theme.title.weight).tabular(), color: theme.colors.onBackground)
            if let expiry = card.expiry {
                Text(PayerForms.shared.leftToRight(text: expiry)).line(theme.caption.tabular(), color: theme.colors.onSurfaceVariant)
            }
        }
    }

    /// The card as a plain row inside the frame, when only the payer data is missing.
    private func cardRow(_ number: CardNumberSpec) -> some View {
        let spec = SavedCardSpecs.shared.of(card: card, dark: theme.logosAreDark)
        return HStack(spacing: 12) {
            CardNetworkIcon(logo: number.logo)
            VStack(alignment: .leading, spacing: 0) {
                Text(PayerForms.shared.leftToRight(text: spec.title))
                    .line(theme.body.weight(theme.title.weight).tabular(), color: theme.colors.onBackground)
                if let subtitle = spec.subtitle {
                    Text(subtitle).line(theme.caption, color: theme.colors.onSurfaceVariant)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .frame(minHeight: 60)
    }

    private func errorKey(_ number: CardNumberSpec) -> String? {
        form.cvcRequired ? CardEntry.shared.cvvError(cvc: cvc, length: number.cvvLength) : nil
    }

    private func leave(_ action: () -> Void) {
        dismissKeyboard()
        action()
    }

    private func pay(_ number: CardNumberSpec) {
        touched = true
        let specs = payer.form.sections(paymentSystems: number.paymentSystems).all
        let payerValid = payer.validateAll(specs)
        guard errorKey(number) == nil, payerValid else { return }
        dismissKeyboard()
        model.handle(intent: CardEntry.shared.submitSavedCard(
            card: card, cvc: form.cvcRequired ? cvc : nil, payerData: payer.form.submitValues(specs: specs)
        ))
    }
}
