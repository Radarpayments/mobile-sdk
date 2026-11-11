# @payrdr/react-native-sdk

Opens the PAYRDR payment sheet in a React Native app. These are the native SDK's own screens: card payment with 3-D
Secure, saved cards, Google Pay, Apple Pay and the card scanner. The module also gives you SDK Core, which encrypts
card data for merchants who are PCI DSS certified and keep their own card form.

## Requirements

- React Native 0.76 or newer with the New Architecture (the module is a TurboModule).
- Android minSdk 24; iOS from React Native's own minimum.
- Expo: works in a development build (`expo prebuild`), not in Expo Go.

## Install

The module is not on npm yet. Each [GitHub release](https://github.com/Radarpayments/mobile-sdk/releases) has it as
`payrdr-react-native-sdk-<version>.tgz`, with the iOS framework inside.

```sh
npm install ./payrdr-react-native-sdk-<version>.tgz
cd ios && pod install
```

On Android the module downloads the SDK from Maven Central. Alpha versions aren't there: an alpha release has
`payrdr-sdk-maven-<sdk-version>.zip` instead. Unpack it and set `payrdrMavenUrl` in `android/gradle.properties` to the
unpacked folder. (The module's version is 1.x; the SDK's is 4.x.)

You don't need to change anything else:

- `MainActivity` stays a plain `ReactActivity`, and `AppDelegate` needs no code.
- The Android manifest needs no entries: Google Pay and the camera permission come with the SDK.

Two iOS features need setup:

- Apple Pay: add the Apple Pay capability with your merchant ID.
- Card scanner: add `NSCameraUsageDescription` to `Info.plist`.

## Take a payment

Your server registers the order at the gateway and gives the app its `mdOrder`. Never put the merchant's login,
password or API key in the app.

```ts
import { PayrdrCheckout } from '@payrdr/react-native-sdk';

const checkout = new PayrdrCheckout({
  baseUrl: 'https://pay.example.com/payment',
  theme: { mode: 'custom', colors: { primary: '#1C1917' }, cornerRadius: 12 },
  googlePay: {}, // everything comes from the gateway
  applePay: { merchantId: 'merchant.com.example' },
  cardScanner: true,
});

const mdOrder = await myBackend.registerOrder(cart);

const result = await checkout.present(mdOrder);
switch (result.status) {
  case 'completed': showPaid(); break;
  case 'canceled': break;
  case 'failed': showError(result.error.message); break; // result.error.code: 'declined', 'network', …
  case 'unknown': checkOnServer(result.orderId); break;
}
```

For an API V2 session, call `checkout.presentSession(sessionId)` with the `ps_…` ID your server created.

`unknown` means the payment was sent but its outcome isn't known. Ask your server how the order ended, and don't offer
to pay again until it knows.

`present` resolves when the sheet closes. It also resolves when the sheet couldn't open, as `failed` with the code
`busy`, `no_screen` or `invalid_request`. It rejects only when a theme colour isn't a hex colour (`#RGB`, `#RRGGBB` or
`#RRGGBBAA`): that is a bug in the app's code.

The config also takes `locale`, `showResultScreen`, `keyProviderUrl`, `sslPins` and `debugLogging`. They mean the same
as in the native SDK; see the [root README](../../README.md#configuration).

## SDK Core

```ts
import { PayrdrCore } from '@payrdr/react-native-sdk';

const result = await PayrdrCore.generateToken({
  type: 'card', mdOrder, pan: '4111111111111111', expiry: '12/30', cvc: '123', pubKey: gatewayPublicKeyPem,
});
if (result.token) {
  sendToServer(result.token); // the server pays with it as seToken
} else {
  showFieldErrors(result.errors); // { pan: 'required', … }
}
```

Other types: `'cardWithoutOrder'`, `'savedCard'` (by `bindingId`) and `'savedCardWithoutOrder'`. `generateToken`
rejects if the request itself is malformed.

## How it works

The TypeScript code turns the config into JSON and calls the native module. On the native side `sdk-bridge` opens the
sheet and sends the result back as JSON. The JSON format is described in
[sdk-bridge/README.md](../../sdk-bridge/README.md).

## Working on the module in this repository

The module uses the SDK built from this repository. Run `scripts/build-bridge.sh` from the repository root once, and
again after changing the SDK. It publishes the Android libraries to `build/maven` and copies
`PayrdrSdkBridge.xcframework` into `ios/Frameworks` and the scanner's camera sources into `ios/Scanner`.

An app that gets the module some other way sets `payrdrMavenUrl` (and, if needed, `payrdrSdkVersion`) in its
`android/gradle.properties` to the Maven repository with the SDK. An app that installs the module with a `file:` path
must point Metro at it, as `samples/react-native-app/metro.config.js` does.

Tests: `npm test`. It covers the TypeScript API and the shared contract files in `sdk-bridge/contract`.
