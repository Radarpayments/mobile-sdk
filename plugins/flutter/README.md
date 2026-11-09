# payrdr_sdk (Flutter)

Opens the PAYRDR payment sheet in a Flutter app. These are the native SDK's own screens: card payment with 3-D Secure,
saved cards, Google Pay, Apple Pay and the card scanner. The plugin also gives you SDK Core, which encrypts card data
for merchants who are PCI DSS certified and keep their own card form.

## Requirements

Android minSdk 24, iOS 15.

## Install

The plugin is not on pub.dev yet. Each [GitHub release](https://github.com/Radarpayments/mobile-sdk/releases) has it
as `payrdr_sdk-<version>.tar.gz`, with the iOS framework inside. Unpack it next to your app and add it by path:

```yaml
dependencies:
  payrdr_sdk:
    path: ../payrdr_sdk
```

On Android the plugin downloads the SDK from Maven Central. Alpha versions aren't there: an alpha release has
`payrdr-sdk-maven-<sdk-version>.zip` instead. Unpack it and set `payrdrMavenUrl` in `android/gradle.properties` to the
unpacked folder. (The plugin's version is 1.x; the SDK's is 4.x.)

You don't need to change anything else:

- `MainActivity` stays a plain `FlutterActivity`.
- The Android manifest needs no entries: Google Pay and the camera permission come with the SDK.
- On iOS the plugin works with Swift Package Manager (Flutter's default from 3.44) and with CocoaPods.

Two iOS features need setup:

- Apple Pay: add the Apple Pay capability with your merchant ID (Runner → Signing & Capabilities).
- Card scanner: add `NSCameraUsageDescription` to `Info.plist`.

## Take a payment

Your server registers the order at the gateway and gives the app its `mdOrder`. Never put the merchant's login,
password or API key in the app.

```dart
import 'package:payrdr_sdk/payrdr_sdk.dart';

const checkout = PayrdrCheckout(PayrdrConfig(
  baseUrl: 'https://pay.example.com/payment',
  theme: PayrdrTheme.custom(colors: PayrdrColors(primary: Color(0xFF1C1917)), cornerRadius: 12),
  googlePay: GooglePayOptions(), // everything else comes from the gateway
  applePay: ApplePayOptions(merchantId: 'merchant.com.example'),
  cardScanner: true,
));

final mdOrder = await myBackend.registerOrder(cart);

switch (await checkout.present(mdOrder)) {
  case CheckoutCompleted():
    showPaid();
  case CheckoutCanceled():
    break;
  case CheckoutFailed(:final error):
    showError(error.message); // error.code: CheckoutErrorCode.declined, .network, …
  case CheckoutUnknown(:final orderId):
    checkOnServer(orderId);
}
```

For an API V2 session, call `checkout.presentSession(sessionId)` with the `ps_…` ID your server created.

`CheckoutUnknown` means the payment was sent but its outcome isn't known. Ask your server how the order ended, and
don't offer to pay again until it knows.

`PayrdrConfig` also takes `locale`, `showResultScreen`, `keyProviderUrl`, `sslPins` and `debugLogging`. They mean the
same as in the native SDK; see the [root README](../../README.md#configuration).

## SDK Core

```dart
final result = await PayrdrCore.generateToken(const CoreTokenRequest.card(
  mdOrder: mdOrder, pan: '4111111111111111', expiry: '12/30', cvc: '123', pubKey: gatewayPublicKeyPem,
));
if (result.isSuccessful) {
  sendToServer(result.token!); // the server pays with it as seToken
} else {
  showFieldErrors(result.errors); // {CoreField.pan: 'required', …}
}
```

Other requests: `CoreTokenRequest.cardWithoutOrder`, `.savedCard` (by `bindingId`) and `.savedCardWithoutOrder`.
`generateToken` throws `ArgumentError` if the request itself is malformed.

## How it works

The Dart code turns the config into JSON and sends it over one method channel. On the native side `sdk-bridge` opens
the sheet and sends the result back as JSON. The JSON format is described in
[sdk-bridge/README.md](../../sdk-bridge/README.md).

## Working on the plugin in this repository

The plugin uses the SDK built from this repository. Run `scripts/build-bridge.sh` from the repository root once, and
again after changing the SDK. It publishes the Android libraries to `build/maven` and copies
`PayrdrSdkBridge.xcframework` into `ios/payrdr_sdk/Frameworks`.

An app that gets the plugin some other way sets `payrdrMavenUrl` (and, if needed, `payrdrSdkVersion`) in its
`android/gradle.properties` to the Maven repository with the SDK.

Tests: `flutter test`. It covers the Dart API and the shared contract files in `sdk-bridge/contract`.
