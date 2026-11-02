package net.payrdr.mobile.payment.sdk.payment.scanner.state

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerConfig
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerResult
import net.payrdr.mobile.payment.sdk.payment.scanner.ScannedCard
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {
    private val card = ScannedCard("4111111111111111", 12, 2029, null)

    @Test
    fun `initial state is Initializing`() =
        runTest {
            val vm = ScannerViewModel(scope = this, config = CardScannerConfig(), dispatcher = StandardTestDispatcher(testScheduler))
            vm.state.value shouldBe ScannerUiState.Initializing
        }

    @Test
    fun `permission granted transitions to Scanning`() =
        runTest {
            val vm = ScannerViewModel(scope = this, config = CardScannerConfig(), dispatcher = StandardTestDispatcher(testScheduler))
            vm.onPermissionResult(granted = true)
            vm.state.value shouldBe ScannerUiState.Scanning
        }

    @Test
    fun `permission denied parks state at PermissionDenied and emits PermissionDenied only on cancel`() =
        runTest {
            val vm = ScannerViewModel(scope = this, config = CardScannerConfig(), dispatcher = StandardTestDispatcher(testScheduler))
            vm.result.test {
                vm.onPermissionResult(granted = false)
                vm.state.value shouldBe ScannerUiState.PermissionDenied
                expectNoEvents() // host MUST NOT auto-dismiss; the screen stays mounted to show the message card
                vm.onCancel()
                awaitItem() shouldBe CardScannerResult.PermissionDenied
            }
        }

    @Test
    fun `anti-flicker requires three agreeing frames before Success`() =
        runTest {
            val vm = ScannerViewModel(scope = this, config = CardScannerConfig(), dispatcher = StandardTestDispatcher(testScheduler))
            vm.onPermissionResult(granted = true)
            vm.onParseResult(card)
            vm.state.value shouldBe ScannerUiState.Detecting(card.pan)
            vm.onParseResult(card)
            vm.state.value shouldBe ScannerUiState.Detecting(card.pan)
            vm.onParseResult(card)
            vm.state.value.shouldBeInstanceOf<ScannerUiState.Success>()
        }

    @Test
    fun `PAN flip resets the streak`() =
        runTest {
            val vm = ScannerViewModel(scope = this, config = CardScannerConfig(), dispatcher = StandardTestDispatcher(testScheduler))
            vm.onPermissionResult(granted = true)
            vm.onParseResult(card)
            vm.onParseResult(card.copy(pan = "4242424242424242"))
            vm.onParseResult(card)
            vm.state.value shouldBe ScannerUiState.Detecting(card.pan) // streak is 1, not 3
        }

    @Test
    fun `timeout parks state at Timeout and emits Failed with Timeout only on cancel`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val vm = ScannerViewModel(scope = this, config = CardScannerConfig(timeoutSeconds = 5), dispatcher = dispatcher)
            vm.result.test {
                vm.onPermissionResult(granted = true)
                advanceTimeBy(5_001)
                vm.state.value shouldBe ScannerUiState.Timeout
                expectNoEvents()
                vm.onCancel()
                awaitItem() shouldBe CardScannerResult.Failed(CardScannerResult.Reason.Timeout)
            }
        }

    @Test
    fun `onCancel from any state emits Cancelled`() =
        runTest {
            val vm = ScannerViewModel(scope = this, config = CardScannerConfig(), dispatcher = StandardTestDispatcher(testScheduler))
            vm.result.test {
                vm.onCancel()
                vm.state.value shouldBe ScannerUiState.Cancelled
                awaitItem() shouldBe CardScannerResult.Cancelled
            }
        }

    @Test
    fun `camera unavailable parks state at CameraUnavailable and emits Failed with CameraUnavailable only on cancel`() =
        runTest {
            val vm = ScannerViewModel(scope = this, config = CardScannerConfig(), dispatcher = StandardTestDispatcher(testScheduler))
            vm.result.test {
                vm.onCameraError()
                vm.state.value shouldBe ScannerUiState.CameraUnavailable
                expectNoEvents()
                vm.onCancel()
                awaitItem() shouldBe CardScannerResult.Failed(CardScannerResult.Reason.CameraUnavailable)
            }
        }

    @Test
    fun `emits event=started on permission granted`() =
        runTest {
            val events = mutableListOf<net.payrdr.mobile.payment.sdk.payment.logger.LogEvent>()
            val logger =
                net.payrdr.mobile.payment.sdk.payment.logger.Logger(
                    transports =
                        listOf(
                            net.payrdr.mobile.payment.sdk.payment.logger
                                .LogTransport { events += it },
                        ),
                )
            val vm =
                ScannerViewModel(
                    scope = this,
                    config = CardScannerConfig(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                    logger = logger,
                )
            vm.onPermissionResult(true)
            val started = events.single { it.message.startsWith("event=started") }
            started.level shouldBe net.payrdr.mobile.payment.sdk.payment.logger.LogLevel.INFO
            started.tag shouldBe "CardScanner"
        }

    @Test
    fun `emits event=success with elapsed_ms and no PAN digits`() =
        runTest {
            val events = mutableListOf<net.payrdr.mobile.payment.sdk.payment.logger.LogEvent>()
            val logger =
                net.payrdr.mobile.payment.sdk.payment.logger.Logger(
                    transports =
                        listOf(
                            net.payrdr.mobile.payment.sdk.payment.logger
                                .LogTransport { events += it },
                        ),
                )
            val vm =
                ScannerViewModel(
                    scope = this,
                    config = CardScannerConfig(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                    logger = logger,
                )
            vm.onPermissionResult(true)
            vm.onParseResult(card)
            vm.onParseResult(card)
            vm.onParseResult(card)
            val success = events.single { it.message.startsWith("event=success") }
            success.level shouldBe net.payrdr.mobile.payment.sdk.payment.logger.LogLevel.INFO
            success.message.contains("elapsed_ms=") shouldBe true
            // No 13+ digit run in any logged message.
            events.forEach { Regex("\\d{13,}").containsMatchIn(it.message) shouldBe false }
        }

    @Test
    fun `emits event=timeout with elapsed_ms on timeout`() =
        runTest {
            val events = mutableListOf<net.payrdr.mobile.payment.sdk.payment.logger.LogEvent>()
            val logger =
                net.payrdr.mobile.payment.sdk.payment.logger.Logger(
                    transports =
                        listOf(
                            net.payrdr.mobile.payment.sdk.payment.logger
                                .LogTransport { events += it },
                        ),
                )
            val vm =
                ScannerViewModel(
                    scope = this,
                    config = CardScannerConfig(timeoutSeconds = 2),
                    dispatcher = StandardTestDispatcher(testScheduler),
                    logger = logger,
                )
            vm.onPermissionResult(true)
            advanceTimeBy(2_001)
            val timeout = events.single { it.message.startsWith("event=timeout") }
            timeout.level shouldBe net.payrdr.mobile.payment.sdk.payment.logger.LogLevel.WARNING
            timeout.message.contains("elapsed_ms=") shouldBe true
        }

    @Test
    fun `emits event=cancelled when user cancels`() =
        runTest {
            val events = mutableListOf<net.payrdr.mobile.payment.sdk.payment.logger.LogEvent>()
            val logger =
                net.payrdr.mobile.payment.sdk.payment.logger.Logger(
                    transports =
                        listOf(
                            net.payrdr.mobile.payment.sdk.payment.logger
                                .LogTransport { events += it },
                        ),
                )
            val vm =
                ScannerViewModel(
                    scope = this,
                    config = CardScannerConfig(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                    logger = logger,
                )
            vm.onCancel()
            events.single { it.message == "event=cancelled" }.level shouldBe net.payrdr.mobile.payment.sdk.payment.logger.LogLevel.INFO
        }

    @Test
    fun `emits event=camera_unavailable on camera error`() =
        runTest {
            val events = mutableListOf<net.payrdr.mobile.payment.sdk.payment.logger.LogEvent>()
            val logger =
                net.payrdr.mobile.payment.sdk.payment.logger.Logger(
                    transports =
                        listOf(
                            net.payrdr.mobile.payment.sdk.payment.logger
                                .LogTransport { events += it },
                        ),
                )
            val vm =
                ScannerViewModel(
                    scope = this,
                    config = CardScannerConfig(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                    logger = logger,
                )
            vm.onCameraError()
            val cu = events.single { it.message == "event=camera_unavailable" }
            cu.level shouldBe net.payrdr.mobile.payment.sdk.payment.logger.LogLevel.ERROR
        }

    @Test
    fun `emits event=permission_denied when permission refused`() =
        runTest {
            val events = mutableListOf<net.payrdr.mobile.payment.sdk.payment.logger.LogEvent>()
            val logger =
                net.payrdr.mobile.payment.sdk.payment.logger.Logger(
                    transports =
                        listOf(
                            net.payrdr.mobile.payment.sdk.payment.logger
                                .LogTransport { events += it },
                        ),
                )
            val vm =
                ScannerViewModel(
                    scope = this,
                    config = CardScannerConfig(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                    logger = logger,
                )
            vm.onPermissionResult(false)
            events.single { it.message == "event=permission_denied" }.level shouldBe
                net.payrdr.mobile.payment.sdk.payment.logger.LogLevel.WARNING
        }
}
