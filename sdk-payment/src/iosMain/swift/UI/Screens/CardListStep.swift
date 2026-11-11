import SwiftUI

/// All saved cards: pay with one, add a new one, or remove cards in edit mode.
struct CardListStep: View {
    let model: CheckoutModel
    let ui: CheckoutUi
    let form: PaymentStateCardFormShown

    @State private var editing = false
    @State private var removing: SavedCardInfo?
    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        let cards = form.savedCards
        let dark = theme.logosAreDark
        VStack(spacing: 0) {
            StepHeader(
                title: strings?.text(key: editing ? "card_list_edit_title" : "card_list_title") ?? "",
                onBack: model.pop,
                onClose: { model.handle(intent: PaymentIntentCancel.shared) }
            )
            .padding(.horizontal, sidePadding)
            .padding(.top, 2)
            .padding(.bottom, 8)
            StepScroll(chrome: headerHeight) { list(cards, dark: dark) }
            .stepBody()
        }
        .coveredFromVoiceOver(removing != nil)
        .sheetOverlay(id: removing?.bindingId, onDismiss: { removing = nil }) {
            if let card = removing {
                removal(card, name: SavedCardSpecs.shared.of(card: card, dark: dark).name)
            }
        }
    }

    private var headerHeight: CGFloat { 54 }

    private func list(_ cards: [SavedCardInfo], dark: Bool) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            sectionTitle(canEdit: cards.contains { $0.canDelete })
            if cards.isEmpty {
                LeadingParagraph(text: strings?.text(key: "card_list_empty") ?? "", style: theme.body, color: theme.colors.onSurfaceVariant)
            }
            VStack(spacing: 8) {
                ForEach(cards, id: \.bindingId) { card in
                    row(card, spec: SavedCardSpecs.shared.of(card: card, dark: dark))
                }
                if !editing {
                    NewCardRow { model.push(route: RouteNewCard.shared) }
                }
            }
        }
        .padding(.horizontal, sidePadding)
        .padding(.top, 6)
        .padding(.bottom, 20)
    }

    private func sectionTitle(canEdit: Bool) -> some View {
        HStack(spacing: 0) {
            Text(strings?.text(key: "sheet_saved_cards") ?? "")
                .line(theme.label.weight(theme.title.weight), color: theme.colors.onSurfaceVariant)
                .frame(maxWidth: .infinity, alignment: .leading)
            if canEdit {
                TextAction(text: strings?.text(key: editing ? "action_done" : "action_edit") ?? "") { editing.toggle() }
                    .offset(x: 8)
            }
        }
        .frame(height: 20)
        .offset(y: 6)
    }

    private func row(_ card: SavedCardInfo, spec: SavedCardSpec) -> some View {
        MethodRow(
            title: spec.title,
            subtitle: spec.subtitle,
            numericTitle: true,
            action: editing ? nil : { model.paySaved(card: card) },
            leading: { CardNetworkIcon(logo: spec.logo) },
            trailing: {
                if !editing {
                    ChevronMark().frame(height: 44)
                } else if card.canDelete {
                    IconAction(
                        icon: .trash,
                        label: "\(strings?.text(key: "card_list_delete") ?? "") \(spec.name)",
                        tint: theme.colors.error,
                        action: { removing = card }
                    )
                    .offset(x: 10)
                }
            }
        )
    }

    @ViewBuilder private func removal(_ card: SavedCardInfo, name: String) -> some View {
        LeadingParagraph(
            text: strings?.format("card_list_remove_title", name) ?? "",
            style: theme.headline,
            color: theme.colors.onBackground
        )
        LeadingParagraph(text: strings?.text(key: "card_list_remove_body") ?? "", style: theme.body, color: theme.colors.onSurfaceVariant)
            .padding(.bottom, 4)
        CheckoutButton(text: strings?.text(key: "card_list_delete") ?? "", kind: .danger) {
            model.handle(intent: PaymentIntentUnbindSavedCard(bindingId: card.bindingId))
            removing = nil
        }
        CheckoutButton(text: strings?.text(key: "card_list_keep") ?? "", kind: .secondary) { removing = nil }
    }
}
