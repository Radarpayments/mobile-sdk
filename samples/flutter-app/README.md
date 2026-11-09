# Flutter sample

The [sample apps'](../README.md) demo, built on the `payrdr_sdk` plugin (`plugins/flutter`). It has everything the
native samples have: the stores Hearth & Co, Roast Lane and Stage Pass with the Scenario panel, Northwind Invest for AFT
transfers, SDK Core and the developer settings, in the same nine languages.

## Where the integration code is

| What | Where |
|---|---|
| Opening the payment sheet and handling the result | `lib/checkout/checkout.dart` |
| The SDK Core card form | `lib/core/` |
| "Your server": order registration | `lib/merchant/order_registrar.dart` |

The demo has no backend, so it registers orders itself, with the credentials from the developer settings:

- API V1: merchant login and password, `register.do` → an `mdOrder`. An AFT transfer uses `registerP2P.do`.
- API V2: the API key, `POST /api2/sessions` → a session ID.

A real app gets the order from its own backend and never contains merchant credentials.

## Native setup

The app has no native code for the SDK. `MainActivity` is a plain `FlutterActivity`, and the SDK's Android libraries
add the internet permission and the Google Pay entry to the manifest themselves.

On iOS the sample has what any app with these features needs:

- `NSCameraUsageDescription` in `Info.plist`, for the card scanner;
- the Apple Pay capability in `ios/Runner/Runner.entitlements`. Its merchant ID `merchant.net.payrdr.samples.test` is
  a placeholder: to try Apple Pay, put a real one there and in the developer settings.

iOS uses Swift Package Manager; there is no Podfile.

## Build and run

You need Flutter 3.44 or newer (Dart 3.13).

First run `scripts/build-bridge.sh` from the repository root. It builds the SDK into `build/maven` and
`plugins/flutter/ios/payrdr_sdk/Frameworks/PayrdrSdkBridge.xcframework`, where the plugin looks for it. Then:

```bash
flutter test
flutter run
```

On Android, leave `JAVA_HOME` unset: Flutter uses Android Studio's JDK.

`flutter test` covers the cart, checkout, order registration, Northwind, the settings storage and the screens.

## Texts

The texts are taken from the Android sample's string resources. After changing them there, run:

```bash
python3 tool/android_strings_to_arb.py   # writes lib/l10n/app_*.arb
flutter gen-l10n                         # also runs on every build
```
