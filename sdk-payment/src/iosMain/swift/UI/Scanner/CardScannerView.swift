import AVFoundation
import SwiftUI
import UIKit

/// The scanner run of one camera screen: Kotlin's session and the state it last reported.
final class ScannerRun: ObservableObject {
    @Published private(set) var state: NativeScannerState?
    let session: NativeScannerSession

    init(scanner: NativeCardScanner, language: String, onResult: @escaping (CardScannerResult) -> Void) {
        session = scanner.openScanner(
            config: CardScannerConfig(extractCardholder: false, timeoutSeconds: 30, enableTorchButton: true),
            language: language,
            onResult: onResult
        )
        session.observe { [weak self] state in self?.state = state }
    }

    deinit {
        session.close()
    }
}

/// The card scanner over the camera, drawn as the Compose `CardScannerScreen`: the dimmed camera with the card's
/// frame, close and flashlight buttons at the top, the hint or what went wrong at the bottom.
struct CardScannerView: View {
    @StateObject private var run: ScannerRun

    init(scanner: NativeCardScanner, language: String, onResult: @escaping (CardScannerResult) -> Void) {
        _run = StateObject(wrappedValue: ScannerRun(scanner: scanner, language: language, onResult: onResult))
    }

    var body: some View {
        let phase = run.state?.phase ?? .starting
        ZStack {
            if phase == .scanning || phase == .detected {
                ScannerCameraPreview(session: run.session.captureSession)
                    .accessibilityHidden(true)
            }
            ScannerFrameOverlay(highlight: phase == .detected || phase == .success)
                .accessibilityHidden(true)
            VStack(spacing: 0) {
                HStack {
                    CameraCircleButton(icon: .fill(MaterialIcon.close), label: text("scan_close")) { run.session.cancel() }
                    Spacer()
                    if run.state?.torchButton == true {
                        CameraCircleButton(
                            icon: .fill(run.state?.torchOn == true ? MaterialIcon.flashOn : MaterialIcon.flashOff),
                            label: text("scan_torch")
                        ) { run.session.toggleTorch() }
                    }
                }
                .padding(CameraCircleButton.edgePadding)
                Spacer(minLength: 0)
                bottom(phase).padding(12)
            }
        }
        .onDisappear { run.session.close() }
    }

    @ViewBuilder
    private func bottom(_ phase: NativeScannerPhase) -> some View {
        switch phase {
        case .starting, .scanning, .detected, .success:
            VStack(spacing: 16) {
                ScannerHint(
                    hint: text("scan_hint"),
                    detected: text("scan_detected"),
                    found: phase == .detected || phase == .success
                )
                CameraPlateButton(
                    text: text("scan_manual_entry"), style: MaterialText.plate, corner: 14, primary: false
                ) { run.session.cancel() }
            }
            .frame(maxWidth: .infinity)
        case .permissionDenied:
            ScannerMessageCard(
                title: text("scan_permission_required_title"),
                message: text("scan_permission_required_body"),
                primary: (text("scan_permission_denied_open_settings"), { run.session.openSettings() }),
                secondary: (text("scan_manual_entry"), { run.session.cancel() })
            )
        case .timeout:
            ScannerMessageCard(
                title: text("scan_timeout_title"),
                message: text("scan_timeout_body"),
                primary: (text("scan_retry"), { run.session.cancel() }),
                secondary: (text("scan_manual_entry"), { run.session.cancel() })
            )
        case .cameraUnavailable:
            ScannerMessageCard(
                title: text("scan_camera_unavailable_title"),
                message: text("scan_camera_unavailable_body"),
                primary: (text("scan_manual_entry"), { run.session.cancel() }),
                secondary: nil
            )
        default:
            EmptyView()
        }
    }

    private func text(_ key: String) -> String { run.session.text(key: key) }
}

/// The Compose scanner's texts: Material's body style (24-point lines, 0.5 tracking) at the sizes the screen sets.
enum MaterialText {
    static let hint = style(15, .regular)
    static let body = style(15, .regular)
    static let title = style(20, .semibold)
    static let plate = style(16, .semibold)
    static let lineHeight: CGFloat = 24

    private static func style(_ size: CGFloat, _ weight: UIFont.Weight) -> CheckoutTextStyle {
        CheckoutTextStyle(size: size, weight: weight, lineHeight: lineHeight, tracking: 0.5, tabularFigures: false)
    }
}

/// The Material icons of the Compose scanner, filled on a 24-unit grid.
enum MaterialIcon {
    static let close = "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"
    static let flashOn = "M7,2v11h3v9l7,-12h-4l4,-8z"
    static let flashOff =
        "M3.27,3L2,4.27l5,5V13h3v9l3.58,-6.14L17.73,20 19,18.73 3.27,3zM17,10h-4l4,-8H7v2.18l8.46,8.46L17,10z"
}

/// One line of Material text in its 24-point line box.
private struct MaterialLine: View {
    let text: String
    let style: CheckoutTextStyle
    let color: Color

    var body: some View {
        Text(text).line(style, color: color).frame(height: max(MaterialText.lineHeight, style.singleLineHeight))
    }
}

/// The hint under the frame. A blind buyer aiming the camera hears when the card is found: the frame's colour says it
/// to everyone else.
private struct ScannerHint: View {
    let hint: String
    let detected: String
    let found: Bool

    var body: some View {
        MaterialLine(text: hint, style: MaterialText.hint, color: .white)
            .multilineTextAlignment(.center)
            .accessibilityElement(children: .ignore)
            .accessibility(label: Text(found ? detected : hint))
            .onChange(of: found) { found in
                if found { UIAccessibility.post(notification: .announcement, argument: detected) }
            }
    }
}

/// What went wrong and what the buyer can do next, on a card at the bottom.
private struct ScannerMessageCard: View {
    let title: String
    let message: String
    let primary: (String, () -> Void)
    let secondary: (String, () -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            MaterialLine(text: title, style: MaterialText.title, color: .white)
                .accessibilityAddTraits(.isHeader)
                // Read out as it appears: the scan has stopped and the buyer must choose what next.
                .onAppear { UIAccessibility.post(notification: .announcement, argument: title) }
            LeadingParagraph(text: message, style: MaterialText.body, color: Color.white.opacity(0.7))
                .padding(.vertical, (MaterialText.lineHeight - MaterialText.body.singleLineHeight) / 2)
                .padding(.bottom, 6)
            CameraPlateButton(text: primary.0, style: MaterialText.plate, corner: 14, primary: true, action: primary.1)
            if let secondary = secondary {
                CameraPlateButton(text: secondary.0, style: MaterialText.plate, corner: 14, primary: false, action: secondary.1)
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 22, style: .circular).fill(cameraCardFill))
    }
}

/// The camera dimmed around a card-shaped hole, outlined white, or green once a card is found.
private struct ScannerFrameOverlay: View {
    let highlight: Bool

    var body: some View {
        CardFrameArea { frame in
            ZStack {
                DimmedAround(hole: frame).fill(Color.black.opacity(0.5), style: FillStyle(eoFill: true))
                RoundedRectangle(cornerRadius: cardFrameCorner, style: .circular)
                    .stroke(highlight ? Color(red: 0x34 / 255, green: 0xC7 / 255, blue: 0x59 / 255) : Color.white.opacity(0.9), lineWidth: 2)
                    .frame(width: frame.width, height: frame.height)
                    .position(x: frame.midX, y: frame.midY)
            }
        }
    }
}

private struct DimmedAround: Shape {
    let hole: CGRect

    func path(in rect: CGRect) -> Path {
        var path = Path(rect)
        path.addRoundedRect(in: hole, cornerSize: CGSize(width: cardFrameCorner, height: cardFrameCorner), style: .circular)
        return path
    }
}

/// The camera's picture, filling the screen; black until the first frame.
private struct ScannerCameraPreview: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> PreviewView {
        let view = PreviewView()
        view.backgroundColor = .black
        view.previewLayer.session = session
        view.previewLayer.videoGravity = .resizeAspectFill
        return view
    }

    func updateUIView(_ view: PreviewView, context: Context) {}

    final class PreviewView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }

        // swiftlint:disable:next force_cast
        var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
    }
}
