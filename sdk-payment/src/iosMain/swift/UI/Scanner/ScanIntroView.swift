import SwiftUI

/// ID-1 card: 85.60 × 53.98 mm.
let cardAspect: CGFloat = 1.586
/// Between the card frame and the screen's sides.
let cardFrameInset: CGFloat = 28
let cardFrameCorner: CGFloat = 14
/// The card under the camera's texts and buttons.
let cameraCardFill = Color(red: 0x1C / 255, green: 0x1C / 255, blue: 0x1E / 255)

/// The camera screen before the camera: a frame where the card will go and, under it, why the camera is asked for.
/// The system's permission prompt comes only after `onTurnOn`, so it reads as the answer to this card.
struct ScanIntroView: View {
    let onTurnOn: () -> Void
    let onManual: () -> Void

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        ZStack {
            CardFrameArea { frame in
                RoundedRectangle(cornerRadius: cardFrameCorner, style: .circular)
                    .strokeBorder(Color.white.opacity(0.3), lineWidth: 2)
                    .frame(width: frame.width, height: frame.height)
                    .position(x: frame.midX, y: frame.midY)
            }
            .accessibilityHidden(true)
            VStack(spacing: 0) {
                HStack {
                    CameraCircleButton(icon: .stroke(.close), label: text("action_close"), action: onManual)
                    Spacer()
                }
                .padding(CameraCircleButton.edgePadding)
                Spacer(minLength: 0)
                VStack(alignment: .leading, spacing: 10) {
                    Text(text("scan_title"))
                        .line(theme.headline, color: .white)
                        .accessibilityAddTraits(.isHeader)
                    LeadingParagraph(text: text("scan_body"), style: theme.body, color: Color.white.opacity(0.7))
                        .padding(.bottom, 6)
                    CameraPlateButton(
                        text: text("scan_turn_on"), style: theme.button, corner: theme.buttonCorner, primary: true, action: onTurnOn
                    )
                    CameraPlateButton(
                        text: text("scan_manual"), style: theme.button, corner: theme.buttonCorner, primary: false,
                        translucency: 0.14, action: onManual
                    )
                }
                .padding(18)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(RoundedRectangle(cornerRadius: 22, style: .circular).fill(cameraCardFill))
                .padding(12)
            }
        }
    }

    private func text(_ key: String) -> String { strings?.text(key: key) ?? "" }
}

/// Lays `content` out over the card frame of the camera screen: as wide as the screen less the insets, centred.
struct CardFrameArea<Content: View>: View {
    @ViewBuilder let content: (CGRect) -> Content

    var body: some View {
        GeometryReader { proxy in
            content(CardFrameArea.frame(in: proxy.size))
        }
    }

    static func frame(in size: CGSize) -> CGRect {
        let width = size.width - 2 * cardFrameInset
        let height = width / cardAspect
        return CGRect(x: cardFrameInset, y: (size.height - height) / 2, width: width, height: height)
    }
}

/// A round 40-point button over the camera.
struct CameraCircleButton: View {
    enum Icon {
        case stroke(PaymentIcon)
        case fill(String)
    }

    /// The circle's 16 points from the screen's edges less the touch target's margin.
    static let edgePadding: CGFloat = 12

    let icon: Icon
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle().fill(Color.white.opacity(0.16))
                switch icon {
                case .stroke(let paymentIcon):
                    IconView(icon: paymentIcon, size: 20, color: .white)
                case .fill(let path):
                    FilledIcon(path: path).fill(Color.white).frame(width: 20, height: 20)
                }
            }
            .frame(width: 40, height: 40)
            // A 48-point target around the 40-point circle, as Compose's minimum touch size.
            .padding(4)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainPress())
        .accessibilityLabel(Text(label))
    }
}

/// A Material icon (24-unit grid, filled), as the Compose scanner draws its buttons.
struct FilledIcon: Shape {
    let path: String

    func path(in rect: CGRect) -> Path {
        let scale = rect.width / 24
        let transform = CGAffineTransform(translationX: rect.minX, y: rect.minY).scaledBy(x: scale, y: scale)
        var result = Path()
        result.addPath(Path(SVGPath.parse(path)), transform: transform)
        return result
    }
}

/// A full-width button over the camera: white for the main action, translucent for the other.
struct CameraPlateButton: View {
    let text: String
    let style: CheckoutTextStyle
    let corner: CGFloat
    let primary: Bool
    var translucency = 0.16
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .line(style, color: primary ? .black : .white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(
                    RoundedRectangle(cornerRadius: corner, style: .circular)
                        .fill(primary ? Color.white : Color.white.opacity(translucency))
                )
                .contentShape(RoundedRectangle(cornerRadius: corner, style: .circular))
        }
        .buttonStyle(PlainPress())
    }
}
