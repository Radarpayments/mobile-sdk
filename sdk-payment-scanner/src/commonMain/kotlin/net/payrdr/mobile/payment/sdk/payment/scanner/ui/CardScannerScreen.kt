package net.payrdr.mobile.payment.sdk.payment.scanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerConfig
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerResult
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.CameraPermissionController
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.CameraPermissionStatus
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.CameraPreview
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.TextRecognizer
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.TorchState
import net.payrdr.mobile.payment.sdk.payment.scanner.parser.CardParser
import net.payrdr.mobile.payment.sdk.payment.scanner.state.ScannerUiState
import net.payrdr.mobile.payment.sdk.payment.scanner.state.ScannerViewModel
import net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources.scan_camera_unavailable_body
import net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources.scan_camera_unavailable_title
import net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources.scan_close
import net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources.scan_detected
import net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources.scan_hint
import net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources.scan_manual_entry
import net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources.scan_permission_denied_open_settings
import net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources.scan_permission_required_body
import net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources.scan_permission_required_title
import net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources.scan_retry
import net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources.scan_timeout_body
import net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources.scan_timeout_title
import net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources.scan_torch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CardScannerScreen(
    config: CardScannerConfig,
    permissions: CameraPermissionController,
    recognizer: TextRecognizer,
    logger: Logger?,
    onResult: (CardScannerResult) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val parser = remember { CardParser() }
    val vm = remember { ScannerViewModel(scope = scope, config = config, logger = logger) }
    var torch by remember { mutableStateOf(TorchState(enabled = false)) }

    DisposableEffect(Unit) {
        val job =
            scope.launch {
                val initial = permissions.status()
                val granted =
                    when (initial) {
                        CameraPermissionStatus.Granted -> true
                        CameraPermissionStatus.Denied, CameraPermissionStatus.NotDetermined ->
                            permissions.request() == CameraPermissionStatus.Granted
                    }
                vm.onPermissionResult(granted)
            }
        onDispose {
            job.cancel()
            recognizer.close()
        }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { onResult(vm.result.first()) }
        onDispose { job.cancel() }
    }

    val state by vm.state.collectAsState()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (state) {
            is ScannerUiState.Scanning, is ScannerUiState.Detecting ->
                CameraPreview(
                    onRecognized = { lines ->
                        parser.parse(lines, config.extractCardholder)?.let(vm::onParseResult)
                    },
                    onError = vm::onCameraError,
                    torch = torch,
                    modifier = Modifier.fillMaxSize(),
                )

            else -> Unit
        }
        CardScannerOverlay(highlight = state is ScannerUiState.Detecting || state is ScannerUiState.Success)

        Box(Modifier.fillMaxSize()) {
            CircleAction(
                icon = Icons.Filled.Close,
                description = stringResource(Res.string.scan_close),
                onClick = vm::onCancel,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            )
            if (config.enableTorchButton && (state is ScannerUiState.Scanning || state is ScannerUiState.Detecting)) {
                CircleAction(
                    icon = if (torch.enabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    description = stringResource(Res.string.scan_torch),
                    onClick = { torch = TorchState(!torch.enabled) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                )
            }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)) {
                when (state) {
                    ScannerUiState.Initializing,
                    ScannerUiState.Scanning,
                    is ScannerUiState.Detecting,
                    is ScannerUiState.Success,
                    ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            // A blind buyer aiming the camera hears when the card is found: the frame's colour says it to
                            // everyone else.
                            val detected = stringResource(Res.string.scan_detected)
                            val found = state is ScannerUiState.Detecting || state is ScannerUiState.Success
                            Text(
                                stringResource(Res.string.scan_hint),
                                color = Color.White,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier =
                                    Modifier.semantics {
                                        liveRegion = LiveRegionMode.Polite
                                        if (found) contentDescription = detected
                                    },
                            )
                            PlateButton(stringResource(Res.string.scan_manual_entry), vm::onCancel, primary = false)
                        }

                    ScannerUiState.PermissionDenied ->
                        MessageCard(
                            title = stringResource(Res.string.scan_permission_required_title),
                            body = stringResource(Res.string.scan_permission_required_body),
                            primary = stringResource(Res.string.scan_permission_denied_open_settings),
                            onPrimary = { permissions.openSettings() },
                            secondary = stringResource(Res.string.scan_manual_entry),
                            onSecondary = vm::onCancel,
                        )

                    ScannerUiState.Timeout ->
                        MessageCard(
                            title = stringResource(Res.string.scan_timeout_title),
                            body = stringResource(Res.string.scan_timeout_body),
                            primary = stringResource(Res.string.scan_retry),
                            onPrimary = vm::onCancel,
                            secondary = stringResource(Res.string.scan_manual_entry),
                            onSecondary = vm::onCancel,
                        )

                    ScannerUiState.CameraUnavailable ->
                        MessageCard(
                            title = stringResource(Res.string.scan_camera_unavailable_title),
                            body = stringResource(Res.string.scan_camera_unavailable_body),
                            primary = stringResource(Res.string.scan_manual_entry),
                            onPrimary = vm::onCancel,
                            secondary = null,
                            onSecondary = null,
                        )

                    ScannerUiState.Cancelled -> Unit
                }
            }
        }
    }
}

/** What went wrong, on a dark card at the bottom: the payment sheet's camera intro has the same card. */
@Composable
private fun MessageCard(
    title: String,
    body: String,
    primary: String,
    onPrimary: () -> Unit,
    secondary: String?,
    onSecondary: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1C1E), RoundedCornerShape(22.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Read out as it appears: the scan has stopped and the buyer must choose what next.
        Text(
            title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Assertive
                },
        )
        Text(body, color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
        PlateButton(primary, onPrimary, primary = true)
        if (secondary != null && onSecondary != null) PlateButton(secondary, onSecondary, primary = false)
    }
}

/** Full-width button over the camera: white for the main action, translucent for the other. */
@Composable
private fun PlateButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (primary) Color.White else Color.White.copy(alpha = 0.16f))
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (primary) Color.Black else Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CircleAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}
