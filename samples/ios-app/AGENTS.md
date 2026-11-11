# iOS sample — agent notes

Read `../AGENTS.md` (all samples) first. SwiftUI app, deployment target iOS 16.0, `SampleApp.xcodeproj`.

## Map (`SampleApp/`)

| Path | What it is |
|---|---|
| `SampleAppApp.swift` | `@main`; `SampleRoute`, `PathHolder` (the `NavigationStack` path), reopening the last cover, `pathFitting` for AFT mode |
| `AppLanguage.swift` | the 9 languages, `View.appLanguage(_:)`; Arabic is RTL with Latin digits |
| `Checkout/Checkout.swift` | **the integration file**: `buildSdkConfig`, `outcome(of:)`, `StoreCheckout`, `presentCheckout` |
| `Merchant/OrderRegistrar.swift` | "your server" (V1 `register.do` / `registerP2P.do`, V2 `/api2/sessions`), injectable `URLSession` |
| `Core/` | SDK Core screen: `SdkCoreView`, `SdkCoreFormModel`, `CardFieldsGroup`, `MaskedTextField` (UIKit), `CardNetwork`, `NetworkLogos/` |
| `Payment/Advanced/`, `Payment/Result/` | developer-only: `HeadlessFlowView`, `AdvancedView`, last result |
| `Store/` | `SampleStore` (SDK config from settings), `StoreModel`, `StorePrefs`, `Keychain`, `AppPrefsKeys`; `Model/`, `Theme/CoverStyle.swift` (→ `PaymentThemeCustom`), `Views/` (start, cover, Scenario, Pay bar, result, the three stores), `Invest/` (Northwind, AFT) |
| `Developer/DeveloperView.swift` | developer settings |
| `Theme/`, `Components/` | sample chrome (always light), shared views |
| `Fonts/`, `Assets.xcassets`, `Localizable.xcstrings`, `SampleApp.entitlements` | |

## The SDK from Swift

- One module: `import sdk_payment_scanner` (it re-exports `sdk-payment`). Never link `sdk_payment.xcframework` as well.
- sdk-core types keep the `Sdk_core` prefix (`Sdk_coreTokenResult`); Kotlin top-level functions come through `…Kt`
  classes (`IosSdkCoreSurfaceKt`); sealed results are matched with SKIE's `onEnum(of:)`.
- Swift conveniences (async `present`, `.payrdrCheckout` modifier, config inits) live in
  `sdk-payment/src/iosMain/swift/`.

## How the framework gets built

The first build phase of the app target is a shell script that runs Gradle:
- one architecture with an existing XCFramework slice → `:sdk-payment-scanner:linkDebugFramework<Target>`, then rsync
  into the slice; otherwise the whole `assembleSdk_payment_scannerDebugXCFramework` (~4 min);
- re-syncs and re-signs the embedded framework, because Xcode otherwise embeds a stale copy.

Gotchas:
- The script **hardcodes** `JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.2-tem"`. If the build fails on Java, that is
  why; any JDK 17–21 works.
- Debug only: there is no release framework path.
- `ENABLE_USER_SCRIPT_SANDBOXING = NO` is required for the script.
- Folders are synchronized groups (`objectVersion 77`): a new `.swift` file under `SampleApp/`, `SampleAppTests/` or
  `SampleAppUITests/` is picked up automatically. **Do not edit `project.pbxproj` to add files.**

## State, texts, platform setup

- `StorePrefs` (UserDefaults): `store_api_version`, `store_order_type`, `store_client_id`, `store_scenario`,
  `store_last_cover`, `store_last_md_order`, `store_sheet_theme`, `store_locale`, `invest_account` — the same names as
  the other samples. `AppPrefsKeys` (`@AppStorage`): base URL, SDK Core fields, Apple Pay merchant ID.
- Credentials are in the Keychain (`Keychain.swift`, `WhenUnlockedThisDeviceOnly`): `store_login`, `store_password`,
  `store_api_key`. Launch arguments (`-store_login x`) override them — that is how UI tests start clean.
- Texts: `Localizable.xcstrings` (string catalog, en source, 9 languages), edited by hand — it is not generated from
  Android. Strings built in code use `String(localized:bundle: AppLanguage.bundle)`; only `Text` follows the
  environment locale.
- `SampleApp-Info.plist`: `NSCameraUsageDescription` (scanner), `UIAppFonts` (add a new font here).
- Apple Pay: `SampleApp.entitlements` and the default in `DeveloperView` use the placeholder
  `merchant.net.payrdr.samples.test`; real Apple Pay needs a paid team with a real merchant ID.
- `ObservableObject` / `@Published` / `@StateObject`, not `@Observable` (iOS 16 target). Checkout types are
  `@MainActor`.

## Tests

```bash
cd samples/ios-app
xcodebuild -project SampleApp.xcodeproj -scheme SampleApp \
    -destination 'platform=iOS Simulator,name=iPhone 15 Pro,OS=17.4' test
# one class: add -only-testing:SampleAppTests/CheckoutTests
```

- Use an **iOS 17.4 simulator**: Kotlin/Native crashes in newer xctest hosts.
- `SampleAppTests` (hosted, `@testable import SampleApp`): order registrar with a `URLProtocol` stub, checkout,
  invest, prefs (isolated `UserDefaults` suite and Keychain service), cart, theme, SDK Core form model.
- `SampleAppUITests`: launched with `-store_login "" -store_password "" -store_last_cover "" -AppleLanguages (en)`;
  some assertions match English text. Accessibility identifiers are snake_case and shared with the other samples
  (`cover_<id>`, `store_pay`, `core_field_<FIELD>`, `dev_login`, …).
- The SDK's SwiftUI checkout is checked here, not in the SDK: `SampleAppUITests/CheckoutVisualTests` (screenshots per
  screen, `testNN…`) and `CheckoutAccessibilityTests` (VoiceOver labels and traits; `VISUAL_DUMP=1` writes each
  screen's element tree). Both run against `tools/visual/fake_gateway.py` (scenario = the merchant login, e.g.
  `cards3.wallet.cvc`), never a real gateway, so they are not part of the plain `xcodebuild test` above:
  ```bash
  tools/visual/locked.sh tools/visual/capture.sh <set> [testName]          # → tools/visual/out/<set>/
  VISUAL_CLASS=CheckoutAccessibilityTests tools/visual/locked.sh tools/visual/capture.sh <set>
  python3 tools/visual/compare.py tools/visual/golden/compose <set>           # diffs in tools/visual/out/diff/
  ```
  `capture.sh` starts the gateway, pins the status bar and picks the simulator (`SIMULATOR_UDID`); `locked.sh` keeps
  two agents from running Gradle, xcodebuild or the simulator at once. `golden/compose` are the screens of the old
  Compose checkout, the reference the SwiftUI port is measured against; `tools/visual/out/` is not committed.
- No SwiftLint/SwiftFormat: follow the file around you (4 spaces, ~120 columns, `///` doc comment per type).
