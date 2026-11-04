# sdk-payment-scanner

Optional camera-based card scanner for `sdk-payment`. Uses ML Kit on Android and Vision on iOS.
Extracts PAN and expiry date; cardholder extraction is opt-in. Pure offline — no network calls.

## Gradle

```kotlin
implementation("io.github.radarpayments:sdk-payment-scanner:<version>")
```

## Wire it up

```kotlin
val sdk = SdkPayment.create(
    SdkPaymentConfig(
        // …your existing config…
        cardScannerProvider = MlKitVisionCardScannerProvider(
            // optional: route scanner events through your existing LogTransport
            logTransport = myLogTransport,
        ),
    ),
)
```

When `cardScannerProvider` is set, a camera icon appears next to the PAN field in the card form.
On successful scan the form is pre-filled with PAN, expiry and (if `extractCardholder = true`)
cardholder; otherwise the user continues manually.

## Android

- `AndroidManifest.xml` merges in `<uses-permission android:name="android.permission.CAMERA" />`
  automatically.
- `<uses-feature android:name="android.hardware.camera" android:required="false" />` keeps your
  app visible on the Play Store to users with camera-less devices.
- Adds ≈ 2.8 MB to your APK (ML Kit bundled text-recognition model + CameraX).
- `androidx.startup` is used to capture the application context; no manual `Application.onCreate`
  wiring is required in a standard app.

## iOS

- **You MUST add `NSCameraUsageDescription`** to your app's `Info.plist`. App Store review will
  reject builds without it. Suggested text:

  > The camera is used to scan your payment card so you don't have to type the number.

- The module ships a `PrivacyInfo.xcprivacy` declaring `NSPrivacyAccessedAPICategoryCameraUsage`.
  Your app-level privacy manifest must still cover its own disclosures.
- 0 MB binary cost (Vision and AVFoundation are system frameworks).

## Configuration

```kotlin
CardScannerConfig(
    extractCardholder = false, // default; set true to attempt cardholder OCR (~60-70% accuracy)
    timeoutSeconds = 30,       // default; scanner auto-returns Failed(Timeout) after this
    enableTorchButton = true,  // default; set false to hide the torch icon
)
```

## Logging

Scanner emits `LogEvent`s to the `Logger` you pass in. Tag is always `"CardScanner"`; messages
follow a stable `event=<name> key=value` shape and never contain PAN digits:

- `event=permission_granted` / `event=permission_denied`
- `event=started`
- `event=success elapsed_ms=<N>`
- `event=cancelled`
- `event=timeout elapsed_ms=<N>`
- `event=camera_unavailable`

## Manual verification checklist

Run through before shipping a change to this module:

- [ ] Physical iPhone, good light — PAN + expiry recognised in < 2 s.
- [ ] Low-end physical Android — same.
- [ ] Low-light — torch toggle works.
- [ ] Permission denied → tap "Open settings" → grant → reopen scanner → works.
- [ ] Damaged / scuffed card — 30 s timeout, user can fall back to manual entry.
- [ ] Cancelled mid-scan → returns to card form without modifying fields.

## Non-goals for v1

- NFC / EMV reading (distinct module if added later).
- CVV scanning (on card back; distinct flow).
- Distinguishing "extraction disabled" from "extraction attempted but not found" at the result
  boundary — both collapse to `cardholder = null`.

## Known follow-ups

- The iOS SampleApp (`samples/ios-app`) does not yet link the scanner XCFramework; the provider
  is therefore unwired in Swift. Wiring requires a dedicated `SdkPaymentScanner.xcframework`
  build+embed step in the SampleApp's Xcode project.
- UI-layer smoke tests (Compose instrumentation on Android, simulator on iOS) are tracked as a
  follow-up; they require refactoring the platform controllers from `expect class` to
  `interface + expect class DefaultXxx : Interface` so that fakes can be injected.
