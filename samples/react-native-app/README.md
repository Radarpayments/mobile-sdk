# React Native sample

The [sample apps'](../README.md) demo, built on the `@payrdr/react-native-sdk` module (`plugins/react-native`). It has
everything the native samples have: the stores Hearth & Co, Roast Lane and Stage Pass with the Scenario panel,
Northwind Invest for AFT transfers, SDK Core and the developer settings, in the same nine languages.

The code follows the Flutter sample file by file: `lib/…/x.dart` there is `src/…/x.ts(x)` here.

## Where the integration code is

| What | Where |
|---|---|
| Opening the payment sheet and handling the result | `src/checkout/checkout.ts` |
| The SDK Core card form | `src/core/` (`coreToken.ts` makes the token) |
| "Your server": order registration | `src/merchant/orderRegistrar.ts` |

The demo has no backend, so it registers orders itself, with the credentials from the developer settings:

- API V1: merchant login and password, `register.do` → an `mdOrder`. An AFT transfer uses `registerP2P.do`.
- API V2: the API key, `POST /api2/sessions` → a session ID.

A real app gets the order from its own backend and never contains merchant credentials.

## Native setup

The app has no native code for the SDK. `MainActivity` is the template's plain `ReactActivity`, and the SDK's Android
libraries add the camera permission and the Google Pay entry to the manifest themselves.

That is also why the screens use a small navigation stack of their own (`src/navigation.tsx`) and not React
Navigation's native stack: `react-native-screens` needs a change in `MainActivity`.

On iOS the sample has what any app with these features needs:

- `NSCameraUsageDescription` in `Info.plist`, for the card scanner;
- the Apple Pay capability in `ios/PayrdrSample/PayrdrSample.entitlements`. Its merchant ID
  `merchant.net.payrdr.samples.test` is a placeholder: to try Apple Pay, put a real one there and in the developer
  settings.

## Build and run

You need Node 22.11 or newer and CocoaPods 1.13 or newer (but not 1.15.0 or 1.15.1).

First run `scripts/build-bridge.sh` from the repository root. It builds the SDK into `build/maven` and copies
`PayrdrSdkBridge.xcframework` into the module. Then:

```bash
npm install
npm test
cd ios && pod install && cd ..
npm run android   # or: npm run ios
```

`npm test` runs Jest over the cart, checkout, order registration, Northwind, the settings storage, the translations
and the app's screens.

A release build bundles the JavaScript and runs without Metro:

```bash
cd android && ./gradlew assembleRelease
```

## Texts and fonts

The texts come from the Flutter sample's ARB files, which are made from the Android sample's string resources. After
changing them, run:

```bash
node tool/arb_to_ts.mjs   # writes src/l10n/messages.ts
```

React Native picks a font by its name and can't set the weight of a variable font. So `tool/make_fonts.py` cuts static
weights from the Flutter sample's variable fonts, plus the few Material Icons the screens use, into `assets/fonts`:

```bash
python3 -m pip install fonttools
python3 tool/make_fonts.py
```

Android takes the whole folder as assets. On iOS the Xcode project lists each file, and `Info.plist` names them in
`UIAppFonts`.
