# Sample apps

Demo stores that take payments with the PAYRDR SDK. The same demo is built four times:

| App | Built with | Folder |
|---|---|---|
| Android | Jetpack Compose, `sdk-payment` | `android-app/` |
| iOS | SwiftUI, `RadarpaymentsSDKPayment` and the scanner | `ios-app/` |
| Flutter | the `payrdr_sdk` plugin | `flutter-app/` ([README](flutter-app/README.md)) |
| React Native | the `@payrdr/react-native-sdk` module | `react-native-app/` ([README](react-native-app/README.md)) |

## Before you start

The apps don't ship with merchant credentials. Open the gear on the start screen and enter, under **Credentials**,
the merchant login and password (API V1) or the API key (API V2) of a merchant on the gateway you use. They stay on
the device.

## What's in the demo

The start screen has two sections: **SDK Payment** with three stores, and **SDK Core**.

### The stores

Each store looks and sells differently, but they pay the same way:

| Store | What you do | Sheet theme |
|---|---|---|
| **Hearth & Co** (furniture) | catalogue → product → cart → Pay | light, black button |
| **Roast Lane** (coffee) | menu → order → Pay. "Order again" fills the cart with the last paid order and pays at once | dark, cream button |
| **Stage Pass** (tickets) | event → seats → checkout, where the seats are held for a while → Pay | light, orange button |

1. **Pay** registers an order for the cart total and gets its `mdOrder` (see [Your server](#your-server)).
2. The SDK sheet opens in the store's colours, through `PaymentTheme.Custom`.
3. When the sheet closes, the store shows its own result screen. What happens next is up to the app; the SDK only
   reports how the payment ended.

Next to Pay, **Scenario** opens a demo panel:

- **Buyer**: *Guest*, or *With cards*. With cards, the order carries a demo `clientId`, so a card saved in an earlier
  payment ("Save card") shows up in the sheet. *Reset* starts over with a new buyer without cards.
- **Payment**: Google Pay (Android) or Apple Pay (iOS), the card scanner, and the SDK's own result screen
  (`showResultScreen`).

Which payer details the sheet asks for (email, phone, billing address) depends on the merchant's settings at the
gateway, not on the app.

### Northwind Invest: AFT transfers

An AFT moves money from the buyer's card into an account, so the demo shows it in an investment app instead of a
store. Choose API V1 and the order type "AFT transfer" in the developer settings, and the start screen shows
**Northwind Invest** instead of the three stores. Its sheet is dark.

- **Top up**: choose €50, €100, €250 or €500, or type any amount from €10 to €1,000 → the SDK sheet → "+€… credited".
  The account is credited only when the SDK reports the transfer as completed; after a decline or an unknown outcome
  it stays as it was. The account lives on the device.
- **Add card** (on the top-up screen): registers a transfer of 0 with the features `WITHOUT_TO_CARD`, `VERIFY` and
  `FORCE_CREATE_BINDING`. The gateway checks the card, reverses the transfer and saves the card; the account isn't
  credited. A banner says whether the card was added, and the next top-up offers it. (The gateway refuses `VERIFY`
  without `FORCE_CREATE_BINDING`.)
- **Invest** moves free cash into a fund, with no payment.

Transfers are registered with the investor's details (`billingPayerData`, `billingRecipientData`, `email`), so the
sheet asks only for the card and the phone. Northwind always sends its demo `clientId`, so its Scenario panel has no
Buyer choice, only Reset.

### SDK Core

For merchants who are PCI DSS certified and keep their own card form. `sdk-core` encrypts the card data on the device
into a token; the merchant's server sends it to the gateway as `seToken`.

The screen makes all three kinds of token:

| Token | Input |
|---|---|
| New card | card data + `mdOrder` |
| No order | card data only |
| Saved card | `bindingId` + `mdOrder` |

Validation errors from `sdk-core` appear next to their fields. Paste the `mdOrder` and the gateway's public key (PEM)
by hand; the last order's `mdOrder` is in the developer settings. The app remembers the token kind, `mdOrder`, key and
`bindingId`, but never the card data.

## Where the integration code is

Everything that calls the SDK is in one file per app:

| App | File |
|---|---|
| Android | `android-app/.../checkout/Checkout.kt` |
| iOS | `ios-app/SampleApp/Checkout/Checkout.swift` |
| Flutter | `flutter-app/lib/checkout/checkout.dart` |
| React Native | `react-native-app/src/checkout/checkout.ts` |

Each file follows the same four steps: build the config, get an order, open the sheet, handle the result. The stores,
the cart and the demo panel never call the SDK themselves.

### Your server

In a real app the order comes from the merchant's own backend, which registers it at the gateway:

- API V1: `register.do` with the merchant login and password returns an `mdOrder`; the app opens it with
  `present(mdOrder)`. An order can also be an AFT transfer (`registerP2P.do`, feature `WITHOUT_TO_CARD`); the same
  call opens a sheet that asks for the sender's and recipient's details and says "Transfer".
- API V2: `POST /api2/sessions` with the API key returns a session ID (`ps_…`); the app opens it with
  `presentSession(sessionId)`. The session sends the buyer back to `sdk://done` (its `successUrl` and `failureUrl`);
  the SDK also recognises the merchant's own pages there.

The demo has no backend, so it registers orders itself (`merchant/OrderRegistrar.kt` on Android,
`Merchant/OrderRegistrar.swift` on iOS). A real app must never contain merchant credentials.

## Developer settings

The gear on the start screen opens the same list in all four apps:

| Group | What it sets |
|---|---|
| **Gateway** | The base URL (default `https://dev.bpcbt.com/payment`; an address that isn't `https://` gets a warning) and the API: **API V1**, or **API V2** at `https://dev.bpcbt.com/api2` |
| **Credentials** | API V1: the order type (Payment or AFT transfer), merchant login and password. API V2: the API key. Stored on the device only; the eye button shows what you typed |
| **SDK appearance** | The sheet theme (store colours, light, dark or system) and the language: System, English, Русский, Română, Ελληνικά, Български, Deutsch, Français, Español or العربية (right to left). The language changes the demo's screens at once and goes to the SDK as `SdkPaymentConfig.locale` |
| **Google Pay** (Android) | Gateway and merchant ID |
| **Apple Pay** (iOS) | Merchant ID. The default `merchant.net.payrdr.samples.test` is a placeholder; Apple Pay needs a real one |
| **Advanced** (Android and iOS) | The payment state stream (a headless payment of a new €1 order) and an SSL pinning check that fails on purpose |
| **Last order** | The `mdOrder` of the last order, with a Copy button |

## Build and run

Run Gradle with JDK 17–21; with a newer JDK it fails. Android Studio's bundled JDK works.

### Android

From the repository root:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :samples:android-app:installDebug
```

Tests:

```bash
./gradlew :samples:android-app:testDebugUnitTest
./gradlew :samples:android-app:connectedDebugAndroidTest   # needs an emulator or a device
```

### iOS

Xcode with the iOS 16 SDK or newer. A Run Script phase builds the SDK framework with Gradle, and it expects JDK 21 at
`$HOME/.sdkman/candidates/java/21.0.2-tem`; with the JDK elsewhere, change `JAVA_HOME` in that phase.

```bash
cd samples/ios-app
xcodebuild -project SampleApp.xcodeproj -scheme SampleApp \
    -destination 'platform=iOS Simulator,name=iPhone 15 Pro,OS=17.4' build
```

Run the tests (unit and UI) on an iOS 17.4 simulator: Kotlin/Native crashes in newer xctest hosts.

```bash
xcodebuild -project SampleApp.xcodeproj -scheme SampleApp \
    -destination 'platform=iOS Simulator,name=iPhone 15 Pro,OS=17.4' test
```

### Flutter and React Native

See [flutter-app/README.md](flutter-app/README.md) and [react-native-app/README.md](react-native-app/README.md). Both
need `scripts/build-bridge.sh` run from the repository root first.

## Troubleshooting

| Problem | What to do |
|---|---|
| "Merchant login and password are not set." or "The API key is not set." | Open the gear → Credentials and enter them for the API chosen under Gateway |
| Pay spins for a long time, then fails | The device can't reach the gateway (on an emulator, check its DNS). Registration gives up after 20 seconds |
| No Google Pay button | Update Google Play services, and check that Google Pay is set up for the merchant |
| SSL pin mismatch | Expected only in Advanced → SSL pinning |

## Credits

- Photos from [Unsplash](https://unsplash.com) (Unsplash License):
  [1555066931](https://images.unsplash.com/photo-1555066931-4365d14bab8c),
  [1586023492125](https://images.unsplash.com/photo-1586023492125-27b2c045efd7),
  [1555041469](https://images.unsplash.com/photo-1555041469-a586c61ea9bc),
  [1567538096630](https://images.unsplash.com/photo-1567538096630-e0c55bd6374c),
  [1592078615290](https://images.unsplash.com/photo-1592078615290-033ee584e267),
  [1503602642458](https://images.unsplash.com/photo-1503602642458-232111445657),
  [1507473885765](https://images.unsplash.com/photo-1507473885765-e6ed057f782c),
  [1517701604599](https://images.unsplash.com/photo-1517701604599-bb29b565090c),
  [1509042239860](https://images.unsplash.com/photo-1509042239860-f550ce710b93),
  [1572119865084](https://images.unsplash.com/photo-1572119865084-43c285814d63),
  [1555507036](https://images.unsplash.com/photo-1555507036-ab1f4038808a),
  [1559925393](https://images.unsplash.com/photo-1559925393-8be0ec4767c8),
  [1470229722913](https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3),
  [1501386761578](https://images.unsplash.com/photo-1501386761578-eac5c94b800a).
- Fonts from [Google Fonts](https://fonts.google.com) (SIL Open Font License 1.1): Cormorant, Montserrat, Nunito,
  Inter, Playfair Display, IBM Plex Sans.
- Material Icons (Apache License 2.0), in the React Native app.
