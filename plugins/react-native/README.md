# @payrdr/react-native-sdk

The payrdr payment sheet in a React Native app: card payment with 3-D Secure, saved cards, Google Pay, Apple Pay and the
card scanner — the native SDK's own screens, opened over your app. Plus SDK Core: card encryption for a merchant with
PCI DSS who keeps its own card form.

A TurboModule (React Native 0.76+, New Architecture). Expo: works in a development build (`expo prebuild`), not in Expo Go.

## Install

The module is not on npm yet: each [GitHub release](https://github.com/Radarpayments/mobile-sdk/releases) has it as
`payrdr-react-native-sdk-<version>.tgz`, with the iOS framework inside.

```sh
npm install ./payrdr-react-native-sdk-<version>.tgz   # in this repository: "file:../../plugins/react-native", see below
cd ios && pod install
```

On Android the module takes the SDK from Maven Central. An alpha is not there: its release has
`payrdr-sdk-maven-<version>.zip`; unpack it and set `payrdrMavenUrl` in `android/gradle.properties` to that folder.

Nothing else is required: `MainActivity` stays a `ReactActivity`, no code in `AppDelegate`, and the Android manifest
needs no entries (Google Pay and the camera permission come with the SDK). Android minSdk 24.

On iOS, per feature: Apple Pay needs the Apple Pay capability with your merchant id, the card scanner needs
`NSCameraUsageDescription` in `Info.plist`.

## Pay

```ts
import { PayrdrCheckout } from '@payrdr/react-native-sdk';

// 1. Describe the sheet once.
const checkout = new PayrdrCheckout({
  baseUrl: 'https://pay.example.com/payment',
  theme: { mode: 'custom', colors: { primary: '#1C1917' }, cornerRadius: 12 },
  googlePay: {},                                   // everything from the gateway
  applePay: { merchantId: 'merchant.com.example' },
  cardScanner: true,
});

// 2. Your server registers the order at the gateway and gives the app its mdOrder.
const mdOrder = await myBackend.registerOrder(cart);

// 3. Open the sheet. 4. Handle the result.
const result = await checkout.present(mdOrder);
switch (result.status) {
  case 'completed': showPaid(); break;
  case 'canceled': break;
  case 'failed': showError(result.error.message); break;   // result.error.code: 'declined', 'network', ...
  case 'unknown': checkOnServer(result.orderId); break;     // sent, outcome not known yet: never offer to pay again
}
```

`present` resolves once the sheet has closed, and also when it could not open (`failed` with `busy`, `no_screen` or
`invalid_request`); it rejects only for a theme colour that is not hex — a mistake in the app's code.

Never put the merchant's API login and password in the app: registering orders is your server's job.

## SDK Core

```ts
import { PayrdrCore } from '@payrdr/react-native-sdk';

const result = await PayrdrCore.generateToken({
  type: 'card', mdOrder, pan: '4111111111111111', expiry: '12/30', cvc: '123', pubKey: gatewayPublicKeyPem,
});
if (result.token) sendToServer(result.token);   // the server pays with it as seToken
else showFieldErrors(result.errors);            // { pan: 'required', ... }
```

Also `type: 'cardWithoutOrder'`, `'savedCard'` (bindingId) and `'savedCardWithoutOrder'`.

## How it works

The TypeScript API turns the config into JSON and calls the native module; the native side (`sdk-bridge`, the SDK's
Kotlin module for cross-platform apps) opens the sheet and answers with the result as JSON. The contract is in
[`sdk-bridge/README.md`](../../sdk-bridge/README.md).

## From this repository

The module takes the SDK from this repository's build: run `scripts/build-bridge.sh` from the repository root once (and
after SDK changes). It publishes the Android libraries to `build/maven` and copies `PayrdrSdkBridge.xcframework` into
`ios/Frameworks`. An app that gets the module from elsewhere sets `payrdrMavenUrl` (and optionally `payrdrSdkVersion`) in
its `android/gradle.properties` to the repository that serves the SDK. With `file:` installs, point Metro at the module
(see `samples/react-native-app/metro.config.js`).

Tests: `npm test` (the TypeScript side); the JSON contract is tested in `sdk-bridge`.
