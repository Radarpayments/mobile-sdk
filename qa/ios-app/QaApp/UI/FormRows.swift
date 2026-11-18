import SwiftUI

/// A field of the form as free text; a problem with its text is shown under it.
struct FormText: View {
    @ObservedObject var form: ConfigForm
    let tag: String
    let label: String
    var keyboard: UIKeyboardType = .default
    var multiline = false

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            let binding = Binding(get: { form.text(tag) }, set: { form.edit(tag, $0) })
            Group {
                if multiline {
                    TextField(label, text: binding, axis: .vertical).lineLimit(3...8)
                } else {
                    TextField(label, text: binding)
                }
            }
            .textFieldStyle(.roundedBorder)
            .keyboardType(keyboard)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .accessibilityIdentifier(tag)
            FieldProblem(problem: form.problem(tag))
        }
    }
}

/// A field with a fixed set of values. Its accessibility value is the field's JSON value; "none" stands for an empty one.
struct FormChoice: View {
    @ObservedObject var form: ConfigForm
    let tag: String
    let label: String
    let options: [String]

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Choice(tag: tag, label: label, value: form.text(tag), options: options) { form.edit(tag, $0) }
            FieldProblem(problem: form.problem(tag))
        }
    }
}

/// A field that is true or false.
struct FormSwitch: View {
    @ObservedObject var form: ConfigForm
    let tag: String
    let label: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Toggle(label, isOn: Binding(get: { sameText(form.text(tag), "true") }, set: { form.edit(tag, String($0)) }))
                .accessibilityIdentifier(tag)
            FieldProblem(problem: form.problem(tag))
        }
    }
}

/// A labelled menu of values whose accessibility value is the chosen one.
struct Choice: View {
    let tag: String
    let label: String
    let value: String
    let options: [String]
    let onSelect: (String) -> Void

    var body: some View {
        Menu {
            ForEach(options, id: \.self) { option in
                Button(option.isEmpty ? "none" : option) { onSelect(option) }
            }
        } label: {
            HStack {
                Text(label).foregroundStyle(.primary)
                Spacer()
                Text(value.isEmpty ? "none" : value)
                Image(systemName: "chevron.up.chevron.down").font(.caption)
            }
            .contentShape(Rectangle())
        }
        .accessibilityIdentifier(tag)
        .accessibilityValue(value)
    }
}

struct FieldProblem: View {
    let problem: Problem?

    var body: some View {
        if let problem {
            Text(problem.message).font(.caption).foregroundStyle(.red)
        }
    }
}

/// A collapsible group of fields, expanded until the tester collapses it.
struct FormGroup<Content: View>: View {
    let tag: String
    let title: String
    @ViewBuilder let content: () -> Content
    @State private var expanded = true

    var body: some View {
        DisclosureGroup(isExpanded: $expanded) {
            VStack(alignment: .leading, spacing: 12) { content() }.padding(.top, 8)
        } label: {
            // On the label only: on the group it would replace the identifiers of the fields inside.
            Text(title).font(.headline).accessibilityIdentifier(tag)
        }
    }
}

/// `<field>: <message>`, the text of `qa_validation_error`.
func problemText(_ problem: Problem) -> String {
    "\(problem.field): \(problem.message)"
}
