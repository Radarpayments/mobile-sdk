import SwiftUI

let sheetItemSpacing: CGFloat = 14
private let walletSlotHeight: CGFloat = 52

/// The first step when there is a wallet or a saved card: the ways to pay.
struct PaymentMethodsStep: View {
    let model: CheckoutModel
    let ui: CheckoutUi
    let form: PaymentStateCardFormShown

    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        VStack(spacing: sheetItemSpacing) {
            StepHeader(title: strings?.text(key: "checkout_title") ?? "", onClose: { model.handle(intent: PaymentIntentCancel.shared) })
            VStack(spacing: sheetItemSpacing) {
                PaymentMethodsBody(
                    wallet: ui.wallets.offeredIn(form: form).any,
                    order: ui.order,
                    transfer: ui.transfer,
                    saveCardOnly: ui.saveCardOnly,
                    savedCards: form.savedCards,
                    onApplePay: { model.handle(intent: PaymentIntentTapApplePay.shared) },
                    onPaySaved: model.paySaved,
                    onNewCard: { model.push(route: RouteNewCard.shared) },
                    onAllCards: { model.push(route: RouteAllCards.shared) },
                    opensCardStep: model.opensCardStep
                )
            }
            .stepBody()
        }
        .padding(.horizontal, sidePadding)
    }
}

/// The methods under the header; the skeleton draws the same body as blocks.
struct PaymentMethodsBody: View {
    let wallet: Bool
    let order: OrderSummary?
    var transfer = false
    var saveCardOnly = false
    let savedCards: [SavedCardInfo]
    var onApplePay: () -> Void = {}
    var onPaySaved: (SavedCardInfo) -> Void = { _ in }
    var onNewCard: () -> Void = {}
    var onAllCards: () -> Void = {}
    var opensCardStep: (SavedCardInfo) -> Bool = { _ in false }

    @State private var chosenId: String?
    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings
    @Environment(\.isPlaceholder) private var isPlaceholder
    @Environment(\.stepPhase) private var phase

    var body: some View {
        if wallet {
            walletSlot
            LabeledDivider(text: strings?.text(key: "sheet_or_pay_by_card") ?? "")
        }
        if savedCards.isEmpty {
            CheckoutButton(text: strings?.text(key: "sheet_pay_by_card") ?? "", primary: true, action: onNewCard)
        } else {
            cards
        }
    }

    @ViewBuilder private var walletSlot: some View {
        if isPlaceholder {
            PlaceholderBlock(corner: theme.buttonCorner).frame(maxWidth: .infinity).frame(height: walletSlotHeight)
        } else {
            ApplePayButton(action: onApplePay).frame(maxWidth: .infinity).frame(height: walletSlotHeight)
        }
    }

    private var selected: SavedCardInfo? {
        savedCards.first { $0.bindingId == chosenId } ?? savedCards.first
    }

    @ViewBuilder private var cards: some View {
        let dark = theme.logosAreDark
        let limit = Int(SkeletonLayoutKt.SHEET_CARD_LIMIT)
        let selected = self.selected
        HStack(spacing: 0) {
            Text(strings?.text(key: "sheet_saved_cards") ?? "")
                .line(theme.label.weight(theme.title.weight), color: theme.colors.onSurfaceVariant)
                .placeholderText()
                .frame(maxWidth: .infinity, alignment: .leading)
            if savedCards.count > limit || savedCards.contains(where: { $0.canDelete }) {
                TextAction(text: strings?.text(key: "sheet_all_cards") ?? "", action: onAllCards).offset(x: 8)
            }
        }
        .frame(height: 20)
        VStack(spacing: 8) {
            ForEach(Array(savedCards.prefix(limit)), id: \.bindingId) { card in
                let spec = SavedCardSpecs.shared.of(card: card, dark: dark)
                let isSelected = card.bindingId == selected?.bindingId
                MethodRow(
                    title: spec.title,
                    subtitle: spec.subtitle,
                    numericTitle: true,
                    selected: isSelected,
                    isChoice: true,
                    action: { chosenId = card.bindingId },
                    leading: { CardNetworkIcon(logo: spec.logo) },
                    trailing: { RadioMark(selected: isSelected) }
                )
            }
            NewCardRow(action: onNewCard)
        }
        let toCardStep = selected.flatMap { opensCardStep($0) ? $0 : nil }
        PayButton(
            label: toCardStep.map { card in
                let mask = SavedCardSpecs.shared.of(card: card, dark: dark).title
                return strings?.format("sheet_pay_with_card", SavedCardSpecs.shared.leftToRight(text: mask)) ?? ""
            } ?? strings?.payLabel(order: order, transfer: transfer, saveCardOnly: saveCardOnly) ?? "",
            enabled: selected != nil,
            showLock: toCardStep == nil,
            action: { if let card = selected { onPaySaved(card) } }
        )
    }
}

struct NewCardRow: View {
    let action: () -> Void

    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        MethodRow(
            title: strings?.text(key: "card_list_add_new") ?? "",
            subtitle: strings?.text(key: "sheet_new_card_hint"),
            action: action,
            leading: { AddCardPlate() },
            trailing: { ChevronMark() }
        )
    }
}
