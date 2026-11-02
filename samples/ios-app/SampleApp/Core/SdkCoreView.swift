import SwiftUI
import UIKit
import sdk_payment_scanner

/// SDK Core: a card form of the merchant's own, encrypted on the device into the token the merchant's
/// server sends to the gateway. Card data stay in memory; mdOrder, key, bindingId and the variant are
/// remembered between launches.
struct SdkCoreView: View {
    @StateObject private var model = SdkCoreFormModel()
    @AppStorage(AppPrefsKeys.mdOrder) private var mdOrder = AppPrefsDefaults.mdOrder
    @AppStorage(AppPrefsKeys.pubKeyPem) private var pubKeyPem = AppPrefsDefaults.pubKeyPem
    @AppStorage(AppPrefsKeys.bindingId) private var bindingId = AppPrefsDefaults.bindingId
    @AppStorage(AppPrefsKeys.sdkCoreVariant) private var variantKey = AppPrefsDefaults.sdkCoreVariant
    @Environment(\.colorScheme) private var scheme
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let colors = CoreColors(dark: scheme == .dark)
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                header(colors)
                if let token = model.token {
                    CoreTokenPanel(token: token, colors: colors) { model.token = nil }
                } else {
                    form(colors)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        // The number pad has no return key: the keyboard goes with a scroll or with "Done" above it.
        .scrollDismissesKeyboard(.interactively)
        .background(colors.background.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Done") {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                }
                .accessibilityIdentifier("core_keyboard_done")
            }
        }
        .onAppear {
            model.mdOrder = mdOrder
            model.pubKeyPem = pubKeyPem
            model.bindingId = bindingId
            model.variant = SdkCoreVariant(rawValue: variantKey) ?? .newCard
        }
        .onChange(of: model.mdOrder) { mdOrder = $0 }
        .onChange(of: model.pubKeyPem) { pubKeyPem = $0 }
        .onChange(of: model.bindingId) { bindingId = $0 }
        .onChange(of: model.variant) {
            variantKey = $0.rawValue
            model.errors = [:]
        }
    }

    private func header(_ colors: CoreColors) -> some View {
        HStack(spacing: 4) {
            Button {
                if model.token != nil { model.token = nil } else { dismiss() }
            } label: {
                Image(systemName: "chevron.left").font(.system(size: 18, weight: .semibold)).foregroundColor(colors.text)
                    .frame(width: 44, height: 44)
            }
            .accessibilityLabel(Text("Back"))
            VStack(alignment: .leading, spacing: 1) {
                Text(verbatim: "SDK Core").font(.custom("Inter", size: 22).weight(.heavy)).foregroundColor(colors.text)
                if model.token != nil {
                    Text("Token is ready").font(.custom("Inter", size: 13)).foregroundColor(colors.muted)
                }
            }
        }
        .padding(.top, 4)
        .padding(.bottom, 12)
    }

    private func form(_ colors: CoreColors) -> some View {
        return VStack(alignment: .leading, spacing: 12) {
            variantSwitch(colors)
            Text(hint).font(.custom("Inter", size: 13)).foregroundColor(colors.muted).fixedSize(horizontal: false, vertical: true)
            if model.variant == .stored {
                input(.bindingId, "bindingId", text: $model.bindingId, kind: .pasted(hint: nil), colors)
                input(.cvc, "CVC/CVV (optional)", text: $model.cvc, kind: .digits, colors)
            } else {
                CardFieldsGroup(model: model, colors: colors, dark: scheme == .dark)
            }
            if model.variant != .instant {
                input(.mdOrder, "mdOrder", text: $model.mdOrder, kind: .pasted(hint: "From your backend"), colors)
            }
            input(.pubKey, "Public key (PEM)", text: $model.pubKeyPem, kind: .pasted(hint: nil, multiline: true), colors)
            Button { model.submit() } label: {
                Text("Get token")
                    .font(.custom("Inter", size: 16).weight(.bold))
                    .foregroundColor(colors.onButton)
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .background(colors.button)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            .padding(.top, 4)
            .accessibilityIdentifier("core_generate")
        }
    }

    private var hint: LocalizedStringKey {
        switch model.variant {
        case .newCard: return "Pays an order you registered: card data plus its mdOrder."
        case .instant: return "No order yet: your server creates it and pays in one call."
        case .stored: return "Pays an order with a saved card: its bindingId instead of card data."
        }
    }

    private func variantSwitch(_ colors: CoreColors) -> some View {
        HStack(spacing: 0) {
            ForEach([(SdkCoreVariant.newCard, LocalizedStringKey("New card")),
                     (.instant, "No order"),
                     (.stored, "Saved card")], id: \.0) { variant, label in
                let on = variant == model.variant
                Button { model.variant = variant } label: {
                    Text(label)
                        .font(.custom("Inter", size: 13).weight(on ? .bold : .medium))
                        .foregroundColor(on ? colors.text : colors.muted)
                        .frame(maxWidth: .infinity, minHeight: 40)
                        .background(on ? colors.surface : .clear)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(colors.track)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func input(_ field: CoreField, _ label: LocalizedStringKey, text: Binding<String>, kind: CoreInputKind, _ colors: CoreColors) -> some View {
        CoreInput(field: field, label: label, text: Binding(get: { text.wrappedValue }, set: {
            text.wrappedValue = $0
            model.errors[field] = nil
        }), error: model.errors[field], kind: kind, colors: colors)
    }
}

/// How an input reads and is filled.
enum CoreInputKind {
    case text, digits
    /// Copied from elsewhere: monospace, with a "Paste" action.
    case pasted(hint: LocalizedStringKey?, multiline: Bool = false)
}

private struct CoreInput: View {
    let field: CoreField
    let label: LocalizedStringKey
    @Binding var text: String
    let error: String?
    let kind: CoreInputKind
    let colors: CoreColors

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.custom("Inter", size: 13).weight(.semibold)).foregroundColor(colors.muted)
            HStack(alignment: multiline ? .top : .center, spacing: 4) {
                ZStack(alignment: .topLeading) {
                    if text.isEmpty, case let .pasted(hint?, _) = kind {
                        Text(hint).font(.custom("Inter", size: 15)).foregroundColor(colors.placeholder)
                    }
                    if multiline {
                        // A whole 2048-bit PEM key: its 64-character lines wrap in two.
                        TextField("", text: $text, axis: .vertical)
                            .lineLimit(4...18)
                            .font(.system(size: 12, design: .monospaced))
                    } else if isDigits {
                        // A saved card's network is unknown: up to 4 digits.
                        MaskedTextField(field: field, text: $text, format: { String($0.filter(\.isNumber).prefix(4)) },
                                        textColor: UIColor(colors.text), tint: UIColor(colors.accent))
                    } else {
                        TextField("", text: $text)
                            .font(mono ? .system(size: 15, design: .monospaced) : .custom("Inter", size: 15))
                    }
                }
                .foregroundColor(colors.text)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .accessibilityIdentifier("core_field_\(field.rawValue)")
                .padding(.vertical, 13)
                if case .pasted = kind {
                    // The system paste button: reading the pasteboard from a plain button asks the user for
                    // permission every time and blocks the app until they answer.
                    PasteButton(payloadType: String.self) { strings in
                        if let pasted = strings.first { text = pasted.trimmingCharacters(in: .whitespacesAndNewlines) }
                    }
                    // Just the clipboard icon: the full system button is too heavy next to a field.
                    .labelStyle(.iconOnly)
                    .buttonBorderShape(.capsule)
                    .controlSize(.small)
                    .tint(colors.accent)
                    .accessibilityIdentifier("core_paste_\(field.rawValue)")
                }
            }
            .padding(.leading, 14)
            .padding(.trailing, 6)
            .frame(minHeight: 48)
            .background(colors.surface)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(error != nil ? colors.error : colors.border))
            if let error {
                Text(coreErrorText(error))
                    .font(.custom("Inter", size: 12))
                    .foregroundColor(colors.error)
                    .accessibilityIdentifier("core_error_\(field.rawValue)")
            }
        }
    }

    private var multiline: Bool { if case .pasted(_, true) = kind { return true } else { return false } }
    private var mono: Bool { if case .text = kind { return false } else { return true } }
    private var isDigits: Bool { if case .digits = kind { return true } else { return false } }
}

/// The token, a copy action and what happens next.
private struct CoreTokenPanel: View {
    let token: String
    let colors: CoreColors
    let onAgain: () -> Void
    @State private var copied = false

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 12) {
                Label("Token received", systemImage: "checkmark")
                    .font(.custom("Inter", size: 12).weight(.bold))
                    .foregroundColor(colors.success)
                    .padding(.horizontal, 10).padding(.vertical, 4)
                    .background(Capsule().fill(colors.successSoft))
                Text(verbatim: token)
                    .font(.system(size: 11, design: .monospaced))
                    .foregroundColor(colors.text)
                    .textSelection(.enabled)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(colors.track)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .accessibilityIdentifier("core_token")
                HStack {
                    Spacer()
                    Button(copied ? "Copied" : "Copy") {
                        UIPasteboard.general.string = token
                        copied = true
                    }
                    .font(.custom("Inter", size: 13).weight(.bold))
                    .foregroundColor(colors.accent)
                    .frame(minHeight: 44)
                }
            }
            .padding(16)
            .background(colors.surface)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(colors.border))
            VStack(alignment: .leading, spacing: 10) {
                step(1, "Send the token to your server")
                step(2, "Your server passes it to the gateway as seToken")
                step(3, "The gateway processes the payment")
            }
            Button(action: onAgain) {
                Text("Again")
                    .font(.custom("Inter", size: 16).weight(.bold))
                    .foregroundColor(colors.text)
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .background(colors.track)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
        }
    }

    private func step(_ n: Int, _ text: LocalizedStringKey) -> some View {
        HStack(spacing: 10) {
            Text(verbatim: "\(n)")
                .font(.custom("Inter", size: 12).weight(.bold))
                .foregroundColor(colors.step)
                .frame(width: 24, height: 24)
                .background(Circle().fill(colors.stepSoft))
            Text(text).font(.custom("Inter", size: 14)).foregroundColor(colors.text)
        }
    }
}

/// Colours of the SDK Core screen; it follows the phone's light or dark theme.
struct CoreColors {
    let background, surface, track, text, muted, placeholder, border, accent, button, onButton: Color
    let error, success, successSoft, step, stepSoft: Color

    init(dark: Bool) {
        if dark {
            background = Color(argb: 0xFF0B_1120); surface = Color(argb: 0xFF11_1827); track = Color(argb: 0xFF1F_2937)
            text = Color(argb: 0xFFF1_F5F9); muted = Color(argb: 0xFF94_A3B8); placeholder = Color(argb: 0xFF64_748B)
            border = Color(argb: 0xFF1F_2937); accent = Color(argb: 0xFF60_A5FA)
            button = Color(argb: 0xFFF1_F5F9); onButton = Color(argb: 0xFF0F_172A)
            error = Color(argb: 0xFFF8_7171); success = Color(argb: 0xFF4A_DE80); successSoft = Color(argb: 0xFF05_2E16)
            step = Color(argb: 0xFFC7_D2FE); stepSoft = Color(argb: 0xFF31_2E81)
        } else {
            background = Color(argb: 0xFFF8_FAFC); surface = .white; track = Color(argb: 0xFFEE_F2F6)
            text = Color(argb: 0xFF0F_172A); muted = Color(argb: 0xFF47_5569); placeholder = Color(argb: 0xFF94_A3B8)
            border = Color(argb: 0xFFE2_E8F0); accent = Color(argb: 0xFF25_63EB)
            button = Color(argb: 0xFF0F_172A); onButton = .white
            error = Color(argb: 0xFFDC_2626); success = Color(argb: 0xFF16_6534); successSoft = Color(argb: 0xFFDC_FCE7)
            step = Color(argb: 0xFF37_30A3); stepSoft = Color(argb: 0xFFE0_E7FF)
        }
    }
}
