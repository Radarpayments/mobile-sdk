# payrdr SDK Sample Apps

Demo stores that show how a merchant app takes payments with `sdk-payment`, and a card form of the
merchant's own encrypted with `sdk-core`:

- **Android** — Jetpack Compose. See `android-app/`.
- **iOS** — SwiftUI. See `ios-app/`.
- **Flutter** — the same demo on the `payrdr_sdk` plugin. See `flutter-app/`.
- **React Native** — the same demo on the `@payrdr/react-native-sdk` module. See `react-native-app/`.

## What the demo shows

The start screen has two sections: **SDK Payment** with three businesses and **SDK Core**.

### SDK Payment

The three businesses look and sell differently, but each runs the same
payment path and every payment scenario:

| Business | What you do | Sheet theme |
|---|---|---|
| **Hearth & Co** — furniture | catalogue → product → cart → Pay | light, black button |
| **Roast Lane** — coffee shop | menu → order → Pay; "Order again" pays the last order with a saved card | dark, cream button |
| **Stage Pass** — tickets | event → seats → checkout with a seat hold → Pay | light, orange button |

Every cart has the same bottom: a **Scenario** row and the store's **Pay** button.

1. **Pay** registers an order for the cart total ("your server", see below) and gets an mdOrder.
2. The SDK sheet opens in the business's brand: `PaymentTheme.Custom` built from its colours.
3. When the sheet closes, the store shows its own result screen. The merchant app decides what
   comes next; the SDK only reports success, decline or cancel.

The **Scenario** row opens the demo panel:

- **Buyer** — *Guest*, or *With cards*: the order carries a demo `clientId`, so cards saved with
  "Save card" in an earlier payment show up. *Reset* starts a new buyer without cards.
- **Payment** — Google Pay (Android) / Apple Pay (iOS), the card scanner, and the SDK's own result
  screen (`showResultScreen`).

Which payer details the SDK asks for (email, phone, billing address) is decided by the merchant's
settings on the gateway, not by the app.

### SDK Core

For merchants with PCI DSS who keep their own card form: `sdk-core` encrypts the card data on the device
into a token, the merchant's server passes it to the gateway as `seToken`. The screen covers the three
kinds of token — *New card* (card data + mdOrder), *No order* (card data only) and *Saved card*
(bindingId + mdOrder) — and shows sdk-core's error codes next to the fields they belong to.

The card fields are grouped the way the SDK's own form groups them: number with the network logo, expiry
and CVC, name.

mdOrder and the gateway's public key (PEM) are pasted by hand — e.g. the last order's mdOrder from the
developer settings. They and the bindingId are remembered; card data are not stored. Code:
`android-app/.../core/SdkCoreScreen.kt`, `ios-app/SampleApp/Core/SdkCoreView.swift`.

## Where the integration is

All code that talks to the SDK is in one file per platform:

- Android — `android-app/.../checkout/Checkout.kt`
- iOS — `ios-app/SampleApp/Checkout/Checkout.swift`

Each starts with the four steps: create the SDK from a config → get an mdOrder → open the sheet →
handle the result. The stores, the cart and the demo panel never call the SDK directly.

### "Your server"

A real app gets the order from **its own backend**, which registers it at the gateway: through API V1
(`register.do` with the merchant login and password → an mdOrder, opened with `present(mdOrder)`) or through
API V2 (`POST /api2/sessions` with the API key → a session id `ps_…`, opened with `presentSession(sessionId)`).
The demo has no backend, so it registers orders itself (`merchant/OrderRegistrar.kt`,
`Merchant/OrderRegistrar.swift`); developer settings choose the API. An API V2 session sends the buyer back to
`sdk://done` (its `successUrl` and `failureUrl`); the SDK also recognizes the merchant's own pages. With API V1 the
order can also be an AFT transfer (`registerP2P.do`, feature `WITHOUT_TO_CARD`): the same `present(mdOrder)` opens a
sheet that asks for the sender's and recipient's details and says "Transfer". Never ship merchant credentials inside
an app.

### Northwind Invest: AFT

An AFT moves money from the buyer's card into an account, so it is shown in an investment app rather than in a
store. With API V1 and Order "AFT transfer" in the developer settings, the start screen swaps the three stores for
**Northwind Invest**: the portfolio → "Top up" → an amount typed on the system keyboard (€10–€1,000) → the SDK sheet →
"+€… credited", and the account's free cash grows. The transfer is registered with the investor's details
(`billingPayerData`, `billingRecipientData`, `email`), so the sheet asks only for the card (and the phone). The
account lives on the device and is credited only when the SDK reports the transfer completed; a declined or unknown
transfer leaves it as it was. "Invest" moves free cash into a fund without a payment.

## Developer settings

The gear on the start screen opens a grouped list, laid out the same way in the four apps:

- **Gateway** — base URL (default `https://dev.bpcbt.com/payment`; anything but an `https://` address is flagged under
  the field) and the merchant API: **API V1** or **API V2** (served next to the payment page:
  `https://dev.bpcbt.com/api2`).
- **Credentials** — with API V1 the order type (Payment or AFT transfer), the merchant login and password; with API V2
  the API key. Nothing ships with the app: type them in on the device; they are stored only there. The eye button
  shows a password or key while you check it.
- **SDK appearance** — the sheet theme (as the store / light / dark / system) and the language: System, English,
  Русский, Română, Ελληνικά, Български, Deutsch, Français, Español or العربية (right to left). It switches the
  demo's own screens at once and is passed to the SDK as `SdkPaymentConfig.locale`; System follows the device.
- **Google Pay** (Android: gateway, merchant ID) / **Apple Pay** (iOS: merchant ID — the default
  `merchant.net.payrdr.samples.test` is a placeholder; Apple Pay needs a real merchant ID).
- **Advanced** (Android and iOS only) — the payment state stream (headless flow, registers a fresh €1 order) and an
  intentionally failing SSL pinning check.
- **Last order** — the mdOrder of the last registered order, with Copy.

## Build and run

### Android

JDK 17 or 21 (Kotlin's Gradle plugin rejects JDK 26).

```bash
# From the repo root
JAVA_HOME=~/.sdkman/candidates/java/21.0.2-tem ./gradlew :samples:android-app:installDebug
```

Tests:

```bash
./gradlew :samples:android-app:testDebugUnitTest
./gradlew :samples:android-app:connectedDebugAndroidTest   # needs an emulator or device
```

### iOS

Xcode with an iOS 16+ SDK; JDK 17 or 21 for the Gradle Run Script phase that builds the
XCFramework.

```bash
cd samples/ios-app
xcodebuild -project SampleApp.xcodeproj -scheme SampleApp \
    -destination 'platform=iOS Simulator,name=iPhone 15 Pro,OS=17.4' build
```

Tests (unit + UI) — use an iOS 17.4 simulator: Kotlin/Native crashes in newer xctest hosts.

```bash
xcodebuild -project SampleApp.xcodeproj -scheme SampleApp \
    -destination 'platform=iOS Simulator,name=iPhone 15 Pro,OS=17.4' test
```

## Credits

- Photos: [Unsplash](https://unsplash.com) (Unsplash License) —
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
- Fonts: [Google Fonts](https://fonts.google.com) (SIL Open Font License 1.1) — Cormorant,
  Montserrat, Nunito, Inter, Playfair Display.

## Troubleshooting

- **"Merchant login and password are not set"** / **"The API key is not set"** — open the gear → Gateway and
  type them in for the API chosen there.
- **Pay spins for long and fails** — the device cannot reach the gateway (on an emulator, check
  its DNS); registration gives up after 20 s.
- **Google Pay button does not appear** — Play Services must be current and the merchant must
  have Google Pay set up.
- **SSL pin mismatch** — expected only in Advanced → SSL pinning.
