# sdk-bridge — agent notes

Read the root `AGENTS.md` first, and `README.md` here for the wire format itself.

## What this module is

The SDK for apps not written in Kotlin or Swift. The Flutter and React Native plugins pass JSON strings in and get JSON
strings back; every mapping between JSON and the SDK's Kotlin types lives here, once, with its tests. The plugins hold no
mapping of their own, and their native glue only forwards strings.

It depends on `:sdk-core`, `:sdk-payment` and `:sdk-payment-scanner`. On iOS it is the one framework an app embeds
(`PayrdrSdkBridge.xcframework`, all of the SDK inside, so the app has a single Kotlin/Native runtime). On Android it is
published as `sdk-bridge-android`, and it brings `play-services-wallet` and the Google Pay manifest entry with it, so
plugin users touch neither Gradle nor the manifest.

## Map

`src/commonMain/kotlin/net/payrdr/mobile/payment/sdk/bridge/`

| File | What it does |
|---|---|
| `PayrdrBridge.kt` | `PayrdrBridge.coreToken(requestJson)`: SDK Core token → `{"token"}`, `{"errors":{field:code}}` or `{"error":{…}}`. Field keys come from the `ParamField` → key mapping here. |
| `CheckoutRequest.kt` | Parsing the checkout request: `ConfigDto`, `ThemeDto`, `GooglePayDto`, `ApplePayDto`, `PinDto`, and `toSdkConfig`. `BridgeJson` settings. |
| `CheckoutResultJson.kt` | Encoding a `CheckoutResult` (`completed`/`failed`/`canceled`/`unknown`); `codeOf` maps each `Sdk*Exception` to its wire code. |
| `OrderIdFill.kt` | Results for a checkout that never opened: `busy`, `no_screen`. |

Platform entry points, `PayrdrBridgeCheckout.kt`:
- androidMain: `start(activity, requestJson, onResult)` (`@MainThread`, `startActivityForResult` with request code
  `0x7A01`) and `handleActivityResult(...)`, which the plugin calls from its activity-result hook. No `dismiss`.
- iosMain: `present(requestJson, from, onResult)` (falls back to the key window's root view controller) and `dismiss()`.

One checkout at a time on both platforms; a second one gets `busy`, a missing activity or window gets `no_screen`.

## Rules

- **Never echo the request back.** A request that does not parse answers `invalid_request` with only the JSON path
  (`BridgeJson.malformed`). kotlinx's own message quotes the input, and a `coreToken` request carries the card number and
  CVC; apps log plugin errors.
- **Forward compatibility.** `BridgeJson` has `ignoreUnknownKeys = true` (a newer plugin works with an older SDK) and
  `explicitNulls = false`. Keep both.
- **Wire naming.** camelCase keys, snake_case error codes, UPPERCASE wallet enum values, colours as `#AARRGGBB`.
- **The contract is the fixtures.** `contract/*.json` are shared test fixtures, not codegen input. The Kotlin tests, the
  Flutter tests and the React Native tests all read them and compare key sets exactly. A case added to a file fails every
  side until it has an answer.

## Changing the contract

Every change touches all three sides in one commit. Checklists:

**New error code**
1. A branch in `CheckoutResultJson.codeOf` (or a constant in `OrderIdFill.kt` for a checkout that did not open).
2. The code in `contract/error-codes.json`, and the exception in the error-code test in `ContractTest.kt`.
3. Dart: a value in `CheckoutErrorCode` (`plugins/flutter/lib/src/result.dart`) with its doc comment.
4. TS: the code in `ERROR_CODES` (`plugins/react-native/src/result.ts`).
5. The `error.code` list in `README.md` here. Optionally a case in `checkout-results.json`.

**New config field**
1. `ConfigDto` (or a sub-DTO) and `toSdkConfig` in `CheckoutRequest.kt`.
2. A case in `contract/checkout-requests.json` and its expectation in `ContractTest.kt`.
3. Dart `PayrdrConfig` field and `toJson` (Flutter always sends its booleans) + `plugins/flutter/test/contract_test.dart`.
4. TS `PayrdrConfig` (RN sends fields only when set) + `plugins/react-native/src/__tests__/contract.test.ts`.
5. "Checkout request" in `README.md`; the plugin READMEs if integrators see it.

**New result field**: `errorOf` in `CheckoutResultJson.kt`; Dart `CheckoutError` (`fromJson`, `==`, `hashCode`); TS
`CheckoutError` and `errorFromWire`; the fixtures.

**New method**: the public bridge API (and the API dumps), then both plugins' native glue — Flutter `onMethodCall`
(Kotlin) and `handle` (Swift); RN `NativePayrdrSdk.ts` spec, `PayrdrSdkModule.kt` and `PayrdrSdk.mm`.

Adding or removing a `@Serializable` DTO changes `api/sdk-bridge.klib.api` even when it is `internal`: run
`./gradlew :sdk-bridge:apiDump` and commit the dump.

## Build and test

```bash
./gradlew :sdk-bridge:testDebugUnitTest :sdk-bridge:iosSimulatorArm64Test
./gradlew :sdk-bridge:apiCheck            # :sdk-bridge:apiDump after an intended API change
scripts/build-bridge.sh [debug|release]   # Android libs → build/maven, XCFramework → both plugins
```

- `generateContractFixtures` (in `build.gradle.kts`) embeds every `contract/*.json` into a generated `ContractFixtures`
  object for commonTest, because iOS tests cannot read project files. The constant is the file name upper-cased with `-`
  → `_` (`CHECKOUT_REQUESTS`).
- `ContractTest` swaps the fixtures' `pubKey: "PEM"` for a real test key.
- The bridge has no UI of its own and no Compose. On iOS the checkout it opens is the SDK's SwiftUI sheet: SKIE
  bundles that Swift code into `PayrdrSdkBridge.framework`, which is why the module applies SKIE and the framework
  exports sdk-payment (the Swift code names sdk-payment's types without a prefix). No resources ship next to it.
- CI runs the Kotlin side only; after changing the contract run `flutter test` in `plugins/flutter` and `npm test` in
  `plugins/react-native` yourself.
