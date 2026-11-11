import SwiftUI
import RadarpaymentsSDKPayment

/// The card fields as one group, the way the SDK's own form shows them: the number across the top with the
/// network logo, expiry and CVC side by side, the name below, split by hairlines in one frame. The focused cell
/// is outlined in the accent colour, a cell with an error in red; the error itself goes under the group.
struct CardFieldsGroup: View {
    @ObservedObject var model: SdkCoreFormModel
    let colors: CoreColors
    let dark: Bool
    @State private var focused: CoreField?
    @State private var registry = MaskedFieldRegistry()

    var body: some View {
        let network = CardNetwork(cardNumber: model.pan)
        VStack(alignment: .leading, spacing: 6) {
            Text("Card information").font(.custom("Inter", size: 13).weight(.semibold)).foregroundColor(colors.muted)
            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    cell(.pan, "Card number", text: panText, format: formatPan, edges: [.top, .leading, .trailing]) {
                        if network != .unknown { ColorNetworkLogo(network: network, dark: dark) }
                    }
                }
                .zIndex(highlight(.pan) != nil ? 1 : 0)
                hairline.frame(height: stroke)
                HStack(spacing: 0) {
                    cell(.expiry, "MM/YY", text: $model.expiry, format: formatExpiryInput, edges: [.leading])
                    hairline.frame(width: stroke)
                    cell(.cvc, "CVC/CVV", text: $model.cvc, format: { String($0.filter(\.isNumber).prefix(network.cvcLength)) }, edges: [.trailing])
                }
                .zIndex(highlight(.expiry) != nil || highlight(.cvc) != nil ? 1 : 0)
                hairline.frame(height: stroke)
                HStack(spacing: 0) {
                    cell(.holder, "Name on card (optional)", text: $model.holder, format: { $0.uppercased() }, edges: [.bottom, .leading, .trailing])
                }
                .zIndex(highlight(.holder) != nil ? 1 : 0)
            }
            // The frame goes under the cells, so an outlined cell draws over it.
            .background(
                RoundedRectangle(cornerRadius: radius).fill(colors.surface)
                    .overlay(RoundedRectangle(cornerRadius: radius).strokeBorder(colors.border, lineWidth: stroke))
            )
            if let field = [CoreField.pan, .expiry, .cvc, .holder].first(where: { model.errors[$0] != nil }), let code = model.errors[field] {
                Text(coreErrorText(code))
                    .font(.custom("Inter", size: 12))
                    .foregroundColor(colors.error)
                    .accessibilityIdentifier("core_error_\(field.rawValue)")
            }
        }
    }

    /// The number as shown, in the network's groups ("4111 1111 1111 1111"); the model keeps the digits.
    private var panText: Binding<String> {
        Binding(
            get: { CardNetwork(cardNumber: model.pan).format(model.pan) },
            set: { shown in
                let digits = shown.filter(\.isNumber)
                model.pan = digits
                // A new network may take a shorter CVC (Amex has 4 digits, the others 3).
                let max = CardNetwork(cardNumber: digits).cvcLength
                if model.cvc.count > max { model.cvc = String(model.cvc.prefix(max)) }
            }
        )
    }

    /// Digits only, as many as the network allows, in its groups.
    private func formatPan(_ typed: String) -> String {
        let all = typed.filter(\.isNumber)
        let network = CardNetwork(cardNumber: all)
        return network.format(String(all.prefix(network.maxPanLength)))
    }

    private let radius: CGFloat = 12
    private let stroke: CGFloat = 1.5

    private var hairline: some View { colors.border }

    private func highlight(_ field: CoreField) -> Color? {
        if model.errors[field] != nil { return colors.error }
        return focused == field ? colors.accent : nil
    }

    private func cell(_ field: CoreField, _ placeholder: LocalizedStringKey, text: Binding<String>, format: @escaping (String) -> String,
                      edges: Edge.Set, @ViewBuilder trailing: () -> some View = { EmptyView() }) -> some View {
        HStack(spacing: 4) {
            ZStack(alignment: .leading) {
                if text.wrappedValue.isEmpty {
                    // The field's own font, so the placeholder and the value line up.
                    Text(placeholder).font(Font(MaskedTextField.font)).foregroundColor(colors.placeholder).lineLimit(1)
                }
                MaskedTextField(
                    field: field,
                    text: Binding(get: { text.wrappedValue }, set: {
                        if $0 != text.wrappedValue { model.errors[field] = nil }
                        text.wrappedValue = $0
                    }),
                    format: format,
                    keyboard: field == .holder ? .asciiCapable : .numberPad,
                    capitalization: field == .holder ? .allCharacters : .none,
                    contentType: contentType(field),
                    textColor: UIColor(colors.text),
                    tint: UIColor(colors.accent),
                    registry: registry,
                    onFocus: { on in
                        if on { focused = field } else if focused == field { focused = nil }
                    }
                )
            }
            trailing()
        }
        .padding(.leading, 14)
        .padding(.trailing, 10)
        .frame(maxWidth: .infinity, minHeight: 48, maxHeight: 48)
        .contentShape(Rectangle())
        .onTapGesture { registry.focus(field) }
        .overlay {
            if let color = highlight(field) {
                CellOutline(edges: edges, radius: radius, stroke: stroke).stroke(color, lineWidth: stroke)
            }
        }
        // Over the hairline beside it, which the next cell in the row would otherwise draw on top.
        .zIndex(highlight(field) != nil ? 1 : 0)
    }

    private func contentType(_ field: CoreField) -> UITextContentType? {
        switch field {
        case .pan: return .creditCardNumber
        case .holder: return .name
        default: return nil
        }
    }
}

/// A cell's outline, as the SDK draws it: over the hairlines on its inner sides and over the frame on its outer
/// ones, rounded where it meets the group's corners.
private struct CellOutline: Shape {
    let edges: Edge.Set
    let radius: CGFloat
    let stroke: CGFloat

    func path(in rect: CGRect) -> Path {
        var r = rect
        if !edges.contains(.top) { r.origin.y -= stroke; r.size.height += stroke }
        if !edges.contains(.bottom) { r.size.height += stroke }
        if !edges.contains(.leading) { r.origin.x -= stroke; r.size.width += stroke }
        if !edges.contains(.trailing) { r.size.width += stroke }
        r = r.insetBy(dx: stroke / 2, dy: stroke / 2)
        var corners: UIRectCorner = []
        if edges.contains(.top) && edges.contains(.leading) { corners.insert(.topLeft) }
        if edges.contains(.top) && edges.contains(.trailing) { corners.insert(.topRight) }
        if edges.contains(.bottom) && edges.contains(.leading) { corners.insert(.bottomLeft) }
        if edges.contains(.bottom) && edges.contains(.trailing) { corners.insert(.bottomRight) }
        let inner = radius - stroke / 2
        return Path(UIBezierPath(roundedRect: r, byRoundingCorners: corners, cornerRadii: CGSize(width: inner, height: inner)).cgPath)
    }
}

/// sdk-core reports stable codes; a merchant maps them to its own words, as here.
func coreErrorText(_ code: String) -> LocalizedStringKey {
    switch code {
    case "required": return "Required"
    case "invalid-format": return "Wrong format"
    default: return "Invalid value"
    }
}
