import SwiftUI

/// The checkout over the merchant's screen: the scrim, and the sheet the steps change in.
struct CheckoutScreen: View {
    let model: CheckoutModel
    let ui: CheckoutUi

    @State private var sheetHeight: CGFloat = 0
    @State private var gone = false

    var body: some View {
        let step = ui.step
        let reduceMotion = ui.reduceMotion
        ZStack(alignment: .bottom) {
            if ui.sheetPresentation {
                Scrim(reduceMotion: reduceMotion, onTap: model.scrimTapped)
                    .opacity(gone ? 0 : 1)
            }
            SheetHost(
                draggable: step.takesDrag,
                showGrabber: !(step is StepThreeDs),
                expanded: step is StepThreeDs,
                reduceMotion: reduceMotion,
                onDismiss: model.sheetDismissed,
                onExpanded: model.sheetExpanded,
                onSheetHeight: { sheetHeight = $0 }
            ) { entered in
                StepContainer(
                    key: CheckoutStepKt.key(step),
                    item: step,
                    forward: ui.forward,
                    revealKey: ui.revealKey,
                    zoomReveal: ui.zoomReveal,
                    blurs: { CheckoutStepKt.blursWhileChanging($0, revealKey: ui.revealKey) },
                    reduceMotion: reduceMotion,
                    expanded: step is StepThreeDs,
                    sheetEntered: entered
                ) { shown in
                    CheckoutStepView(model: model, ui: ui, step: shown)
                }
                .primaryButtonLayer(forward: ui.forward)
            }
            .offset(y: gone && !reduceMotion ? max(sheetHeight, 1) : 0)
            .opacity(gone && reduceMotion ? 0 : 1)
        }
        .sheetOverlayHost(reduceMotion: reduceMotion)
        .scannerCoverHost(reduceMotion: reduceMotion)
        .onChange(of: ui.shown) { shown in
            guard !shown else { return }
            let duration = reduceMotion ? SheetMotion.reducedSheetCloseDuration : SheetMotion.sheetCloseDuration
            withAnimation(sheetCloseAnimation(reduceMotion)) { gone = true }
            DispatchQueue.main.asyncAfter(deadline: .now() + duration) { model.exitFinished() }
        }
    }
}

private extension Step {
    var takesDrag: Bool { !(self is StepProcessing) && !(self is StepThreeDs) }
}

/// One step's content. Each screen is ported into its own file; until then the step says which one it is.
struct CheckoutStepView: View {
    let model: CheckoutModel
    let ui: CheckoutUi
    let step: Step

    var body: some View {
        switch onEnum(of: step) {
        case .loading:
            CheckoutSkeleton(model: model, ui: ui)
        case .form(let form):
            switch onEnum(of: form.route) {
            case .methods: PaymentMethodsStep(model: model, ui: ui, form: form.form)
            case .newCard: CardFormStep(model: model, ui: ui, form: form.form, isRoot: form.isRoot)
            case .allCards: CardListStep(model: model, ui: ui, form: form.form)
            case .saved(let saved): SavedCardStep(model: model, ui: ui, form: form.form, card: saved.card)
            }
        case .processing(let processing):
            ProcessingStepView(step: processing.step, order: ui.order, reduceMotion: ui.reduceMotion)
        case .threeDs:
            ThreeDsStepView(order: ui.order, reduceMotion: ui.reduceMotion) { model.handle(intent: PaymentIntentCancel.shared) }
        case .result(let result):
            ResultStepHost(model: model, ui: ui, completed: result.completed)
        }
    }
}

private struct StepPlaceholder: View {
    let name: String

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        Text(name)
            .checkoutStyle(theme.title)
            .foregroundColor(theme.colors.onBackground)
            .frame(maxWidth: .infinity, minHeight: 200)
            .padding(.horizontal, sidePadding)
    }
}
