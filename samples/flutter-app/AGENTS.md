# Flutter sample — agent notes

Read `../AGENTS.md` (all samples) and `README.md` here first. The plugin it uses is `plugins/flutter`
(path dependency); see `plugins/AGENTS.md`.

## Map (`lib/`)

| Path | What it is |
|---|---|
| `main.dart`, `app.dart` | loads `AppPrefs`, `MaterialApp` with the locale from prefs; `demoLanguages` (settings order); `CoverRoutes` (cover → store → `StoreResultScreen`) |
| `app_scope.dart` | `AppScope` `InheritedWidget`: prefs, store, `registrarFactory`, `transferRegistrarFactory`, `presentSheet` (test seam; null opens the real SDK). `context.l10n` / `.prefs` / `.store` |
| `checkout/checkout.dart` | **the only place the payment sheet is called**: `buildSdkConfig`, `paymentTheme`, `toOutcome`, `StoreCheckout` (ignores taps while busy); sealed `CheckoutOutcome` / `CheckoutState` |
| `core/` | SDK Core screen; the only other SDK call (`PayrdrCore.generateToken`) |
| `merchant/order_registrar.dart` | "your server": API V1 `register.do` (form) or `registerP2P.do` (JSON, AFT); API V2 `POST /api2/sessions`; `sessionIdToMdOrder`; 20 s timeout; sealed `OrderRegistration` (`MissingCredentials` / `MissingApiKey` mean nothing was sent) |
| `prefs/app_prefs.dart` | `ChangeNotifier` over shared_preferences; key names match iOS `StorePrefs`; clientId kept short on purpose (gateway error 71015) |
| `store/` | start screen, Scenario sheet, `store_checkout_bar.dart` (Pay bar of all three stores), result screen, `store_model.dart`; `furniture/`, `coffee/`, `tickets/`, `model/`, `theme/` |
| `store/invest/` | Northwind Invest (AFT): `invest_store.dart` (portfolio → top up → done), `invest_model.dart` (credits only on success), `top_up_amount.dart` (€10–€1000, Arabic/Persian digits), `invest_mode.dart` (`isAftDemo` = API V1 + AFT order) |
| `dev/developer_screen.dart` | developer settings |
| `l10n/` | `app_*.arb` + generated `app_localizations*.dart`, both committed |

State is plain `ChangeNotifier` + `ListenableBuilder` through `AppScope` — no Provider/Riverpod/BLoC; keep it that way.
Amounts are euro cents (`amountMinor`), currency EUR (978).

## Before running

`scripts/build-bridge.sh` from the repo root after any SDK or bridge change (Android libs into `build/maven`, the
XCFramework into the plugin). Without it the app builds against a stale or missing SDK. iOS uses Swift Package Manager
(no Podfile); `flutter run` on Android: leave `JAVA_HOME` unset.

Toolchain: the lockfile needs Flutter ≥ 3.44 / Dart ≥ 3.13 (the code uses null-aware map entries). Android build: AGP 9.1,
Kotlin 2.4, Gradle 9.3 — independent of the SDK's own Gradle build.

## Texts

The Android sample's string resources are the source. Do not edit `.arb` files by hand:

```bash
python3 tool/android_strings_to_arb.py   # android-app/src/main/res/values*/strings_{store,catalog,core,invest}.xml → lib/l10n/app_*.arb
flutter gen-l10n
```

Placeholder names are set in the script's `PLACEHOLDERS`; texts only Flutter needs (Apple Pay) are in `FLUTTER_ONLY`.
The RN sample generates its texts from these `.arb` files, so regenerate it too (`samples/react-native-app/tool/arb_to_ts.mjs`).

## Tests and lint

```bash
flutter test       # cart, checkout, invest, order_registrar, prefs, widget — no native code needed
flutter analyze    # flutter_lints 6; formatter page_width 120
```

Tests fake the registrar and `presentSheet` through `AppScope`; HTTP goes through `package:http/testing` `MockClient`;
prefs through `SharedPreferences.setMockInitialValues`. The widget test runs at a 1170×2532 view. Test names are
lowercase behaviour sentences, groups named after the type (`group('StoreCheckout')`). Keys are snake_case
(`store_pay`, `dev_invest_reset`) and shared with the RN sample's `testID`s.

## Conventions

snake_case files, `_Private` widgets, doc comments on types and functions in plain English saying why. The RN sample
mirrors this one file by file: a change here usually needs the same change in `samples/react-native-app`.
