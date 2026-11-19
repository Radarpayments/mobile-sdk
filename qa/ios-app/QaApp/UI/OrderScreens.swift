import SwiftUI

/// The order: where it comes from and, for a registered one, its type, amount, features and optional parameters.
struct OrderScreen: View {
    @ObservedObject var session: QaSession
    @ObservedObject var form: ConfigForm
    let back: () -> Void
    let go: (Route) -> Void

    var body: some View {
        let order = form.config.order
        let problems = launchProblems(config: form.config, profile: session.selectedProfile)
        let launchProblem = { (field: String) in problems.first { sameText($0.field, field) } }
        QaScreen(title: "Order", back: back) {
            FormSegments(form: form, tag: Tags.orderSource, caption: "Source", options: [
                ("register", "Register"), ("mdOrder", "mdOrder"), ("sessionId", "Session id"),
            ])
            switch order.source {
            case .register:
                FormSegments(form: form, tag: Tags.orderType, caption: "Type", options: [("payment", "Payment"), ("aft", "AFT")])
                if let problem = launchProblem("order.type") {
                    Help(text: rowProblem(problem), isProblem: true)
                }
                AmountField(form: form)
                FeaturesField(form: form)
                Caption("Optional")
                ListGroup {
                    NavRow(label: "Client id", value: clientIdSummary(order), tag: Tags.orderClientIdOpen) {
                        go(.field(Tags.orderClientId))
                    }
                    NavRow(label: "Extra parameters", value: extraSummary(order), tag: Tags.orderExtraOpen) {
                        go(.field(Tags.orderExtra))
                    }
                }
            case .mdOrder:
                IdField(
                    form: form,
                    tag: Tags.orderMdOrder,
                    pasteTag: Tags.orderMdOrderPaste,
                    caption: "mdOrder",
                    hint: "Enter the order's id",
                    missing: launchProblem("order.mdOrder") != nil
                )
                Note("The app does not register this order: type, amount and features are not used. The order's status is read only with an API v1 stand.")
            case .sessionId:
                IdField(
                    form: form,
                    tag: Tags.orderSessionId,
                    pasteTag: Tags.orderSessionIdPaste,
                    caption: "Session id",
                    hint: "Enter the session's id",
                    missing: launchProblem("order.sessionId") != nil
                )
                Note("The app does not register this session: type, amount and features are not used.")
            }
        }
    }
}

/// The amount in major units with the currency menu inside the field; the helper names the minor units sent.
private struct AmountField: View {
    @ObservedObject var form: ConfigForm

    var body: some View {
        let problem = form.problem(Tags.orderAmount) ?? form.problem(Tags.orderCurrency)
        let currency = form.text(Tags.orderCurrency)
        Caption("Amount")
        TextInput(
            text: form.text(Tags.orderAmount),
            tag: Tags.orderAmount,
            isProblem: problem != nil,
            look: InputLook(keyboard: .decimalPad),
            onChange: { form.edit(Tags.orderAmount, $0) },
            trailing: {
                FieldUnit(text: currency, tag: Tags.orderCurrency) {
                    ForEach(Currencies.codes, id: \.self) { code in
                        Button { form.edit(Tags.orderCurrency, code) } label: {
                            if sameText(code, currency) {
                                Label(code, systemImage: "checkmark")
                            } else {
                                Text(code)
                            }
                        }
                        .accessibilityIdentifier(Tags.option(Tags.orderCurrency, code))
                    }
                }
            }
        )
        if let problem {
            Help(text: rowProblem(problem), isProblem: true)
        } else {
            let amount = form.config.order.amount
            Help(text: amount == 1 ? "1 minor unit" : "\(amount) minor units")
        }
    }
}

private let knownFeatures = ["FORCE_TDS", "FORCE_SSL", "VERIFY", "FORCE_CREATE_BINDING", "AUTO_PAYMENT"]

/// The features as chips, with + Other for a name the list does not have.
private struct FeaturesField: View {
    @ObservedObject var form: ConfigForm
    @State private var adding = false
    @State private var name = ""

    var body: some View {
        Caption("Features")
        FormChips(form: form, tag: Tags.orderFeatures, known: knownFeatures, chipTag: Tags.feature) {
            Chip(text: "+ Other", on: adding, tag: Tags.orderFeaturesOther) { adding.toggle() }
        }
        if adding {
            TextInput(
                text: name,
                tag: Tags.orderFeaturesOtherName,
                look: InputLook(placeholder: "FEATURE_NAME", style: QaType.inputSmall),
                onChange: { name = $0 },
                trailing: { QaLink(text: "Add", tag: Tags.orderFeaturesOtherAdd, action: add) }
            )
            .padding(.top, QaSize.gap)
        }
    }

    private func add() {
        let chosen = namesFromText(form.text(Tags.orderFeatures))
        let new = trimmed(name)
        if !new.isEmpty, !chosen.contains(where: { sameText($0, new) }) {
            form.edit(Tags.orderFeatures, namesToText(chosen + [new]))
        }
        name = ""
        adding = false
    }
}

/// The id of an order or a session to open without registering one, with Paste.
private struct IdField: View {
    @ObservedObject var form: ConfigForm
    let tag: String
    let pasteTag: String
    let caption: String
    let hint: String
    let missing: Bool

    var body: some View {
        let problem = form.problem(tag)
        Caption(caption)
        TextInput(
            text: form.text(tag),
            tag: tag,
            isProblem: problem != nil || missing,
            look: InputLook(placeholder: "01ea8a09-9d44-…", style: QaType.inputSmall),
            onChange: { form.edit(tag, $0) },
            trailing: {
                QaLink(text: "Paste", tag: pasteTag) { form.edit(tag, trimmed(UIPasteboard.general.string ?? "")) }
            }
        )
        if let problem {
            Help(text: rowProblem(problem), isProblem: true)
        } else {
            Help(text: hint, isProblem: missing)
        }
    }
}

/// One optional field on a screen of its own: the client id or the extra parameters.
struct FieldScreen: View {
    @ObservedObject var form: ConfigForm
    let tag: String
    let back: () -> Void

    var body: some View {
        let extra = sameText(tag, Tags.orderExtra)
        let title = extra ? "Extra parameters" : "Client id"
        QaScreen(title: title, back: back) {
            FormInput(
                form: form,
                tag: tag,
                caption: title,
                help: extra
                    ? "A JSON object added to the registration request; null leaves a parameter out"
                    : "Sent as clientId, as merchantCustomerId with API v2; empty sends none",
                look: InputLook(singleLine: !extra, style: QaType.inputSmall)
            )
        }
    }
}
