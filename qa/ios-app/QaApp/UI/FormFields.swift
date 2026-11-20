import SwiftUI

/// A free-text field of the form under its caption; its problem (or `problem` from the launch check) shows in red under
/// it, `help` in grey otherwise.
struct FormInput: View {
    @ObservedObject var form: ConfigForm
    let tag: String
    let caption: String
    var help: String?
    var problem: Problem?
    var look = InputLook()

    var body: some View {
        let shown = form.problem(tag) ?? problem
        Caption(caption)
        TextInput(text: form.text(tag), tag: tag, isProblem: shown != nil, look: look) { form.edit(tag, $0) }
        if let shown {
            Help(text: rowProblem(shown), isProblem: true)
        } else if let help {
            Help(text: help)
        }
    }
}

/// A field that is true or false, as a row of a list.
struct FormSwitch: View {
    @ObservedObject var form: ConfigForm
    let tag: String
    let label: String

    var body: some View {
        SwitchRow(label: label, isOn: sameText(form.text(tag), "true"), tag: tag) { form.edit(tag, $0 ? "true" : "false") }
    }
}

/// A choice of a few values as segments under a caption.
struct FormSegments: View {
    @ObservedObject var form: ConfigForm
    let tag: String
    let caption: String
    let options: [(value: String, label: String)]

    var body: some View {
        Caption(caption)
        Segmented(
            segments: options.map { Segment(value: $0.value, label: $0.label, tag: Tags.option(tag, $0.value)) },
            selected: form.text(tag),
            tag: tag
        ) { form.edit(tag, $0) }
        if let problem = form.problem(tag) {
            Help(text: rowProblem(problem), isProblem: true)
        }
    }
}

/// A one-of list: a row with a mark per value. The list carries `listTag` and reads as the chosen value.
struct FormMarks: View {
    @ObservedObject var form: ConfigForm
    let tag: String
    let options: [(value: String, label: String)]
    var listTag: String?
    var onChosen: () -> Void = {}

    var body: some View {
        let selected = form.text(tag)
        ListGroup(tag: listTag, value: selected) {
            ForEach(options, id: \.value) { option in
                MarkRow(label: option.label, selected: sameText(option.value, selected), tag: Tags.option(tag, option.value)) {
                    form.edit(tag, option.value)
                    onChosen()
                }
            }
        }
    }
}

/// A set of names as chips: the known ones, then the chosen ones that are not among them. The set carries `tag` and
/// reads as the names joined by commas.
struct FormChips<Extra: View>: View {
    @ObservedObject var form: ConfigForm
    let tag: String
    let known: [String]
    var chipTag: (String) -> String
    @ViewBuilder var extra: () -> Extra

    var body: some View {
        let text = form.text(tag)
        let chosen = namesFromText(text)
        let names = known + chosen.filter { name in !known.contains { sameText($0, name) } }
        ChipFlow {
            ForEach(names, id: \.self) { name in
                let on = chosen.contains { sameText($0, name) }
                Chip(text: name, on: on, tag: chipTag(name)) {
                    let next = on ? chosen.filter { !sameText($0, name) } : chosen + [name]
                    form.edit(tag, namesToText(next))
                }
            }
            extra()
        }
        .padding(.horizontal, QaSize.screen)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier(tag)
        .accessibilityValue(chosen.joined(separator: ","))
        if let problem = form.problem(tag) {
            Help(text: rowProblem(problem), isProblem: true)
        }
    }
}

extension FormChips where Extra == EmptyView {
    init(form: ConfigForm, tag: String, known: [String], chipTag: @escaping (String) -> String) {
        self.init(form: form, tag: tag, known: known, chipTag: chipTag, extra: { EmptyView() })
    }
}
