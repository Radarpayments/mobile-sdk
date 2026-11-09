# PAYRDR Mobile SDK (Kotlin Multiplatform)

Unified Kotlin Multiplatform implementation of the PAYRDR Mobile SDK for Android and iOS.
Supersedes the separate `android-sdk` and `ios-sdk` repositories starting from version `4.0.0`.

## Modules

- **sdk-core** — headless tokenization library (card validation, RSA cryptogram, payment-string processor).
- **sdk-payment** — payment SDK with UI (Compose Multiplatform), PaymentApi, 3DS web challenge, Apple Pay / Google Pay.
- **sdk-payment-scanner** — optional card scanner (ML Kit on Android, Vision framework on iOS).

## Install

Versions: [GitHub releases](https://github.com/Radarpayments/mobile-sdk/releases); the first one is `4.0.0-beta.1`.
Android minSdk 24 and Kotlin 2.1 or newer in the app (the SDK's Compose Multiplatform 1.10 needs it), iOS 14.

**Android** — Maven Central:

```kotlin
dependencies {
    implementation("io.github.radarpayments:sdk-payment:4.0.0-beta.1")
    implementation("io.github.radarpayments:sdk-payment-scanner:4.0.0-beta.1") // optional: the card scanner
    implementation("com.google.android.gms:play-services-wallet:19.4.0") // optional: Google Pay
}
```

Headless tokenization only: `io.github.radarpayments:sdk-core`. Each release also has the AARs to download. The
manifest needs nothing: the internet permission and the SDK's activities are merged in, and the scanner adds the camera
permission (add `<uses-feature android:name="android.hardware.camera" android:required="false" />` to stay listed for
devices without a camera).

**iOS** — Swift Package Manager: File → Add Package Dependencies → `https://github.com/Radarpayments/mobile-sdk`,
product `PayrdrSDK`, then `import sdk_payment_scanner` (the whole SDK: checkout, SDK Core, card scanner). Each release
also has `sdk_payment_scanner.xcframework.zip` to embed by hand. Apple Pay needs the Apple Pay capability with your
merchant id (Signing & Capabilities); the card scanner needs `NSCameraUsageDescription` in `Info.plist`, or App Store
review rejects the build.

**Flutter, React Native** — the plugins are in each GitHub release: see [Flutter](plugins/flutter/README.md),
[React Native](plugins/react-native/README.md).

Pre-releases (`4.1.0-beta.1`, `4.1.0-rc.1`) are taken only when asked for exactly; alphas are only on GitHub, with a
Maven repository to download.

## Quick start

A payment takes four steps: describe the checkout, get an order from your server, open the payment sheet, handle the
result.

**1. Your server registers the order.** The app never holds the merchant's login, password or API key. Through API V1
the server calls `register.do` and hands the app an mdOrder; through API V2 it calls `POST /api2/sessions` and hands
over a session id (`ps_…`) — set its `successUrl` and `failureUrl` to `sdk://done` or to your own pages. Google Pay and
Apple Pay show in the sheet only when the gateway has enabled them for the merchant.

**2. Describe the checkout and 3. open the sheet — Android.** Build the checkout while the screen is being created
(in a field or in `onCreate`: it registers an activity result), so the result arrives even after a rotation:

```kotlin
val config = SdkPaymentConfig(
    baseUrl = "https://dev.bpcbt.com/payment",
    googlePay = GooglePayConfig(), // optional; the rest comes from the gateway and the order
    cardScannerProvider = MlKitVisionCardScannerProvider(), // optional; needs sdk-payment-scanner
)

class CartActivity : AppCompatActivity() {
    private val checkout = PayrdrCheckout.Builder(config) { result -> handle(result) }.build(this)

    fun onPayClick(mdOrder: String) = checkout.present(mdOrder) // API V2: checkout.presentSession(sessionId)
}
```

In Compose: `val checkout = rememberPayrdrCheckout(config) { result -> handle(result) }`. A plain Activity can use
`PayrdrCheckoutIntent` with `startActivityForResult`.

**2–3 — iOS.** On the main thread; keep the `PayrdrCheckout` while the sheet is open:

```swift
import sdk_payment_scanner

let config = SdkPaymentConfig(
    baseUrl: "https://dev.bpcbt.com/payment",
    applePay: ApplePayConfig(merchantId: "merchant.com.example"), // optional
    cardScannerProvider: MlKitVisionCardScannerProvider(logTransport: nil) // optional
)
let checkout = PayrdrCheckout(config: config)
checkout.present(mdOrder: mdOrder, from: viewController) { result in handle(result) }
// or: let result = try await checkout.present(mdOrder: mdOrder, from: viewController)
// API V2: checkout.presentSession(sessionId: sessionId, from: viewController) { result in … }
```

In SwiftUI: `.payrdrCheckout(isPresented: $paying, mdOrder: mdOrder, config: config) { result in … }` (mdOrder only;
a session opens through `presentSession`).

**4. Handle the result.** Every result carries `orderId`; your server, not the result, decides whether goods are
released.

```kotlin
when (result) {
    is CheckoutResult.Completed -> showPaid(result.orderId)
    is CheckoutResult.Failed -> showDeclined(result.error) // the payment definitely did not happen
    is CheckoutResult.Canceled -> Unit // the buyer closed the sheet
    is CheckoutResult.Unknown -> checkOnServer(result.orderId) // sent, outcome not known: do not offer to pay again
}
```

In Swift: `switch onEnum(of: result) { case .completed(let paid): … case .failed, .canceled, .unknown … }`.

`SdkPaymentConfig` also takes `locale`, `theme` (`PaymentTheme` System / Light / Dark / Custom), `showResultScreen`
(the SDK shows its own success or declined screen first), `logTransport` (your log sink; the SDK has no crash
reporter) and `sslPinning`.

## Languages

The checkout's screens are translated into English, Russian, Romanian, Greek, Bulgarian, German, French, Spanish
and Arabic (laid out right to left). By default they follow the device language; to fix one, pass it in the
config — `SdkPaymentConfig(baseUrl = …, locale = "de")` — or wrap standalone screens in `PaymentLocaleProvider`.
The order amount is written the way that language writes amounts (`€1,234.50`, `1.234,50 €`).

## Build

```bash
./gradlew build                           # Build all modules + run all tests
./gradlew :sdk-core:allTests              # Run sdk-core tests on all targets
./gradlew :sdk-core:iosSimulatorArm64Test # Run sdk-core tests on iOS simulator (requires macOS)
```

## Documentation

- [Card scanner](sdk-payment-scanner/README.md)
- [Bridge for Flutter and React Native: the JSON contract](sdk-bridge/README.md)
- [Flutter plugin](plugins/flutter/README.md), [React Native plugin](plugins/react-native/README.md)
- [Sample apps](samples/README.md)

## License

[MIT](LICENSE.md) © BPC
