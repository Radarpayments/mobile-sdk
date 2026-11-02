import SwiftUI
import UIKit

/// The text fields of the form by field, so that tapping anywhere in a cell can focus its field.
final class MaskedFieldRegistry {
    var fields: [CoreField: UITextField] = [:]

    func focus(_ field: CoreField) { fields[field]?.becomeFirstResponder() }
}

/// A `UITextField` that applies [format] to every edit (typing, deleting, pasting, AutoFill) before it shows.
/// SwiftUI's `TextField` cannot do this reliably: when a change formats back to the value it already had (one
/// digit too many), the field is not redrawn and keeps showing what was typed.
struct MaskedTextField: UIViewRepresentable {
    let field: CoreField
    @Binding var text: String
    /// The whole new text as typed → the text to show.
    let format: (String) -> String
    var keyboard: UIKeyboardType = .numberPad
    var capitalization: UITextAutocapitalizationType = .none
    var contentType: UITextContentType?
    let textColor: UIColor
    let tint: UIColor
    var registry: MaskedFieldRegistry?
    var onFocus: (Bool) -> Void = { _ in }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> UITextField {
        let view = UITextField()
        view.delegate = context.coordinator
        view.font = Self.font
        view.keyboardType = keyboard
        view.autocapitalizationType = capitalization
        view.autocorrectionType = .no
        view.spellCheckingType = .no
        view.textContentType = contentType
        view.accessibilityIdentifier = "core_field_\(field.rawValue)"
        view.returnKeyType = .done
        // The number pad has no return key: "Done" above the keys (SwiftUI's keyboard toolbar skips UIKit fields).
        let bar = UIToolbar()
        bar.items = [
            UIBarButtonItem(systemItem: .flexibleSpace),
            UIBarButtonItem(title: String(localized: "Done", bundle: AppLanguage.bundle), style: .done, target: view, action: #selector(UIResponder.resignFirstResponder)),
        ]
        bar.items?.last?.accessibilityIdentifier = "core_keyboard_done"
        bar.sizeToFit()
        view.inputAccessoryView = bar
        view.setContentHuggingPriority(.defaultLow, for: .horizontal)
        view.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        registry?.fields[field] = view
        return view
    }

    func updateUIView(_ view: UITextField, context: Context) {
        context.coordinator.parent = self
        if view.text != text { view.text = text }
        view.textColor = textColor
        view.tintColor = tint
        registry?.fields[field] = view
    }

    /// Inter with even digits: the same font as the placeholder drawn under the field.
    static let font: UIFont = {
        let base = UIFont(name: "Inter", size: 15) ?? .systemFont(ofSize: 15)
        let even = base.fontDescriptor.addingAttributes([
            .featureSettings: [[UIFontDescriptor.FeatureKey.type: kNumberSpacingType,
                                UIFontDescriptor.FeatureKey.selector: kMonospacedNumbersSelector]],
        ])
        return UIFont(descriptor: even, size: 15)
    }()

    final class Coordinator: NSObject, UITextFieldDelegate {
        var parent: MaskedTextField

        init(_ parent: MaskedTextField) { self.parent = parent }

        func textField(_ view: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
            let current = view.text ?? ""
            guard let swiftRange = Range(range, in: current) else { return false }
            let typed = current.replacingCharacters(in: swiftRange, with: string)
            let formatted = parent.format(typed)
            // Keep the cursor after the same number of letters and digits as in what was typed.
            let typedPrefix = String(typed.prefix(range.location + (string as NSString).length))
            view.text = formatted
            var offset = typedPrefix.count
            if formatted != typed {
                let significant = typedPrefix.filter { $0.isLetter || $0.isNumber }.count
                var seen = 0
                offset = significant == 0 ? 0 : formatted.count
                for (i, char) in formatted.enumerated() where char.isLetter || char.isNumber {
                    seen += 1
                    if seen == significant { offset = i + 1; break }
                }
            }
            if let position = view.position(from: view.beginningOfDocument, offset: min(offset, formatted.count)) {
                view.selectedTextRange = view.textRange(from: position, to: position)
            }
            parent.text = formatted
            return false
        }

        func textFieldShouldReturn(_ view: UITextField) -> Bool {
            view.resignFirstResponder()
            return false
        }

        func textFieldDidBeginEditing(_ view: UITextField) { parent.onFocus(true) }

        func textFieldDidEndEditing(_ view: UITextField) { parent.onFocus(false) }
    }
}
