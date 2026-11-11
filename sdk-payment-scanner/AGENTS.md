# sdk-payment-scanner — agent notes

Read the root `AGENTS.md` first, and `README.md` here (what the integrator and the buyer see, log events).

## What this module is

The optional camera card scanner: CameraX + ML Kit text recognition on Android, AVFoundation + Vision on iOS. Offline;
it reads the PAN and expiry, the cardholder only on request. The merchant turns it on by passing
`MlKitVisionCardScannerProvider(logTransport)` as `SdkPaymentConfig.cardScannerProvider`. That class is the module's
whole public API for integrators (`api/sdk-payment-scanner.api`).

## iOS: three modules from two Gradle modules

| Swift module | Built from | What is inside |
|---|---|---|
| `RadarpaymentsSDKPayment` | this module's XCFramework | `sdk-payment` and `sdk-core` (both exported, so no `Sdk_core*` prefixes), the scanner's parser, state and SwiftUI screen — everything but the camera |
| `RadarpaymentsSDKScanner` | `ios/Sources/RadarpaymentsSDKScanner/` (Swift sources, an SPM target) | the camera: `VisionScannerCamera`, `VisionCameraFeed`, and `MlKitVisionCardScannerProvider.init(logTransport:)` |
| `RadarpaymentsSDKCore` | `sdk-core`'s XCFramework | SDK Core alone |

- Two Kotlin/Native frameworks cannot live in one app (two runtimes, two Swift types for one Kotlin class). So Core and
  Payment are either-or, and the scanner cannot be a Kotlin framework of its own: it is Swift on top of Payment.
- The payment binary must reference no capture API (`AVCapture…`, Vision): an app without the scanner product ships no
  camera code. The camera reaches Kotlin through `camera/ScannerCamera.kt` in iosMain (`@PayrdrUiApi`: public for the
  Swift module, hidden from the API dumps). The Kotlin `MlKitVisionCardScannerProvider(camera, logTransport)` takes
  it; the Swift module adds the `init(logTransport:)` integrators call.
- The Swift sources are not in `src/iosMain/swift`: SKIE would compile them into the framework. Each file starts with
  `#if canImport(RadarpaymentsSDKPayment) … #else import PayrdrSdkBridge #endif`, because the same files are compiled
  in three places: the SPM target (root `Package.swift`, written by `scripts/package-release.sh`), inside the Flutter
  and React Native plugins (`scripts/build-bridge.sh` copies them; both copies are gitignored), and in the iOS sample
  (one symlink per file in `samples/ios-app/SampleApp/Scanner/` — Xcode does not compile a symlinked folder, so a new
  source file needs a new symlink).
- `sdk-payment` keeps a framework of its own (`sdk_payment`) that is never shipped: CI links it to check that module's
  Swift alone.

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
| `provider/MlKitVisionCardScannerProvider.{android,ios}.kt` | the public entry point, one class per platform (not `expect`: the iOS one takes the camera). Android draws the Compose screen; iOS is a `NativeCardScanner` opening `VisionScannerSession` |
| `state/ScannerViewModel.kt` | 3 matching frames in a row before `Success`; timeout; on denied / timeout / no camera it parks the result and emits it on cancel, so the message card stays up |
| parser (`CardParser`) | PAN 13–19 digits with Luhn (longest wins), expiry `MM/YY(YY)` on or below the PAN line and not in the past, issuer-word blocklist for the cardholder |
| `camera/` | common: `RecognizedLine`, `NormalisedRect` (top-left 0..1), `CameraPermissionStatus`. androidMain: `TextRecognizer`, `CameraPermissionController`, `TorchController`, `CameraPreview` (Compose). iosMain: `ScannerCamera`, the contract the Swift camera implements |
| `presentation/ScannerTexts.kt` | the scanner's texts by language, from the generated tables |
| UI | Android: androidMain `ui/CardScannerScreen` + overlay (Compose). iOS: SwiftUI in `sdk-payment/src/iosMain/swift/UI/Scanner`, driven by iosMain `presentation/VisionScannerSession.kt` |
| strings | `src/commonMain/strings/values*/strings.xml` (9 languages) → generated `PaymentStringTables` in `presentation` (common) and Android `Res.string` keys in `…sdk.sdk.payment.scanner.generated.resources` (the doubled `sdk` is real) |

Platform files are named `X.android.kt` / `X.ios.kt`.

## Platform rules

- Android: the manifest merges `CAMERA`, `uses-feature camera required=false` and an androidx.startup
  `ScannerInitializer`. `ContextHolder` keeps the app context and, weakly, the last resumed `ComponentActivity` for the
  permission dialog — the host app must use androidx.activity.
- iOS `VisionCameraFeed.swift`: frames are read one at a time on a serial queue with late frames dropped, and
  everything holding a frame is released before the callback returns — the capture output has few buffers, and a held
  one freezes the preview (the reason recognition was never done from Kotlin, whose GC releases Vision objects late).
  `startRunning`/`stopRunning` run on a serial queue; a feed starts once and stops for good. `onText` arrives on a
  background thread with Vision's bottom-left boxes already flipped to top-left.
- The frameworks' iOS minimum is overridden to 14.0 (Kotlin/Native defaults to 12.0).
- The host app adds `NSCameraUsageDescription`. `src/iosMain/resources/PrivacyInfo.xcprivacy` is not packaged into
  any artifact yet.
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
- Not covered by tests yet: the Android camera controllers (`TextRecognizer`, `TorchController`,
  `CameraPermissionController`) are plain classes, so no fake can be injected; on iOS `ScannerCamera` is an interface
  and `VisionScannerSession` could be tested with a fake one. The Swift camera has no tests of its own; the release's
  SPM check (`.github/spm-check`) only proves it compiles and plugs into the config.

## Out of scope

NFC / EMV reading and CVC scanning (back of the card, a different flow) — each would be a separate module or flow.
