# payrdr SDK Flutter sample

The demo stores of the native samples (see `../README.md`) built on the `payrdr_sdk` plugin
(`plugins/flutter`): Hearth & Co, Roast Lane and Stage Pass with the Scenario panel and their own result
screens, SDK Core, and the developer settings, in the same nine languages.

## Where the integration is

All code that talks to the payment sheet is in `lib/checkout/checkout.dart`, which starts with the four
steps: config → mdOrder from "your server" → open the sheet → handle the result. SDK Core's card form is
in `lib/core/`.

"Your server" is `lib/merchant/order_registrar.dart`: the demo has no backend, so it registers orders
itself with what the developer settings hold: the merchant login and password (API V1, `register.do`,
an mdOrder; `registerP2P.do` when the order is an AFT transfer) or the API key (API V2, `POST /api2/sessions`,
a session id). A real app gets the order from its own backend and never ships merchant credentials.

## No native setup

The app needs no native code and no manifest entries for the SDK: `MainActivity` is a plain
`FlutterActivity`, and the SDK's Android libraries bring the INTERNET permission and the Google Pay
(`com.google.android.gms.wallet.api.enabled`) entry with them. On iOS, add what any app with these
features needs:

- `NSCameraUsageDescription` in `Info.plist` for the card scanner;
- the Apple Pay capability with your merchant ID (`ios/Runner/Runner.entitlements`; the sample's
  `merchant.net.payrdr.samples.test` is a placeholder — change it in the developer settings too).

iOS uses Swift Package Manager; there is no Podfile.

## Build and run

The plugin's native inputs come from the repo: `build/maven` and
`plugins/flutter/ios/payrdr_sdk/Frameworks/PayrdrSdkBridge.xcframework`; `scripts/build-bridge.sh` at the
repo root makes them.

```bash
flutter test
flutter run            # Android: leave JAVA_HOME unset, Flutter uses Android Studio's JDK
```

## Texts

The texts come from the Android sample's string resources. After changing them there:

```bash
python3 tool/android_strings_to_arb.py   # writes lib/l10n/app_*.arb
flutter gen-l10n                         # also runs with every build
```
