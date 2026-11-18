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

final class QrScannerController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    private let onScan: (QrScan) -> Void
    private let session = AVCaptureSession()
    private let sessionQueue = DispatchQueue(label: "qa.qr.session")
    private lazy var preview = AVCaptureVideoPreviewLayer(session: session)
    private var finished = false

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
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            start()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    if granted { self?.start() } else { self?.finish(.unavailable) }
                }
            }
        default:
            finish(.unavailable)
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        // A swipe down closes the sheet without the cancel button.
        finish(.cancelled)
    }

    private func start() {
        guard !finished, !session.isRunning else { return }
        let output = AVCaptureMetadataOutput()
        guard let camera = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: camera),
              session.canAddInput(input), session.canAddOutput(output)
        else { return finish(.unavailable) }
        session.addInput(input)
        session.addOutput(output)
        // The QR type is offered only once the output is attached to a session with a camera.
        output.metadataObjectTypes = [.qr]
        output.setMetadataObjectsDelegate(self, queue: .main)
        // startRunning blocks until the camera is up.
        sessionQueue.async { [session] in session.startRunning() }
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
        guard !finished else { return }
        finished = true
        sessionQueue.async { [session] in session.stopRunning() }
        onScan(scan)
    }
}
