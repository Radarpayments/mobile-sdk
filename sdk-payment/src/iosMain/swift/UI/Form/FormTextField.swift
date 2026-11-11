import SwiftUI
import UIKit

/// What a field holds: it picks the keyboard, the autofill hint, the direction and the figures.
enum FieldContent: String {
    case pan = "CREDIT_CARD_NUMBER"
    case expiry = "CREDIT_CARD_EXPIRATION"
    case cvc = "CREDIT_CARD_SECURITY_CODE"
    case holder = "CREDIT_CARD_HOLDER"
    case email = "EMAIL"
    case phone = "PHONE"
    case addressLine = "ADDRESS_LINE"
    case city = "CITY"
    case state = "STATE"
    case postalCode = "POSTAL_CODE"
    case none = "NONE"

    var isCard: Bool { self == .pan || self == .expiry || self == .cvc }

    /// Latin by nature: typed and shown left to right in a right-to-left layout too.
    var isLatin: Bool { self != .state && self != .none }

    var textContentType: UITextContentType? {
        switch self {
        case .pan: return .creditCardNumber
        case .expiry: return UITextContentType(rawValue: "creditCardExpiration")
        case .cvc: return UITextContentType(rawValue: "creditCardSecurityCode")
        case .holder: return .name
        case .email: return .emailAddress
        case .phone: return .telephoneNumber
        case .addressLine: return .fullStreetAddress
        case .city: return .addressCity
        case .state: return .addressState
        case .postalCode: return .postalCode
        case .none: return nil
        }
    }
}

/// The checkout's text field. The caller owns the text: after every edit the field shows what the caller kept of it.
struct FormTextField: UIViewRepresentable {
    let text: String
    var placeholder = ""
    var content: FieldContent = .none
    var numberPad = false
    var enabled = true
    var autoFocus = false
    var label: String?
    var errorMessage: String?
    var identifier: String?
    let onChange: (String) -> Void
    /// What the caller kept of the last edit, read right after `onChange`.
    var kept: (() -> String)?
    var onFocus: (Bool) -> Void = { _ in }

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.layoutDirection) private var direction
    @Environment(\.stepPhase) private var phase

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> UITextField {
        let field = content.isCard ? UITextField() : PixelCentredTextField()
        field.borderStyle = .none
        field.clearButtonMode = .never
        field.autocorrectionType = .no
        field.spellCheckingType = .no
        field.textContentType = content.textContentType
        field.isSecureTextEntry = content == .cvc
        field.adjustsFontForContentSizeCategory = false
        field.setContentHuggingPriority(.defaultLow, for: .horizontal)
        field.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        configureKeyboard(of: field, coordinator: context.coordinator)
        field.addTarget(context.coordinator, action: #selector(Coordinator.changed(_:)), for: .editingChanged)
        field.addTarget(context.coordinator, action: #selector(Coordinator.began), for: .editingDidBegin)
        field.addTarget(context.coordinator, action: #selector(Coordinator.ended), for: .editingDidEnd)
        return field
    }

    func updateUIView(_ field: UITextField, context: Context) {
        let coordinator = context.coordinator
        coordinator.onChange = onChange
        coordinator.kept = kept
        coordinator.onFocus = onFocus
        let colors = theme.colors
        let font = textFont(theme.input)
        // The card fields come out as UIKit sets the system font; the others are as wide as the font without its tracking.
        let kern = content.isCard ? 0 : theme.input.tracking - MatchedFont.tracking(of: font)
        field.font = font
        if kern != 0 { field.defaultTextAttributes[.kern] = kern }
        field.textColor = UIColor(colors.onBackground)
        field.tintColor = UIColor(colors.primary)
        var placeholderAttributes: [NSAttributedString.Key: Any] = [.foregroundColor: UIColor(colors.placeholder), .font: font]
        if kern != 0 { placeholderAttributes[.kern] = kern }
        field.attributedPlaceholder = NSAttributedString(string: placeholder, attributes: placeholderAttributes)
        if field.text != text { field.text = text }
        field.isEnabled = enabled
        field.accessibilityLabel = [label ?? placeholder, errorMessage ?? ""].filter { !$0.isEmpty }.joined(separator: ", ")
        field.accessibilityIdentifier = identifier
        let rightToLeft = direction == .rightToLeft
        if content.isLatin {
            field.semanticContentAttribute = .forceLeftToRight
            field.textAlignment = rightToLeft ? .right : .left
        } else {
            field.semanticContentAttribute = .unspecified
            field.textAlignment = .natural
        }
        if autoFocus, phase.settled, !coordinator.focusedOnce {
            coordinator.focusedOnce = true
            // No field takes the keyboard while the sheet or the step is still moving.
            DispatchQueue.main.async { field.becomeFirstResponder() }
        }
    }

    private func textFont(_ style: CheckoutTextStyle) -> UIFont {
        let size = UIFontMetrics.default.scaledValue(for: style.size)
        let tabular = content.isCard || content == .phone
        if let custom = style.registeredFont(size: size, weight: .regular, tabular: tabular) { return custom }
        if tabular {
            return UIFont.monospacedDigitSystemFont(ofSize: size, weight: .regular)
        }
        return UIFont.systemFont(ofSize: size)
    }

    private func configureKeyboard(of field: UITextField, coordinator: Coordinator) {
        field.autocapitalizationType = .none
        var pad = numberPad
        switch content {
        case .pan, .expiry, .cvc:
            field.keyboardType = .numberPad
            pad = true
        case .holder:
            field.keyboardType = .asciiCapable
            field.autocapitalizationType = .allCharacters
        case .email:
            field.keyboardType = .emailAddress
        case .phone:
            field.keyboardType = .phonePad
            pad = true
        case .addressLine, .city, .state:
            field.keyboardType = .asciiCapable
            field.autocapitalizationType = .words
        case .postalCode:
            field.keyboardType = .asciiCapable
            field.autocapitalizationType = .allCharacters
        case .none:
            field.keyboardType = numberPad ? .numberPad : .default
        }
        if pad {
            // A number pad has no Return key: Done over the keys leaves the field.
            field.inputAccessoryView = Coordinator.doneToolbar(for: field)
        } else {
            field.returnKeyType = .done
            field.addTarget(coordinator, action: #selector(Coordinator.returned(_:)), for: .editingDidEndOnExit)
        }
    }

    final class Coordinator: NSObject {
        var onChange: (String) -> Void = { _ in }
        var kept: (() -> String)?
        var onFocus: (Bool) -> Void = { _ in }
        var focusedOnce = false

        @objc func changed(_ field: UITextField) {
            onChange(field.text ?? "")
            // Within the edit itself: a text replaced a frame later lands between two keys and the keyboard drops the next ones.
            if let kept = kept?(), field.text != kept { field.text = kept }
        }

        @objc func began() { onFocus(true) }

        @objc func ended() { onFocus(false) }

        @objc func returned(_ field: UITextField) { field.resignFirstResponder() }

        static func doneToolbar(for field: UITextField) -> UIToolbar {
            let toolbar = UIToolbar(frame: CGRect(x: 0, y: 0, width: 320, height: 44))
            toolbar.items = [
                UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil),
                UIBarButtonItem(barButtonSystemItem: .done, target: field, action: #selector(UIResponder.resignFirstResponder)),
            ]
            toolbar.sizeToFit()
            return toolbar
        }
    }
}

/// Takes the keyboard away from whichever field has it.
func dismissKeyboard() {
    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
}

/// A field whose line sits where Compose centres a text of the same font: on whole pixels, half a pixel going down.
/// UIKit rounds the other way. The card fields are UIKit's own in the Compose form too and stay as they are.
private final class PixelCentredTextField: UITextField {
    override func textRect(forBounds bounds: CGRect) -> CGRect { line(in: bounds) }

    override func editingRect(forBounds bounds: CGRect) -> CGRect { line(in: bounds) }

    override func placeholderRect(forBounds bounds: CGRect) -> CGRect { line(in: bounds) }

    /// An empty field's value is its placeholder; when that is the field's name too, VoiceOver would say it twice. The
    /// Compose payer field reads it once.
    override var accessibilityValue: String? {
        get { (text ?? "").isEmpty && placeholder == accessibilityLabel ? nil : super.accessibilityValue }
        set { super.accessibilityValue = newValue }
    }

    private func line(in bounds: CGRect) -> CGRect {
        guard let font = font else { return bounds }
        let scale = UIScreen.main.scale
        let height = (font.lineHeight * scale).rounded() / scale
        return CGRect(x: bounds.minX, y: bounds.minY + pixelCentredTop(height, in: bounds.height), width: bounds.width, height: height)
    }
}
