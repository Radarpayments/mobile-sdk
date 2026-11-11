# plugins — agent notes

Read the root `AGENTS.md` and `sdk-bridge/AGENTS.md` first: the plugins are thin wrappers over `sdk-bridge`, and most
changes start there.

- `flutter/` — Dart plugin `payrdr_sdk`.
- `react-native/` — `@payrdr/react-native-sdk`, a TurboModule (New Architecture only, RN ≥ 0.76; Expo needs a dev build).

Both expose the same API in their language: `PayrdrCheckout(config).present(mdOrder)` / `.presentSession(sessionId)` →
a sealed/discriminated `CheckoutResult`, and `PayrdrCore.generateToken(...)`. Both share one version (`pubspec.yaml`,
`package.json`; set with `scripts/set-version.sh`, released with the SDK: see the root `AGENTS.md`, Version),
unpublished (`publish_to: none`, `private: true`).

## How they work

Dart/TS builds a config object → serialises it to the bridge's JSON → one native call with a string → the bridge opens
the native checkout (Activity / view controller) → a JSON string comes back → parsed into the result type. 3DS, Apple Pay,
Google Pay and the card scanner all run inside the native screen.

The native glue only forwards strings. A new config field, result field or error code needs **no** native change; a new
method does.

| | Flutter | React Native |
|---|---|---|
| Channel / module | `MethodChannel('net.payrdr.sdk')`, methods `present`, `coreToken` | TurboModule `PayrdrSdk` (`src/NativePayrdrSdk.ts`), codegen `PayrdrSdkSpec` |
| Dart/TS API | `lib/src/{checkout,config,result,core}.dart`, exported by `lib/payrdr_sdk.dart` | `src/{checkout,config,result,core}.ts`, exported by `src/index.ts`; TS shipped as source, no build step |
| Android glue | `android/src/main/kotlin/.../flutter/PayrdrSdkPlugin.kt` (forwards `onActivityResult`) | `android/.../reactnative/PayrdrSdkModule.kt` + `PayrdrSdkPackage.kt` (`ActivityEventListener`) |
| iOS glue | `ios/payrdr_sdk/Sources/payrdr_sdk/PayrdrSdkPlugin.swift` | `ios/PayrdrSdk.mm` (ObjC++, calls `PSBPayrdrBridge…`) |
| iOS packaging | SwiftPM `ios/payrdr_sdk/Package.swift` **and** CocoaPods `ios/payrdr_sdk.podspec`, same sources | `PayrdrSdk.podspec` |

Mapping notes:
- Colours: Dart writes `#AARRGGBB` via `Color.toARGB32`; TS accepts CSS `#RGB/#RRGGBB/#RRGGBBAA` and converts, throwing
  synchronously on anything else (so `present` rejects).
- Error codes: Dart `CheckoutErrorCode` uses camelCase names carrying a `wire` string; TS uses the wire strings as a
  literal union (`ERROR_CODES`). An unknown wire code becomes `unknown` on both, so an older plugin survives a newer SDK.
- Core token types: public `card/cardWithoutOrder/savedCard/savedCardWithoutOrder` → wire `card/cardInstant/binding/bindingInstant`.
- Flutter always sends its booleans (`theme`, `cardScanner`, `showResultScreen`, `debugLogging`); RN sends only what
  the caller set. Both are fine because the bridge has defaults.

## Where the SDK comes from

Nothing is fetched from a registry. Run `scripts/build-bridge.sh` from the repo root after **any** change to the SDK or
the bridge, before running a plugin or a sample:
- Android: the plugins' Gradle files read `sdk-bridge-android` from `<repo>/build/maven`. Properties `payrdrMavenUrl` and
  `payrdrSdkVersion` override the location and version; the default version is written as text in
  `flutter/android/build.gradle.kts` and `react-native/android/build.gradle`; `scripts/set-version.sh` keeps it equal to
  `version` in the root `gradle.properties`, and `./gradlew verifyVersions` (in CI) fails when it is not.
- iOS: the script rsyncs `PayrdrSdkBridge.xcframework` into `flutter/ios/payrdr_sdk/Frameworks/` and
  `react-native/ios/Frameworks/`. Both folders are gitignored; a missing or stale framework is the usual cause of
  "symbol not found" or old behaviour.
- iOS, the scanner's camera: the script also copies the Swift sources of `RadarpaymentsSDKScanner` into
  `flutter/ios/payrdr_sdk/Sources/payrdr_sdk/Scanner/` and `react-native/ios/Scanner/` (gitignored too; edit them in
  `sdk-payment-scanner/ios/Sources/`). The plugin compiles them and passes `VisionScannerCamera()` to
  `PayrdrBridgeCheckout`. In React Native that makes the pod mixed: `PayrdrSdk.mm` imports the generated
  `PayrdrSdk-Swift.h` after the bridge's header.

The plugins' Android builds use their own toolchain (Flutter's: AGP 9.1 / Kotlin 2.4, compileSdk 36), not the root
version catalog. That is deliberate: they build inside the host app's Gradle, not this one.

## Tests

```bash
cd plugins/flutter && flutter test          # contract_test.dart (fixture parity) + payrdr_sdk_test.dart (mocked channel)
cd plugins/react-native && npm test         # jest: contract.test.ts + checkout.test.ts
cd plugins/react-native && npm run typecheck
```

`contract_test.dart` reads `../../sdk-bridge/contract/*.json` from disk; `contract.test.ts` imports them as JSON
modules. Both compare key sets exactly and round-trip every error code. The RN jest setup maps `react-native` to
`src/__tests__/react-native.mock.ts` (in both the jest config and tsconfig `paths`). CI does not run these: run them
whenever the contract or the plugin code changes.

## Conventions

- Keep the two plugins mirrors of each other: same public names, same defaults, same docs. A change to one is a change
  to the other in the same commit.
- Dart: `flutter_lints`; `analysis_options.yaml` excludes `android/` and `ios/`. Result classes have hand-written
  `==`/`hashCode` — update them with any new field.
- Changes go into the root `CHANGELOG.md` (rules in the root `AGENTS.md`); `flutter/CHANGELOG.md` is still the
  template and not maintained.
- iOS minimum: the SDK supports 14.0, the Flutter package declares 15, RN follows `min_ios_version_supported`. Do not
  raise it further without asking.
- Never send card data anywhere but `coreToken`, and never include request content in an error or a log line.
