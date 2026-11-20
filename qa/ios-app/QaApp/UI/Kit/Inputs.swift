import SwiftUI

/// How an input looks and types: the placeholder shown in grey while it is empty, one line or several, a secret.
struct InputLook {
    var placeholder = ""
    var singleLine = true
    var keyboard = UIKeyboardType.default
    var style = QaType.input
    var hidden = false

    static let small = InputLook(style: QaType.inputSmall)
}

/// A text input in a `FieldFrame`: monospace, as `look` says, with `trailing` on the right (a unit, Paste, Show). The
/// tag goes on the editable text itself, so a test reads and types the value through it.
struct TextInput<Trailing: View>: View {
    let text: String
    let tag: String
    var isProblem = false
    var look = InputLook()
    let onChange: (String) -> Void
    @ViewBuilder let trailing: () -> Trailing
    @FocusState private var focused: Bool

    var body: some View {
        FieldFrame(focused: focused, isProblem: isProblem, minHeight: look.singleLine ? QaSize.field : 104) {
            HStack(alignment: look.singleLine ? .center : .top, spacing: QaSize.screen) {
                field
                    .font(look.style.font)
                    .foregroundStyle(QaColor.ink)
                    .tint(QaColor.ink)
                    .keyboardType(look.keyboard)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .focused($focused)
                    .padding(.vertical, 14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityIdentifier(tag)
                HStack(spacing: 0, content: trailing).padding(.trailing, QaSize.screen - 8)
            }
            .padding(.leading, QaSize.screen)
        }
    }

    @ViewBuilder
    private var field: some View {
        let binding = Binding(get: { text }, set: onChange)
        let prompt = Text(look.placeholder).foregroundColor(QaColor.muted)
        if look.hidden {
            SecureField("", text: binding, prompt: prompt)
        } else if look.singleLine {
            TextField("", text: binding, prompt: prompt)
        } else {
            TextField("", text: binding, prompt: prompt, axis: .vertical).lineLimit(3...)
        }
    }
}

extension TextInput where Trailing == EmptyView {
    init(text: String, tag: String, isProblem: Bool = false, look: InputLook = InputLook(), onChange: @escaping (String) -> Void) {
        self.init(text: text, tag: tag, isProblem: isProblem, look: look, onChange: onChange, trailing: { EmptyView() })
    }
}

/// The unit inside a field that opens a picker, such as the currency: the code and a small chevron. It carries the tag
/// and reads as the code, so the field's identifier keeps carrying the value.
struct FieldUnit<Items: View>: View {
    let text: String
    let tag: String
    @ViewBuilder let items: () -> Items

    var body: some View {
        Menu(content: items) {
            HStack(spacing: 6) {
                Text(text).qaStyle(QaType.label).foregroundStyle(QaColor.ink)
                Chevron(direction: .down, size: 7, stroke: 1.5, color: QaColor.ink)
            }
            .frame(minWidth: QaSize.control, minHeight: QaSize.control)
            .contentShape(Rectangle())
        }
        .accessibilityIdentifier(tag)
        .accessibilityValue(text)
    }
}
