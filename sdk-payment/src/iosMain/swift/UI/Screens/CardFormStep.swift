import SwiftUI

/// The step of a new card: the card's fields, whether to keep the card, and the payer data the gateway asks for.
struct CardFormStep: View {
    let model: CheckoutModel
    let ui: CheckoutUi
    let form: PaymentStateCardFormShown
    let isRoot: Bool

    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        if let strings = strings {
            CardFormContent(model: model, ui: ui, form: form, isRoot: isRoot, strings: strings)
        }
    }
}

private struct CardFormContent: View {
    let model: CheckoutModel
    let ui: CheckoutUi
    let form: PaymentStateCardFormShown
    let isRoot: Bool
    let strings: PaymentStrings

    @StateObject private var payer: PayerStore
    @State private var pan = ""
    @State private var expiry = ""
    @State private var cvc = ""
    @State private var saveCard: Bool
    @State private var touched: Set<CardDetailsGroup.Cell> = []
    @State private var hintShown = false
    /// Counts the scanner's openings: each one is a new camera screen with its own run.
    @State private var scans = 0
    @State private var scannerOpen = false
    @Environment(\.checkoutTheme) private var theme
    @Environment(\.cardScanner) private var scanner

    init(model: CheckoutModel, ui: CheckoutUi, form: PaymentStateCardFormShown, isRoot: Bool, strings: PaymentStrings) {
        self.model = model
        self.ui = ui
        self.form = form
        self.isRoot = isRoot
        self.strings = strings
        _payer = StateObject(wrappedValue: PayerStore(form: PayerForms.shared.of(request: form.payerData, card: nil, strings: strings)))
        _saveCard = State(initialValue: form.canSaveCard && (form.saveCardByDefault || form.saveCardLocked))
    }

    var body: some View {
        let number = CardEntry.shared.number(pan: pan, dark: theme.logosAreDark)
        FormScaffold(
            payLabel: strings.payLabel(order: ui.order, transfer: form.transfer, saveCardOnly: form.saveCardOnly),
            onPay: { pay(number) },
            hintText: strings.text(key: number.cvvHintKey),
            hintShown: $hintShown,
            header: {
                StepHeader(
                    title: strings.text(key: "card_form_title"),
                    onBack: isRoot ? nil : { leave(model.pop) },
                    onClose: { leave { model.handle(intent: PaymentIntentCancel.shared) } }
                )
            },
            content: {
                VStack(spacing: 8) {
                    CardDetailsGroup(
                        pan: $pan, expiry: $expiry, cvc: $cvc, number: number, status: { status($0, number) }, hintShown: $hintShown,
                        onScan: scanner == nil ? nil : openScanner
                    )
                    if form.canSaveCard {
                        CheckboxRow(
                            title: strings.text(key: "card_form_save_card"),
                            description: strings.text(key: "card_form_save_card_hint"),
                            checked: saveCard,
                            onChange: form.saveCardLocked ? nil : { saveCard = $0 }
                        )
                    }
                }
                PayerFieldsSection(store: payer, paymentSystems: number.paymentSystems, collapsibleBilling: true)
            }
        )
        .accessibility(identifier: "CardFormScreen")
        .coveredFromVoiceOver(payer.picker != nil)
        .scannerCover(scannerCover)
        .payerPickers(store: payer)
        .onChange(of: pan) { _ in
            if CardEntry.shared.number(pan: pan, dark: false).complete { touched.insert(.pan) }
        }
        .onChange(of: expiry) { if $0.count == 4 { touched.insert(.expiry) } }
        .onChange(of: cvc) { if $0.count == Int(number.cvvLength) { touched.insert(.cvc) } }
    }

    private func errorKey(_ cell: CardDetailsGroup.Cell, _ number: CardNumberSpec) -> String? {
        switch cell {
        case .pan: return CardEntry.shared.panError(pan: pan)
        case .expiry: return CardEntry.shared.expiryError(expiry: expiry)
        case .cvc: return CardEntry.shared.cvvError(cvc: cvc, length: number.cvvLength)
        }
    }

    private func status(_ cell: CardDetailsGroup.Cell, _ number: CardNumberSpec) -> GroupFieldStatus {
        guard touched.contains(cell) else { return GroupFieldStatus() }
        let error = errorKey(cell, number).map { strings.text(key: $0) }
        return GroupFieldStatus(isError: error != nil, isValid: error == nil, message: error)
    }

    private var scannerCover: ScannerCoverItem? {
        guard scannerOpen, let scanner = scanner else { return nil }
        return ScannerCoverItem(id: scans, scanner: scanner, onResult: scanned, onClose: { scannerOpen = false })
    }

    private func openScanner() {
        dismissKeyboard()
        scans += 1
        scannerOpen = true
    }

    private func scanned(_ result: CardScannerResult) {
        if let success = result as? CardScannerResultSuccess {
            pan = success.card.pan
            expiry = String(format: "%02d%02d", Int(success.card.expiryMonth), Int(success.card.expiryYear) % 100)
            touched.formUnion([.pan, .expiry])
        }
        scannerOpen = false
    }

    private func leave(_ action: () -> Void) {
        dismissKeyboard()
        action()
    }

    private func pay(_ number: CardNumberSpec) {
        touched = [.pan, .expiry, .cvc]
        let specs = payer.form.sections(paymentSystems: number.paymentSystems).all
        let payerValid = payer.validateAll(specs)
        let cardValid = [CardDetailsGroup.Cell.pan, .expiry, .cvc].allSatisfy { errorKey($0, number) == nil }
        guard cardValid, payerValid else { return }
        dismissKeyboard()
        model.handle(intent: CardEntry.shared.submitNewCard(
            pan: pan, expiry: expiry, cvc: cvc, saveCard: form.canSaveCard && saveCard, payerData: payer.form.submitValues(specs: specs)
        ))
    }
}
