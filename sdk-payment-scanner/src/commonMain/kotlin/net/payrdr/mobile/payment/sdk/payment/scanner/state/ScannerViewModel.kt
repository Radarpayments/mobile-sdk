package net.payrdr.mobile.payment.sdk.payment.scanner.state

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerConfig
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerResult
import net.payrdr.mobile.payment.sdk.payment.scanner.ScannedCard
import kotlin.time.Clock

/**
 * Scanner state machine. Owns:
 *  - the `StateFlow<ScannerUiState>` that the UI renders
 *  - the `SharedFlow<CardScannerResult>` the screen collects to invoke the caller's `onResult`
 *  - the timeout timer (started on `Scanning`, cancelled on any terminal state)
 *  - a 3-frame anti-flicker gate on incoming parse results.
 *  - structured [Logger] events (tag="CardScanner", no PAN digits ever logged).
 *
 * Call `onPermissionResult` before any other event.
 */
internal class ScannerViewModel(
    scope: CoroutineScope,
    private val config: CardScannerConfig,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val logger: Logger? = null,
) {
    private val workScope = scope
    private val workDispatcher = dispatcher
    private val _state = MutableStateFlow<ScannerUiState>(ScannerUiState.Initializing)
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    private val _result =
        MutableSharedFlow<CardScannerResult>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val result: SharedFlow<CardScannerResult> = _result.asSharedFlow()

    private var timeoutJob: Job? = null
    private var streakPan: String? = null
    private var streakCount: Int = 0
    private var startedAtMs: Long = 0L

    /**
     * Result that should be emitted to the host the next time the user dismisses the screen
     * (`onCancel`). For non-Success terminal states (PermissionDenied / Timeout /
     * CameraUnavailable) we hold the result here instead of emitting eagerly — the screen needs
     * to stay mounted to render the corresponding message-card so the user has a chance to read
     * the error and pick "Open settings" / "Enter manually". Success bypasses this and emits
     * immediately because auto-dismiss is the right UX there.
     */
    private var pendingResult: CardScannerResult? = null

    fun onPermissionResult(granted: Boolean) {
        if (!granted) {
            logger?.warning(TAG, "event=permission_denied")
            timeoutJob?.cancel()
            timeoutJob = null
            _state.value = ScannerUiState.PermissionDenied
            pendingResult = CardScannerResult.PermissionDenied
            return
        }
        startedAtMs = Clock.System.now().toEpochMilliseconds()
        logger?.info(TAG, "event=permission_granted")
        logger?.info(TAG, "event=started")
        _state.value = ScannerUiState.Scanning
        timeoutJob =
            workScope.launch(workDispatcher) {
                delay(config.timeoutSeconds * 1000L)
                val elapsed = Clock.System.now().toEpochMilliseconds() - startedAtMs
                logger?.warning(TAG, "event=timeout elapsed_ms=$elapsed")
                timeoutJob = null
                _state.value = ScannerUiState.Timeout
                pendingResult = CardScannerResult.Failed(CardScannerResult.Reason.Timeout)
            }
    }

    fun onParseResult(card: ScannedCard) {
        val current = _state.value
        if (current is ScannerUiState.Success || current is ScannerUiState.Cancelled) return
        if (current is ScannerUiState.Timeout || current is ScannerUiState.CameraUnavailable) return
        if (current is ScannerUiState.PermissionDenied) return
        if (card.pan == streakPan) {
            streakCount += 1
        } else {
            streakPan = card.pan
            streakCount = 1
        }
        if (streakCount >= REQUIRED_STREAK) {
            val elapsed = Clock.System.now().toEpochMilliseconds() - startedAtMs
            logger?.info(TAG, "event=success elapsed_ms=$elapsed")
            timeoutJob?.cancel()
            timeoutJob = null
            _state.value = ScannerUiState.Success(card)
            _result.tryEmit(CardScannerResult.Success(card))
        } else {
            _state.value = ScannerUiState.Detecting(card.pan)
        }
    }

    fun onCancel() {
        logger?.info(TAG, "event=cancelled")
        timeoutJob?.cancel()
        timeoutJob = null
        // Honour the pending error if one was stashed while the user was reading the message
        // card; otherwise this is a clean user cancel from Scanning/Detecting.
        val emit = pendingResult ?: CardScannerResult.Cancelled
        pendingResult = null
        _state.value = ScannerUiState.Cancelled
        _result.tryEmit(emit)
    }

    fun onCameraError() {
        logger?.error(TAG, "event=camera_unavailable")
        timeoutJob?.cancel()
        timeoutJob = null
        _state.value = ScannerUiState.CameraUnavailable
        pendingResult = CardScannerResult.Failed(CardScannerResult.Reason.CameraUnavailable)
    }

    companion object {
        const val REQUIRED_STREAK: Int = 3
        private const val TAG: String = "CardScanner"
    }
}
