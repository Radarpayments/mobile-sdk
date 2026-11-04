# Android sample — agent notes

Read `../AGENTS.md` (all samples) first. Package root `net.payrdr.mobile.payment.samples.android`, sources in
`src/main/kotlin/net/payrdr/mobile/payment/samples/android/`.

## Map

| Path | What it is |
|---|---|
| `MainActivity.kt` | wraps the context with `AppLanguage.wrap` in `attachBaseContext`; edge-to-edge; `SampleTheme { SampleNavHost() }` |
| `AppLanguage.kt` | `DemoLanguages`; the UI language in SharedPreferences `app_language` (needed synchronously, so not DataStore); Arabic is `ar-u-nu-latn` + RTL |
| `SampleTheme.kt` | the app's own screens are always light; the checkout follows the system or the chosen sheet theme |
| `checkout/Checkout.kt` | **the integration file**: `buildSdkConfig`, `CheckoutOutcome`, `CheckoutState`, `rememberPayrdrCheckout` |
| `checkout/ClassicCheckoutActivity.kt` | the same without Compose (`PayrdrCheckout.Builder`), launched from Advanced |
| `merchant/` | `OrderRegistrar.kt` ("your server", V1 / V1 AFT / V2), `MerchantHttp.kt` (Ktor + OkHttp) |
| `core/` | SDK Core screen: `SdkCoreScreen.kt`, `SdkCoreViewModel.kt` (a plain class held with `remember`), `CardFieldsGroup.kt`, `CardNetwork.kt` |
| `dev/DeveloperScreen.kt` | developer settings |
| `payment/advanced/`, `payment/composition/`, `payment/result/` | developer-only: headless `PaymentState` trace, SSL pinning demo, classic Activity, last result; texts not localised |
| `nav/` | `Routes.kt`, `SampleNavGraph.kt` — `SampleNavHost(registrarFactory)`, the factory is the test seam; reopens the last business |
| `prefs/` | `AppPrefs.kt` (DataStore `sample_prefs`, `Snapshot` flow, typed setters), `AppPrefsComposable.kt` (`rememberPrefs`, `rememberPersistedText`) |
| `store/` | `StartScreen`, `ScenarioSheet`, `StoreResultScreen`, `StoreViewModel`, `StoreCheckoutBar` (`LocalOrderRegistrarFactory`, `LocalTransferRegistrarFactory`, `coverSdkConfig`) |
| `store/{furniture,coffee,tickets}/` | Hearth & Co, Roast Lane, Stage Pass |
| `store/invest/` | Northwind Invest (AFT): `InvestMode.kt` (`isAftDemo` = V1 + AFT), `InvestAccount.kt`, top-up and portfolio screens |
| `store/model/`, `store/theme/CoverStyle.kt` | carts, catalog, scenario; brand tokens → `PaymentTheme.Custom` |
| `ui/components/` | `NavTopBar`, `PrimaryCta`, `InputShell`, `ResultHero`, `BrandGradients` |

## Build

- Plain Android app (`android.application` + `kotlin.android` + Compose compiler), not Compose Multiplatform: Compose
  from the BOM, material3 pinned apart. Project deps on `:sdk-core`, `:sdk-payment`, `:sdk-payment-scanner`.
- It declares `play-services-wallet` itself: the SDK has it `compileOnly`, and without it Google Pay falls back to a plain
  button. That is what integrators must do too — keep the comment.
- Release is shrunk by R8 with no rules of the sample's own (`proguard-rules.pro` empty): it checks the SDK's consumer
  rules the way an integrator's release build would. No signing config.
- `detekt-baseline.xml` holds a few old findings; new ones fail.

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :samples:android-app:installDebug
./gradlew :samples:android-app:testDebugUnitTest
./gradlew :samples:android-app:connectedDebugAndroidTest   # emulator or device
./gradlew :samples:android-app:ktlintCheck :samples:android-app:detekt
```

## State and texts

- `AppPrefs` (DataStore) holds everything: gateway URL, credentials (plain, empty by default), API version, order type,
  clientId, scenario, last cover and mdOrder, sheet theme, SDK locale, the invest account (JSON). `Snapshot.toString()`
  hides the secrets — keep it so when adding a field.
- Bind text fields through `rememberPersistedText`; binding straight to DataStore loses keystrokes.
- The UI language (`app_language`) and the SDK locale (`AppPrefs.localeOverride` → `SdkPaymentConfig.locale`) are two
  settings; changing the UI language recreates the activity.
- Strings: `res/values*/strings_{store,catalog,core,invest}.xml` in 9 languages. **These files are the source for the
  Flutter and RN samples' texts** — regenerate them after a change (see `../AGENTS.md`). A new `%1$s` placeholder may
  need a name in `PLACEHOLDERS` of `flutter-app/tool/android_strings_to_arb.py`.

## Tests

- `src/test`: JUnit 5, `kotlin.test`, coroutines-test, Ktor `MockEngine`. Checkout config/outcome mapping, the order
  registrar (bodies, errors, timeouts), view models, invest, cart, catalog, theme. Names are backtick sentences.
- `src/androidTest`: JUnit 4, `createComposeRule` / `ActivityScenario`. Navigation, German and Arabic RTL, developer
  settings, SDK Core, the three store flows, `StartScreen` with `aftDemo = true`.
  - Tags are `internal const val *_TAG` in main code (`PAY_BUTTON_TAG = "store_pay"`, `coreFieldTag(f)`, …).
  - DataStore screens render after loading: `waitUntil` for a tag first.
  - Stub the network with `LocalOrderRegistrarFactory provides { … }` or `SampleNavHost(registrarFactory)`.
  - Look texts up with `getString(R.string.…)` after `AppLanguage.save(ctx, null)`.
  - Tests write the device's real `sample_prefs`; reset what you set.
