package net.payrdr.mobile.payment.sdk.payment.scanner.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.presentation.NativeScannerPhase
import net.payrdr.mobile.payment.sdk.payment.presentation.NativeScannerSession
import net.payrdr.mobile.payment.sdk.payment.presentation.NativeScannerState
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerConfig
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerResult
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.CameraFeed
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.CameraPermissionController
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.CameraPermissionStatus
import net.payrdr.mobile.payment.sdk.payment.scanner.parser.CardParser
import net.payrdr.mobile.payment.sdk.payment.scanner.state.ScannerUiState
import net.payrdr.mobile.payment.sdk.payment.scanner.state.ScannerViewModel
import platform.AVFoundation.AVCaptureSession

/**
 * The Compose scanner screen's logic without Compose: the same view model, permission flow, parser and camera feed,
 * started in the same order, so both screens behave alike.
 */
internal class VisionScannerSession(
    private val config: CardScannerConfig,
    language: String,
    logger: Logger?,
    onResult: (CardScannerResult) -> Unit,
) : NativeScannerSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val permissions = CameraPermissionController()
    private val parser = CardParser()
    private val viewModel = ScannerViewModel(scope = scope, config = config, logger = logger)
    private val torchOn = MutableStateFlow(false)
    private val texts = ScannerTexts(language)
    private val feed =
        CameraFeed(
            onRecognized = { lines -> parser.parse(lines, config.extractCardholder)?.let(viewModel::onParseResult) },
            onError = viewModel::onCameraError,
        )

    override val captureSession: AVCaptureSession get() = feed.session

    init {
        // Subscribed before anything can emit: the view model's result flow does not replay.
        scope.launch(start = CoroutineStart.UNDISPATCHED) { onResult(viewModel.result.first()) }
        scope.launch {
            val granted =
                when (permissions.status()) {
                    CameraPermissionStatus.Granted -> true
                    CameraPermissionStatus.Denied, CameraPermissionStatus.NotDetermined ->
                        permissions.request() == CameraPermissionStatus.Granted
                }
            viewModel.onPermissionResult(granted)
        }
        // The Compose screen keeps the preview, and with it the camera, only while scanning. The feed starts once and
        // stops for good, so the state before the permission answer must not stop it.
        scope.launch {
            viewModel.state.collect { state ->
                when {
                    state.isLive() -> feed.start()
                    state != ScannerUiState.Initializing -> feed.stop()
                }
            }
        }
        scope.launch { torchOn.collect(feed::setTorch) }
    }

    override fun text(key: String): String = texts.text(key)

    override fun observe(onChange: (NativeScannerState) -> Unit) {
        scope.launch {
            combine(viewModel.state, torchOn) { state, torch ->
                NativeScannerState(
                    phase = state.phase(),
                    torchOn = torch,
                    torchButton = config.enableTorchButton && state.isLive(),
                )
            }.collect(onChange)
        }
    }

    override fun toggleTorch() {
        torchOn.value = !torchOn.value
    }

    override fun cancel() {
        viewModel.onCancel()
    }

    override fun openSettings() {
        permissions.openSettings()
    }

    override fun close() {
        feed.stop()
        scope.cancel()
    }

    private fun ScannerUiState.isLive(): Boolean = this is ScannerUiState.Scanning || this is ScannerUiState.Detecting

    private fun ScannerUiState.phase(): NativeScannerPhase =
        when (this) {
            ScannerUiState.Initializing -> NativeScannerPhase.Starting
            ScannerUiState.PermissionDenied -> NativeScannerPhase.PermissionDenied
            ScannerUiState.Scanning -> NativeScannerPhase.Scanning
            is ScannerUiState.Detecting -> NativeScannerPhase.Detected
            is ScannerUiState.Success -> NativeScannerPhase.Success
            ScannerUiState.Timeout -> NativeScannerPhase.Timeout
            ScannerUiState.CameraUnavailable -> NativeScannerPhase.CameraUnavailable
            ScannerUiState.Cancelled -> NativeScannerPhase.Cancelled
        }
}
