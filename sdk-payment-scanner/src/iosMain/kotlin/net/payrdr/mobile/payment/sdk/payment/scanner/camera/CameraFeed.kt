package net.payrdr.mobile.payment.sdk.payment.scanner.camera

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVMediaTypeVideo
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreMedia.CMSampleBufferRef
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create

/**
 * The back camera feeding the text recognizer: [onRecognized] gets the lines of a frame (on a background thread),
 * [onError] is called when the camera cannot start. Started once and stopped once; both the Compose preview and the
 * SwiftUI scanner screen drive it.
 */
@OptIn(ExperimentalForeignApi::class)
internal class CameraFeed(
    private val onRecognized: (List<RecognizedLine>) -> Unit,
    private val onError: () -> Unit,
) {
    val session = AVCaptureSession()
    private val recognizer = TextRecognizer()
    private val torch = TorchController()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val inflight = Mutex()

    // Serial: stop always runs after a start queued before it.
    private val sessionQueue = dispatch_queue_create("sdk.payment.scanner.session", null)
    private var started = false
    private var stopped = false

    fun start() {
        if (started) return
        started = true
        try {
            val device =
                AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
                    ?: error("no video device")
            torch.attach(device)
            val input =
                AVCaptureDeviceInput.deviceInputWithDevice(device, null)
                    ?: error("cannot create device input")
            val output = AVCaptureVideoDataOutput()
            output.alwaysDiscardsLateVideoFrames = true
            val queue = dispatch_queue_create("sdk.payment.scanner.frames", null)
            output.setSampleBufferDelegate(FrameDelegate(), queue)

            session.beginConfiguration()
            if (session.canAddInput(input)) session.addInput(input)
            if (session.canAddOutput(output)) session.addOutput(output)
            session.commitConfiguration()
            // startRunning blocks for a few hundred milliseconds: off the main thread, as Apple asks.
            dispatch_async(sessionQueue) { session.startRunning() }
        } catch (_: Throwable) {
            onError()
        }
    }

    fun setTorch(on: Boolean) {
        if (!stopped) torch.setEnabled(on)
    }

    fun stop() {
        if (stopped) return
        stopped = true
        torch.attach(null)
        dispatch_async(sessionQueue) { session.stopRunning() }
        recognizer.close()
        scope.cancel()
    }

    private inner class FrameDelegate :
        NSObject(),
        AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
        override fun captureOutput(
            output: AVCaptureOutput,
            didOutputSampleBuffer: CMSampleBufferRef?,
            fromConnection: AVCaptureConnection,
        ) {
            val buf = didOutputSampleBuffer ?: return
            // One frame at a time; the rest are dropped right here.
            if (!inflight.tryLock()) return
            // The sample buffer is valid only during this callback: AVFoundation recycles it as soon as we return.
            // Keep it alive until the recognizer is done with it.
            CFRetain(buf)
            scope
                .launch {
                    val lines = recognizer.recognize(buf)
                    if (lines.isNotEmpty()) onRecognized(lines)
                }.invokeOnCompletion {
                    // Also runs when the scope is cancelled before the job starts.
                    CFRelease(buf)
                    inflight.unlock()
                }
        }
    }
}
