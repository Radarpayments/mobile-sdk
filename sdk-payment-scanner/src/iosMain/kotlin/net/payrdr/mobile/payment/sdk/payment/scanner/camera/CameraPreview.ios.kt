package net.payrdr.mobile.payment.sdk.payment.scanner.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
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
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreGraphics.CGRectZero
import platform.CoreMedia.CMSampleBufferRef
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create

@OptIn(ExperimentalForeignApi::class)
private class CameraPreviewContainer : UIView(frame = CGRectZero.readValue()) {
    var previewLayer: AVCaptureVideoPreviewLayer? = null

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.frame = bounds
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
public actual fun CameraPreview(
    onRecognized: (List<RecognizedLine>) -> Unit,
    onError: () -> Unit,
    torch: TorchState,
    modifier: Modifier,
) {
    val currentOnRecognized by rememberUpdatedState(onRecognized)
    val currentOnError by rememberUpdatedState(onError)
    val recognizer = remember { TextRecognizer() }
    val torchController = remember { TorchController() }
    val session = remember { AVCaptureSession() }
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    val inflightMutex = remember { Mutex() }
    // Serial: stop always runs after a start queued before it.
    val sessionQueue = remember { dispatch_queue_create("sdk.payment.scanner.session", null) }

    DisposableEffect(Unit) {
        try {
            val device =
                AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
                    ?: error("no video device")
            torchController.attach(device)
            val input =
                AVCaptureDeviceInput.deviceInputWithDevice(device, null)
                    ?: error("cannot create device input")
            val output = AVCaptureVideoDataOutput()
            output.alwaysDiscardsLateVideoFrames = true
            val queue = dispatch_queue_create("sdk.payment.scanner.frames", null)
            val delegate =
                object : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
                    override fun captureOutput(
                        output: AVCaptureOutput,
                        didOutputSampleBuffer: CMSampleBufferRef?,
                        fromConnection: AVCaptureConnection,
                    ) {
                        val buf = didOutputSampleBuffer ?: return
                        // One frame at a time; the rest are dropped right here.
                        if (!inflightMutex.tryLock()) return
                        // The sample buffer is valid only during this callback: AVFoundation recycles it as
                        // soon as we return. Keep it alive until the recognizer is done with it.
                        CFRetain(buf)
                        scope
                            .launch {
                                val lines = recognizer.recognize(buf)
                                if (lines.isNotEmpty()) currentOnRecognized(lines)
                            }.invokeOnCompletion {
                                // Also runs when the scope is cancelled before the job starts.
                                CFRelease(buf)
                                inflightMutex.unlock()
                            }
                    }
                }
            output.setSampleBufferDelegate(delegate, queue)

            session.beginConfiguration()
            if (session.canAddInput(input)) session.addInput(input)
            if (session.canAddOutput(output)) session.addOutput(output)
            session.commitConfiguration()
            // startRunning blocks for a few hundred milliseconds: off the main thread, as Apple asks.
            dispatch_async(sessionQueue) { session.startRunning() }
        } catch (_: Throwable) {
            currentOnError()
        }

        onDispose {
            torchController.attach(null)
            dispatch_async(sessionQueue) { session.stopRunning() }
            recognizer.close()
            scope.cancel()
        }
    }

    LaunchedEffect(torch) {
        torchController.setEnabled(torch.enabled)
    }

    UIKitView(
        modifier = modifier,
        factory = {
            val container = CameraPreviewContainer()
            // Opaque until the first frame: the view sits under a hole in the Compose canvas, and a clear one
            // would show the checkout's native text fields underneath while the camera starts.
            container.backgroundColor = UIColor.blackColor
            val layer = AVCaptureVideoPreviewLayer(session = session)
            layer.videoGravity = AVLayerVideoGravityResizeAspectFill
            container.previewLayer = layer
            container.layer.addSublayer(layer)
            container
        },
        update = { /* layoutSubviews keeps the layer sized */ },
    )
}
