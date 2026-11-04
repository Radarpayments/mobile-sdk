# sdk-payment-scanner — agent notes

Read the root `AGENTS.md` first, and `README.md` here (behaviour, log events, the manual device checklist).

## What this module is

The optional camera card scanner: CameraX + ML Kit text recognition on Android, AVFoundation + Vision on iOS. Offline;
it reads the PAN and expiry, the cardholder only on request. The merchant turns it on by passing
`MlKitVisionCardScannerProvider(logTransport)` as `SdkPaymentConfig.cardScannerProvider`. That class is the module's
whole public API (`api/sdk-payment-scanner.api`).

On iOS this module is also **the framework merchants link**: `sdk_payment_scanner.xcframework` re-exports
`sdk-payment`, so `import sdk_payment_scanner` brings the whole SDK. An app must not link `sdk_payment.xcframework`
next to it (two Swift types for one Kotlin class). `sdk-core` is deliberately not exported, so its Swift names keep the
`Sdk_core*` prefix. This XCFramework, release build, is what a release ships through Swift Package Manager
(`Package.swift` in the repository root, written by `scripts/package-release.sh`).

The Swift conveniences for `sdk-payment` types live in `sdk-payment/src/iosMain/swift/`: SKIE packs them into that
module's klib and they reach this framework with the export.

## The contract (lives in sdk-payment)

`sdk-payment/src/commonMain/.../payment/scanner/`: `CardScannerProvider` (`@Composable CardScannerScreen(config,
onResult)`, which **must call `onResult` exactly once**), `CardScannerConfig`, `ScannedCard` (4-digit year),
`CardScannerResult` (`Success`, `Cancelled`, `PermissionDenied`, `Failed(Timeout | CameraUnavailable | Unknown)`).
The card form in `sdk-payment` (`CardFormScreen.kt`) fills PAN and expiry on success, closes the scanner on any result,
and always passes the default `CardScannerConfig()`.

## Map

`src/commonMain/kotlin/net/payrdr/mobile/payment/sdk/payment/scanner/`

| Part | What it does |
|---|---|
| `provider/MlKitVisionCardScannerProvider.kt` | the public entry point |
| `state/ScannerViewModel.kt` | 3 matching frames in a row before `Success`; timeout; on denied / timeout / no camera it parks the result and emits it on cancel, so the message card stays up |
| parser (`CardParser`) | PAN 13–19 digits with Luhn (longest wins), expiry `MM/YY(YY)` on or below the PAN line and not in the past, issuer-word blocklist for the cardholder |
| `camera/` | `expect` `TextRecognizer`, `CameraPermissionController`, `TorchController`, `@Composable CameraPreview`; coordinates are top-left 0..1 (`NormalisedRect`) |
| UI | `CardScannerScreen` + overlay; strings in `composeResources/values*` (9 languages), generated package `…sdk.sdk.payment.scanner.generated.resources` (the doubled `sdk` is real) |

Platform files are named `X.android.kt` / `X.ios.kt`.

## Platform rules

- Android: the manifest merges `CAMERA`, `uses-feature camera required=false` and an androidx.startup
  `ScannerInitializer`. `ContextHolder` keeps the app context and, weakly, the last resumed `ComponentActivity` for the
  permission dialog — the host app must use androidx.activity.
- iOS: `src/nativeInterop/cinterop/scannervision.def` defines an Objective-C (ARC) `payrdr_recognize_text`. It exists
  because Vision objects created from Kotlin hold the camera pixel buffer until the Kotlin GC runs, which freezes the
  preview. Do not move recognition back into Kotlin.
- iOS `CameraPreview`: `CFRetain`/`CFRelease` around the sample buffer, one frame at a time behind a `Mutex`,
  `startRunning`/`stopRunning` on a serial queue. The framework's iOS minimum is overridden to 14.0 — the comment in
  `build.gradle.kts` explains the CoreMedia crash below 12.2.
- `PrivacyInfo.xcprivacy` declares the camera reason; the host app adds `NSCameraUsageDescription`.
- **No PAN digits in logs**, ever: log events are `event=…` with tag `CardScanner`, as listed in the README.

## Tests

```bash
./gradlew :sdk-payment-scanner:testDebugUnitTest :sdk-payment-scanner:iosSimulatorArm64Test
```

- commonTest: `CardParserTest`, `ScannerViewModelTest` (Turbine + `StandardTestDispatcher`).
- androidUnitTest: `LatestResumedTest` (the `ContextHolder` activity tracking).
- iosTest: `CardScannerOverlayTest`, a pixel check with `runComposeUiTest` — Compose UI tests need no device on iOS.
  Test names there are snake_case: Kotlin/Native rejects some characters (commas) in backtick names.
- Camera behaviour itself is checked by hand on devices (README checklist).
