import AVFoundation
import SwiftUI
import UIKit

/// How a QR scan ended.
nonisolated enum QrScan: Equatable {
    /// The code's text, for `intake(fromText:)`; empty when the code carries bytes that are not text.
    case text(String)

    /// The tester closed the scanner.
    case cancelled

    /// The device has no camera, or the tester denied the app access to it.
    case unavailable
}

// A scanned code may carry credentials.
nonisolated extension QrScan: CustomStringConvertible, CustomDebugStringConvertible, CustomReflectable {
    var description: String {
        switch self {
        case .text: "text(…)"
        case .cancelled: "cancelled"
        case .unavailable: "unavailable"
        }
    }

    var debugDescription: String {
        description
    }

    var customMirror: Mirror {
        Mirror(self, children: [], displayStyle: .enum)
    }
}

/// A full-screen camera that reads one QR code and reports how the scan ended through `onScan`, exactly once.
struct QrScannerView: UIViewControllerRepresentable {
    let onScan: (QrScan) -> Void

    /// Whether the device has a camera at all; the simulator has none, so the scan button can be hidden there.
    static var isAvailable: Bool {
        AVCaptureDevice.default(for: .video) != nil
    }

    func makeUIViewController(context _: Context) -> QrScannerController {
        QrScannerController(onScan: onScan)
    }

    func updateUIViewController(_: QrScannerController, context _: Context) {}
}

/// What a scanner screen does as it appears and as a scan ends, apart from the camera: it reports once, and a screen that
/// is gone without a code is a cancel.
nonisolated struct QrScannerState {
    enum Step: Equatable {
        case configure
        case resume
        case none
    }

    private var configured = false
    private var reported = false

    /// The camera is configured on the first appearance and resumed on a later one, until the scan has been reported.
    mutating func appear() -> Step {
        guard !reported else { return .none }
        guard configured else {
            configured = true
            return .configure
        }
        return .resume
    }

    /// The scan to report, or nil when one has been reported already.
    mutating func end(_ scan: QrScan) -> QrScan? {
        guard !reported else { return nil }
        reported = true
        return scan
    }

    mutating func disappear() -> QrScan? {
        end(.cancelled)
    }

    /// The camera may start: false once the scan has been reported, e.g. the screen closed during the permission prompt.
    var mayStart: Bool {
        !reported
    }
}

final class QrScannerController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    private let onScan: (QrScan) -> Void
    private let session = AVCaptureSession()
    private let sessionQueue = DispatchQueue(label: "qa.qr.session")
    private lazy var preview = AVCaptureVideoPreviewLayer(session: session)
    private var state = QrScannerState()

    init(onScan: @escaping (QrScan) -> Void) {
        self.onScan = onScan
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("QrScannerController is created in code")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        preview.videoGravity = .resizeAspectFill
        view.layer.addSublayer(preview)
        let cancel = UIButton(type: .system, primaryAction: UIAction(title: "Cancel") { [weak self] _ in
            self?.finish(.cancelled)
        })
        cancel.tintColor = .white
        cancel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(cancel)
        NSLayoutConstraint.activate([
            cancel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            cancel.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -24),
        ])
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        preview.frame = view.bounds
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        switch state.appear() {
        case .configure:
            authorize()
        case .resume:
            // A swipe down that was abandoned shows the screen again; the camera may not be configured yet if the
            // permission prompt is still open.
            sessionQueue.async { [session] in
                if !session.inputs.isEmpty, !session.isRunning { session.startRunning() }
            }
        case .none:
            break
        }
    }

    // Not viewWillDisappear: a swipe down that is started and abandoned calls it, and the sheet stays.
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        if let scan = state.disappear() { report(scan) }
    }

    private func authorize() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            configure()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    if granted { self?.configure() } else { self?.finish(.unavailable) }
                }
            }
        default:
            finish(.unavailable)
        }
    }

    private func configure() {
        guard state.mayStart else { return }
        let output = AVCaptureMetadataOutput()
        output.setMetadataObjectsDelegate(self, queue: .main)
        // Configuring and starting the session block until the camera is up.
        sessionQueue.async { [weak self, session] in
            guard let camera = AVCaptureDevice.default(for: .video),
                  let input = try? AVCaptureDeviceInput(device: camera),
                  session.canAddInput(input), session.canAddOutput(output)
            else {
                DispatchQueue.main.async { self?.finish(.unavailable) }
                return
            }
            session.beginConfiguration()
            session.addInput(input)
            session.addOutput(output)
            // The QR type is offered only once the output is attached to a session with a camera.
            output.metadataObjectTypes = [.qr]
            session.commitConfiguration()
            session.startRunning()
        }
    }

    nonisolated func metadataOutput(
        _: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from _: AVCaptureConnection
    ) {
        let code = metadataObjects.lazy.compactMap { $0 as? AVMetadataMachineReadableCodeObject }.first { $0.type == .qr }
        guard let code else { return }
        let text = code.stringValue ?? ""
        // The delegate queue is the main queue.
        MainActor.assumeIsolated { finish(.text(text)) }
    }

    private func finish(_ scan: QrScan) {
        if let scan = state.end(scan) { report(scan) }
    }

    private func report(_ scan: QrScan) {
        sessionQueue.async { [session] in session.stopRunning() }
        onScan(scan)
    }
}
