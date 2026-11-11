package net.payrdr.mobile.payment.sdk.payment.scanner.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Full-frame camera preview hosting the OCR pipeline: `AndroidView` + CameraX `PreviewView` with `ImageAnalysis`
 * configured with `STRATEGY_KEEP_ONLY_LATEST`. Each detected-text frame calls [onRecognized] on the main thread with
 * a list of normalised lines; on camera startup failure, [onError] fires.
 */
@Composable
internal fun CameraPreview(
    onRecognized: (List<RecognizedLine>) -> Unit,
    onError: () -> Unit,
    torch: TorchState,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnRecognized by rememberUpdatedState(onRecognized)
    val currentOnError by rememberUpdatedState(onError)
    val recognizer = remember { TextRecognizer() }
    val torchController = remember { TorchController() }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val analysisScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // For the whole screen: the camera below is bound again when its view comes, and must not take these with it.
    DisposableEffect(Unit) {
        onDispose {
            analysisScope.cancel()
            recognizer.close()
        }
    }

    DisposableEffect(previewView) {
        val view = previewView
        val providerFuture = ProcessCameraProvider.getInstance(context)
        if (view != null) {
            providerFuture.addListener({
                try {
                    val provider = providerFuture.get()
                    val preview =
                        Preview.Builder().build().apply {
                            setSurfaceProvider(view.surfaceProvider)
                        }
                    val analysis =
                        ImageAnalysis
                            .Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                    analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy ->
                        analysisScope.launch {
                            try {
                                currentOnRecognized(recognizer.recognize(proxy))
                            } catch (_: Throwable) {
                                proxy.close()
                            }
                        }
                    }
                    provider.unbindAll()
                    val camera =
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    torchController.attach(camera)
                } catch (_: Throwable) {
                    currentOnError()
                }
            }, ContextCompat.getMainExecutor(context))
        }
        onDispose {
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (_: Throwable) {
                // best-effort teardown
            }
            torchController.attach(null)
        }
    }

    LaunchedEffect(torch) {
        torchController.setEnabled(torch.enabled)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).also { previewView = it }
        },
    )
}

/** UI-controlled torch state, wired to the TorchController of the preview. */
internal data class TorchState(
    val enabled: Boolean,
)
