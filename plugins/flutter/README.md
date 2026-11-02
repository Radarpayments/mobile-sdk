# payrdr_sdk (Flutter)

The payrdr payment sheet in a Flutter app: card payment with 3-D Secure, saved cards, Google Pay, Apple Pay and the card
scanner — the native SDK's own screens, opened over your app. Plus SDK Core: card encryption for a merchant with PCI DSS
who keeps its own card form.

## Install

```yaml
dependencies:
  payrdr_sdk:
    path: ../plugins/flutter   # in this repository; see "From this repository" below
```

Nothing else is required: `MainActivity` stays a `FlutterActivity`, the Android manifest needs no entries (Google Pay
and the camera permission come with the SDK), and iOS works with Swift Package Manager (Flutter's default) as well as
CocoaPods. Android minSdk 24, iOS 15.

On iOS, per feature: Apple Pay needs the Apple Pay capability with your merchant id (Runner → Signing & Capabilities),
the card scanner needs `NSCameraUsageDescription` in `Info.plist`.

## Pay

```dart
import 'package:payrdr_sdk/payrdr_sdk.dart';

// 1. Describe the sheet once.
const checkout = PayrdrCheckout(PayrdrConfig(
  baseUrl: 'https://pay.example.com/payment',
  theme: PayrdrTheme.custom(colors: PayrdrColors(primary: Color(0xFF1C1917)), cornerRadius: 12),
  googlePay: GooglePayOptions(),                            // everything from the gateway
  applePay: ApplePayOptions(merchantId: 'merchant.com.example'),
  cardScanner: true,
));

// 2. Your server registers the order at the gateway and gives the app its mdOrder.
final mdOrder = await myBackend.registerOrder(cart);

// 3. Open the sheet. 4. Handle the result.
switch (await checkout.present(mdOrder)) {
  case CheckoutCompleted():
    showPaid();
  case CheckoutCanceled():
    break;
  case CheckoutFailed(:final error):
    showError(error.message);   // error.code: CheckoutErrorCode.declined, .network, ...
  case CheckoutUnknown(:final orderId):
    checkOnServer(orderId);      // sent, outcome not known yet: never offer to pay again meanwhile
}
```

Never put the merchant's API login and password in the app: registering orders is your server's job.

## SDK Core

```dart
final result = await PayrdrCore.generateToken(const CoreTokenRequest.card(
  mdOrder: mdOrder, pan: '4111111111111111', expiry: '12/30', cvc: '123', pubKey: gatewayPublicKeyPem,
));
if (result.isSuccessful) sendToServer(result.token!);   // the server pays with it as seToken
else showFieldErrors(result.errors);                    // {CoreField.pan: 'required', ...}
```

Also `CoreTokenRequest.cardWithoutOrder`, `.savedCard` (bindingId) and `.savedCardWithoutOrder`.

## How it works

The Dart API turns the config into JSON and sends it over one method channel; the native side (`sdk-bridge`, the SDK's
Kotlin module for cross-platform apps) opens the sheet and answers with the result as JSON. The contract is in
[`sdk-bridge/README.md`](../../sdk-bridge/README.md).

## From this repository

The plugin takes the SDK from this repository's build: run `scripts/build-bridge.sh` from the repository root once (and
after SDK changes). It publishes the Android libraries to `build/maven` and copies `PayrdrSdkBridge.xcframework` into
`ios/payrdr_sdk/Frameworks`. An app that gets the plugin from elsewhere sets `payrdrMavenUrl` (and optionally
`payrdrSdkVersion`) in its `android/gradle.properties` to the repository that serves the SDK.

Tests: `flutter test` (the Dart side); the JSON contract is tested in `sdk-bridge`.
