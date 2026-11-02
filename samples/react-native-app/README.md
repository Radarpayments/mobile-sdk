# payrdr SDK React Native sample

The demo stores of the other samples (see `../README.md`) built on the `@payrdr/react-native-sdk` module
(`plugins/react-native`): Hearth & Co, Roast Lane and Stage Pass with the Scenario panel and their own result
screens, SDK Core, and the developer settings, in the same nine languages. It mirrors the Flutter sample file by file
(`lib/…/x.dart` → `src/…/x.ts(x)`).

## Where the integration is

All code that talks to the payment sheet is in `src/checkout/checkout.ts`, which starts with the four steps: config →
mdOrder from "your server" → open the sheet → handle the result. SDK Core's card form is in `src/core/`
(`coreToken.ts` makes the token).

"Your server" is `src/merchant/orderRegistrar.ts`: the demo has no backend, so it registers orders itself with the
merchant login and password typed in the developer settings. A real app gets the mdOrder from its own backend and never
ships merchant credentials.

## No native setup

The app needs no native code and no manifest entries for the SDK: `MainActivity` is the template's plain
`ReactActivity`, and the SDK's Android libraries bring the camera permission and the Google Pay entry with them. That
is also why the screens use a small stack of their own (`src/navigation.tsx`) instead of React Navigation's native
stack: react-native-screens asks for a change in `MainActivity`. On iOS, add what any app with these features needs:

- `NSCameraUsageDescription` in `Info.plist` for the card scanner;
- the Apple Pay capability with your merchant ID (`ios/PayrdrSample/PayrdrSample.entitlements`; the sample's
  `merchant.net.payrdr.samples.test` is a placeholder — change it in the developer settings too).

## Build and run

The module's native inputs come from the repo: `scripts/build-bridge.sh` at the repo root publishes the Android
libraries to `build/maven` and copies `PayrdrSdkBridge.xcframework` into the module.

```bash
npm install
npm test                              # Jest: models, registrar, checkout, settings, screens
cd ios && pod install && cd ..
npm run android                       # or: npm run ios
```

A release build bundles the JavaScript and needs no Metro:

```bash
cd android && ./gradlew assembleRelease
```

## Texts, fonts

The texts are the Flutter sample's ARB files, which come from the Android sample's string resources. After changing
them:

```bash
node tool/arb_to_ts.mjs                # writes src/l10n/messages.ts
```

The brand fonts are static weights cut from the Flutter sample's variable Google Fonts, plus the few Material Icons the
screens draw; React Native picks a font by its name and cannot drive a variable font's weight. They live in
`assets/fonts` (Android takes the folder as assets, the Xcode project lists each file and `Info.plist` names them in
`UIAppFonts`):

```bash
python3 -m pip install fonttools
python3 tool/make_fonts.py
```
