# samples — agent notes

Read the root `AGENTS.md` and `README.md` here (what the demo shows, developer settings, build and run) first. Each app
has its own `AGENTS.md`.

| App | Stack | SDK through | State |
|---|---|---|---|
| `android-app/` | Jetpack Compose, Gradle module `:samples:android-app` | project deps on the SDK modules | DataStore |
| `ios-app/` | SwiftUI, `SampleApp.xcodeproj` | `RadarpaymentsSDKPayment.xcframework` (debug), built by an Xcode script phase, plus the scanner's Swift sources | UserDefaults + Keychain |
| `flutter-app/` | Flutter | `plugins/flutter` (path dependency) → `sdk-bridge` | `ChangeNotifier` + shared_preferences |
| `react-native-app/` | React Native 0.88 | `plugins/react-native` (`file:` dependency) → `sdk-bridge` | `Listenable` + AsyncStorage |

## What the samples are for

They are the integrator's reference: what an app writes to take a payment. So the code that talks to the SDK must stay
small, readable and copyable, and must only use public API.

- **One integration file per app** opens the payment sheet, starting with the four steps (config → mdOrder from "your
  server" → open the sheet → handle the result): `android-app/.../checkout/Checkout.kt`, `ios-app/SampleApp/Checkout/Checkout.swift`,
  `flutter-app/lib/checkout/checkout.dart`, `react-native-app/src/checkout/checkout.ts`. Stores, carts and the demo
  panel never call the SDK.
- Other SDK users, by design: the SDK Core screen (`core/`), the developer-only Advanced screens on Android and iOS
  (headless flow, SSL pinning, classic Activity), and files that only take SDK types (theme, config, result).
- **"Your server"** is `OrderRegistrar`: the demo registers orders itself because it has no backend (API V1
  `register.do` / `registerP2P.do` for AFT; API V2 `POST /api2/sessions`; 20 s timeout). Keep the comments that say a
  real app does this on its backend.
- The samples carry their own card-network table, card formatting, gateway HTTP and session-id decoding. Do not import
  those from the SDK: they are internal there.

## Parity

The four apps show the same demo: three stores (Hearth & Co, Roast Lane, Stage Pass) with the Scenario panel and result
screens, the SDK Core screen (new card / no order / saved card), Northwind Invest (AFT, shown when API V1 + order type
AFT), and the developer settings (gateway, API V1/V2 credentials, order type, sheet theme, 9 languages, Google Pay or
Apple Pay, last order with Copy). A user-visible change to one app is made in all four, or the commit says
why not. Exceptions: Advanced (headless flow, SSL pinning, classic Activity) exists only in the native apps, because the
plugins expose only the checkout and SDK Core.

The developer settings are an inset grouped list in all four apps (grey page, white cards): fields with their name
above the value, a segmented control for two choices, a row with a list (menu on iOS, bottom sheet elsewhere) for four
or more, done states shown in place for 2 s. Android's `dev/SettingsRows.kt` is the reference for sizes and colours.
Headings in capitals go through `toCaps` (Android `ui/Caps.kt`, Flutter `lib/caps.dart`, RN `src/ui/caps.ts`): Greek
capitals drop the stress mark, which the platforms' upper case keeps. Android's Tabler icons are not auto-mirrored;
directional ones (back, chevrons) take `Modifier.mirrorInRtl()`. On iOS a text that means different things in different
places gets its own key with an English value in the catalog (`result_payment_id`, `theme_light`): one key has one
translation, and "Light" there is the furniture category.

Shared across apps, keep them identical:
- persistence key names (`store_login`, `store_api_version`, `store_order_type`, `store_client_id`, `store_last_cover`,
  `invest_account`, …);
- test tags / accessibility identifiers / Flutter keys / RN `testID`s, snake_case with a screen prefix (`store_pay`,
  `store_scenario`, `scenario_with_cards`, `dev_login`, `dev_api_key`, `core_field_<FIELD>`, `core_generate`, …);
- the demo clientId shape `demo-` + 13 characters (a longer id made the gateway decline card saving with 71015);
- amounts in euro cents, currency EUR; top-up €10–€1,000.

## Texts

One source for three apps: the Android string resources
`android-app/src/main/res/values*/strings_{store,catalog,core,invest}.xml`. Flutter's `.arb` files are generated from
them (`flutter-app/tool/android_strings_to_arb.py`, then `flutter gen-l10n`), and RN's `src/l10n/messages.ts` from the
`.arb` files (`react-native-app/tool/arb_to_ts.mjs`). iOS has its own `Localizable.xcstrings`, edited by hand. A new text
goes into all 9 languages (en, ru, ro, el, bg, de, fr, es, ar); brand and product names are `translatable="false"`.

## Credentials and hosts

Nothing ships with the apps: the merchant login and password, the API key and Google Pay/Apple Pay settings are typed in
on the device. The only default host is the public sandbox `https://dev.bpcbt.com/payment`. The Apple Pay merchant ID
`merchant.net.payrdr.samples.test` is a placeholder. Never commit real credentials or a customer stand's host, not even
in a test.

## Before running a sample

- After any SDK change: Android and iOS samples build the SDK themselves; Flutter and RN need
  `scripts/build-bridge.sh` from the repo root first.
- iOS tests (native, and probably the plugin samples too): an iOS 17.4 simulator — Kotlin/Native crashes in newer
  xctest hosts.
