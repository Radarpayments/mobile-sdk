import AVFoundation
import UIKit
import Vision
#if canImport(RadarpaymentsSDKPayment)
import RadarpaymentsSDKPayment
#else
import PayrdrSdkBridge
#endif

/// One camera run: the capture session, its preview and the text Vision reads from the frames.
final class VisionCameraFeed: NSObject, ScannerCameraFeed, AVCaptureVideoDataOutputSampleBufferDelegate {
    private let onText: ([ScannerTextLine]) -> Void
    private let onError: () -> Void
    private let session = AVCaptureSession()
    private let previewView = CameraPreviewView()
    private let sessionQueue = DispatchQueue(label: "sdk.payment.scanner.session")
    private let frameQueue = DispatchQueue(label: "sdk.payment.scanner.frames")
    private var device: AVCaptureDevice?
    private var started = false
    private var stopped = false

    init(onText: @escaping ([ScannerTextLine]) -> Void, onError: @escaping () -> Void) {
        self.onText = onText
        self.onError = onError
        super.init()
        previewView.previewLayer.session = session
        previewView.previewLayer.videoGravity = .resizeAspectFill
    }

    var preview: UIView { previewView }

    func start() {
        guard !started, !stopped else { return }
        started = true
        guard let camera = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: camera),
              session.canAddInput(input)
        else { return onError() }
        device = camera
        session.addInput(input)
        let output = AVCaptureVideoDataOutput()
        output.alwaysDiscardsLateVideoFrames = true
        output.setSampleBufferDelegate(self, queue: frameQueue)
        guard session.canAddOutput(output) else { return onError() }
        session.addOutput(output)
        // startRunning blocks until the camera is up.
        sessionQueue.async { [session] in session.startRunning() }
    }

    func stop() {
        guard !stopped else { return }
        stopped = true
        guard started else { return }
        sessionQueue.async { [session] in session.stopRunning() }
    }

    func setTorch(on: Bool) {
        guard !stopped, let device, device.hasTorch, (try? device.lockForConfiguration()) != nil else { return }
        device.torchMode = on ? .on : .off
        device.unlockForConfiguration()
    }

    func captureOutput(_: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from _: AVCaptureConnection) {
        // The frame queue is serial and late frames are dropped, so one frame is read at a time. The capture output has
        // only a few buffers to hand out, and Vision autoreleases objects that hold the frame: without a pool of its
        // own they outlive the callback and the preview freezes.
        let lines: [ScannerTextLine] = autoreleasepool {
            guard let pixels = CMSampleBufferGetImageBuffer(sampleBuffer) else { return [] }
            let request = VNRecognizeTextRequest()
            request.recognitionLevel = .accurate
            request.usesLanguageCorrection = false
            try? VNImageRequestHandler(cvPixelBuffer: pixels, options: [:]).perform([request])
            return (request.results ?? []).compactMap { observation in
                guard let text = observation.topCandidates(1).first?.string else { return nil }
                // Vision's origin is the bottom left corner; the parser's is the top left.
                let box = observation.boundingBox
                return ScannerTextLine(
                    text: text,
                    left: Float(box.minX),
                    top: Float(1 - box.maxY),
                    right: Float(box.maxX),
                    bottom: Float(1 - box.minY)
                )
            }
        }
        if !lines.isEmpty { onText(lines) }
    }
}

private final class CameraPreviewView: UIView {
    override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }

    var previewLayer: AVCaptureVideoPreviewLayer {
        // The layer class above makes the cast safe.
        // swiftlint:disable:next force_cast
        layer as! AVCaptureVideoPreviewLayer
    }
}
