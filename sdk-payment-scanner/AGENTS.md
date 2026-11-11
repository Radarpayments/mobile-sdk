# sdk-payment-scanner — agent notes

Read the root `AGENTS.md` first, and `README.md` here (what the integrator and the buyer see, log events).

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

`sdk-payment/src/commonMain/.../payment/scanner/`: `CardScannerProvider` (an `expect` interface: on Android it has
`@Composable CardScannerScreen(config, onResult)`, on iOS no members — the SwiftUI checkout offers scanning when the
provider is also a `NativeCardScanner` (`openScanner(config, language, onResult)`); either **must call `onResult`
exactly once**), `CardScannerConfig`, `ScannedCard` (4-digit year),
`CardScannerResult` (`Success`, `Cancelled`, `PermissionDenied`, `Failed(Timeout | CameraUnavailable | Unknown)`).
The card form in `sdk-payment` (`CardFormScreen.kt`) fills PAN and expiry on success, closes the scanner on any result,
and always passes the default `CardScannerConfig()`.

## Map

`src/commonMain/kotlin/net/payrdr/mobile/payment/sdk/payment/scanner/`

| Part | What it does |
|---|---|
| `provider/MlKitVisionCardScannerProvider.kt` | the public entry point (`expect class`; the Android actual draws the Compose screen, the iOS actual is a `NativeCardScanner` opening `VisionScannerSession`) |
| `state/ScannerViewModel.kt` | 3 matching frames in a row before `Success`; timeout; on denied / timeout / no camera it parks the result and emits it on cancel, so the message card stays up |
| parser (`CardParser`) | PAN 13–19 digits with Luhn (longest wins), expiry `MM/YY(YY)` on or below the PAN line and not in the past, issuer-word blocklist for the cardholder |
| `camera/` | `expect` `TextRecognizer`, `CameraPermissionController`, `TorchController`; coordinates are top-left 0..1 (`NormalisedRect`). The preview is platform code: androidMain `CameraPreview` (Compose), iosMain `CameraFeed` (AVFoundation) |
| `presentation/ScannerTexts.kt` | the scanner's texts by language, from the generated tables |
| UI | Android: androidMain `ui/CardScannerScreen` + overlay (Compose). iOS: SwiftUI in `sdk-payment/src/iosMain/swift/UI/Scanner`, driven by iosMain `presentation/VisionScannerSession.kt` |
| strings | `src/commonMain/strings/values*/strings.xml` (9 languages) → generated `PaymentStringTables` in `presentation` (common) and Android `Res.string` keys in `…sdk.sdk.payment.scanner.generated.resources` (the doubled `sdk` is real) |

Platform files are named `X.android.kt` / `X.ios.kt`.

## Platform rules

- Android: the manifest merges `CAMERA`, `uses-feature camera required=false` and an androidx.startup
  `ScannerInitializer`. `ContextHolder` keeps the app context and, weakly, the last resumed `ComponentActivity` for the
  permission dialog — the host app must use androidx.activity.
- iOS: `src/nativeInterop/cinterop/scannervision.def` defines an Objective-C (ARC) `payrdr_recognize_text`. It exists
  because Vision objects created from Kotlin hold the camera pixel buffer until the Kotlin GC runs, which freezes the
  preview. Do not move recognition back into Kotlin.
- iOS `CameraFeed`: `CFRetain`/`CFRelease` around the sample buffer, one frame at a time behind a `Mutex`,
  `startRunning`/`stopRunning` on a serial queue. The framework's iOS minimum is overridden to 14.0 — the comment in
  `build.gradle.kts` explains the CoreMedia crash below 12.2.
- `PrivacyInfo.xcprivacy` declares the camera reason; the host app adds `NSCameraUsageDescription`.
- **No PAN digits in logs**, ever: log events are `event=…` with tag `CardScanner`, as listed in the README.
- `CardScannerConfig` (`extractCardholder`, `timeoutSeconds`, `enableTorchButton`) is public, but no integrator can set
  it: the provider takes no config and the card form passes the default. So the cardholder is never read through the
  SDK UI, and "cardholder extraction off" and "cardholder not found" both end as `cardholder = null`.

## Tests

```bash
./gradlew :sdk-payment-scanner:testDebugUnitTest :sdk-payment-scanner:iosSimulatorArm64Test
```

- commonTest: `CardParserTest`, `ScannerViewModelTest` (Turbine + `StandardTestDispatcher`).
- androidUnitTest: `LatestResumedTest` (the `ContextHolder` activity tracking).
- The scanner screens on iOS are covered by the sample app's XCUITests (`tools/visual`, screen 91). iosTest names
  would be snake_case: Kotlin/Native rejects some characters (commas) in backtick names.
- Camera behaviour itself is checked by hand on devices before shipping a change to this module:
  - a physical iPhone and a low-end physical Android, good light: PAN and expiry read in under 2 s;
  - low light: the torch toggle works;
  - permission denied → "Open settings" → grant → reopen the scanner: it scans;
  - a scuffed card: the 30 s timeout appears and the buyer can type the card instead;
  - closing mid-scan returns to the card form with the fields untouched.
- Not covered by tests yet: the camera controllers (`TextRecognizer`, `TorchController`, `CameraPermissionController`)
  are `internal expect class`, so no fake can be injected. Testing them needs `interface` + `expect class DefaultX`.

## Out of scope

NFC / EMV reading and CVC scanning (back of the card, a different flow) — each would be a separate module or flow.
