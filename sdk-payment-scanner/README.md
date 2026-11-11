# Card scanner

An optional part of the payment sheet. When it is on, a camera icon appears next to the card number field. The buyer
points the camera at the card, and the scanner fills in the card number and expiry date. If the scan doesn't work out,
the buyer just types the number.

The scanner works offline and sends nothing over the network. It uses ML Kit on Android and Apple's Vision on iOS. It
doesn't read the cardholder name or the CVC.

## Turn it on

Pass the provider in the config you already give the checkout:

```kotlin
val config = SdkPaymentConfig(
    baseUrl = "https://dev.bpcbt.com/payment",
    cardScannerProvider = MlKitVisionCardScannerProvider(),
)
```

```swift
import RadarpaymentsSDKPayment
import RadarpaymentsSDKScanner

let config = SdkPaymentConfig(
    baseUrl: "https://dev.bpcbt.com/payment",
    cardScannerProvider: MlKitVisionCardScannerProvider()
)
```

To see the scanner's events in your logs, pass your `LogTransport`: `MlKitVisionCardScannerProvider(logTransport = …)`
(`logTransport:` in Swift).

## Android

Add the dependency:

```kotlin
implementation("io.github.radarpayments:sdk-payment-scanner:<version>")
```

Nothing else is needed. The library's manifest brings the camera permission and declares the camera as not required,
so the app still installs on devices without one. Your app must use AndroidX Activity: the scanner asks for the camera
permission through it.

The scanner makes the app bigger: ML Kit ships its text recognition model inside the app, about 11 MB for each
architecture (arm64-v8a, armeabi-v7a, x86, x86_64). A universal APK grows by about 40 MB; an app bundle delivers one
architecture per device.

## iOS

Add the `RadarpaymentsSDKScanner` product of the package next to `RadarpaymentsSDKPayment`. It is a small Swift module
with the camera; an app that leaves it out has no camera code at all. It uses the system's Vision and adds no model of
its own.

Without Swift Package Manager, add the Swift files from `RadarpaymentsSDKScanner-<version>.zip` (in each release) to
your app target; then there is no `import RadarpaymentsSDKScanner`.

Add `NSCameraUsageDescription` to your `Info.plist`. Without it App Store review rejects the build. For example:

> The camera is used to scan your payment card so you don't have to type the number.

## What the buyer sees

- The first time, the system asks for camera access. If the buyer refuses, the scanner explains that it needs the
  camera and offers to open Settings or to enter the card by hand.
- In poor light the buyer can turn on the torch.
- If nothing is read in 30 seconds, the scanner suggests better light or typing the details. Either button takes the
  buyer back to the card form; the camera icon starts a new scan.
- Closing the scanner leaves the card form as it was.

## Log events

The scanner writes to your `LogTransport` with the tag `CardScanner`. Messages look like `event=<name> key=value` and
never contain card digits:

| Event | When |
|---|---|
| `event=permission_granted`, `event=permission_denied` | The buyer answered the camera permission request |
| `event=started` | The camera started |
| `event=success elapsed_ms=<N>` | The card was read |
| `event=cancelled` | The buyer closed the scanner |
| `event=timeout elapsed_ms=<N>` | Nothing was read in 30 seconds |
| `event=camera_unavailable` | The device has no usable camera |
