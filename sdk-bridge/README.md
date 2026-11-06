# sdk-bridge

The SDK for apps that are not written in Kotlin or Swift. The Flutter plugin (`plugins/flutter`) and the React Native
module (`plugins/react-native`) are thin: they pass JSON strings between their language and this module, which turns them
into the SDK's own types and back. All mapping lives here, once, with its tests (`src/commonTest`).

| Platform | Entry point | Shipped as |
|---|---|---|
| Android | `PayrdrBridgeCheckout.start(activity, requestJson, onResult)` + `handleActivityResult(...)` | `io.github.radarpayments:sdk-bridge-android` (AAR) |
| iOS | `PayrdrBridgeCheckout().present(requestJson:from:onResult:)` | `PayrdrSdkBridge.xcframework` |
| both | `PayrdrBridge.coreToken(requestJson)` | |

`scripts/build-bridge.sh [debug|release]` builds both: the Android libraries into `build/maven`, the framework into each
plugin's iOS folder.

## Why it is shaped like this

- **One Kotlin framework.** `PayrdrSdkBridge.xcframework` holds all of the SDK (core, payment, scanner): an app gets one
  Kotlin/Native runtime. The checkout's strings and images (Compose resources) are inside each framework, where Compose
  Multiplatform 1.9+ finds them, so the app needs no build phase whether the framework comes through Swift Package Manager,
  CocoaPods or Xcode.
- **No changes to the app's activity.** The sheet starts with `startActivityForResult` (`PayrdrCheckoutIntent`), so a
  plain `FlutterActivity` or `ReactActivity` works; the plugin forwards activity results.
- **Google Pay included.** `sdk-payment` keeps `play-services-wallet` optional; the bridge depends on it and declares
  `com.google.android.gms.wallet.api.enabled` in its manifest, so a plugin user touches neither Gradle nor the manifest.

## Checkout request

```json
{
  "config": {
    "baseUrl": "https://pay.example.com/payment",
    "locale": "de",
    "theme": { "mode": "custom", "base": "dark", "colors": { "primary": "#FFF3D9B1" }, "cornerRadius": 18, "buttonCornerRadius": 999 },
    "googlePay": { "merchantName": "Hearth & Co", "environment": "TEST" },
    "applePay": { "merchantId": "merchant.com.example" },
    "cardScanner": true,
    "showResultScreen": false,
    "keyProviderUrl": "https://pay.example.com/payment/se/keys.do",
    "sslPins": [{ "host": "pay.example.com", "sha256": "<44 characters of base64>" }],
    "debugLogging": false
  },
  "mdOrder": "<order registered by the merchant's server>"
}
```

- Only `config.baseUrl` and one of `mdOrder` / `sessionId` (API v2, `ps_…`) are required; everything else keeps the
  SDK's default. Unknown fields are ignored, so a newer plugin works with an older SDK.
- `theme.mode`: `system` (default), `light`, `dark` or `custom`. A custom theme starts from `base` (`light` | `dark`) and
  changes only the colours it names, as `#RRGGBB` or `#AARRGGBB`: `primary`, `onPrimary`, `background`, `onBackground`,
  `surface`, `onSurface`, `error`, `onError`, `outline`, `success`, `onSurfaceVariant`, `placeholder`, `outlineStrong`,
  `scrim`. `cornerRadius` (fields and cards) and `buttonCornerRadius` are in dp / points.
- `googlePay`: `merchantId`, `merchantName`, `gateway`, `gatewayMerchantId`, `environment` (`TEST` | `PRODUCTION`),
  `countryCode`, `cardNetworks` (`AMEX`, `DISCOVER`, `INTERAC`, `JCB`, `MASTERCARD`, `VISA`), `authMethods` (`PAN_ONLY`,
  `CRYPTOGRAM_3DS`), `billingAddressRequired`. Any of them may be left out: the SDK takes it from the gateway and the
  order. `{}` turns Google Pay on with everything from the gateway.
- `applePay`: `merchantId` (required), `countryCode`, `networks` (`AMEX`, `DISCOVER`, `JCB`, `MASTERCARD`, `VISA`),
  `capabilities` (`THREE_D_SECURE`, `EMV`, `CREDIT`, `DEBIT`).

## Checkout result

```json
{ "status": "failed", "orderId": "…", "error": { "code": "declined", "message": "Insufficient funds", "declineCode": "116" } }
```

| `status` | Meaning |
|---|---|
| `completed` | The gateway confirmed the payment. |
| `failed` | The payment did not happen, or the sheet could not open; `error` says why. |
| `canceled` | The buyer closed the sheet. |
| `unknown` | The payment was sent but how it ended is not known; `error` is the last one seen, if any. Check the order on the server. |

`error.code`: `declined` (+ `declineCode`), `api` (+ `httpStatus`, `gatewayErrorCode`), `network`, `transaction`,
`session_not_found`, `order_not_found`, `session_expired` (the time to pay ran out; the order is
declined, register a new one), `invalid_session_id`, `cryptogram`,
`not_configured`, `three_ds_failed`, `three_ds_timeout`, `google_pay`, `apple_pay`, `unknown` — and,
when nothing opened, `invalid_request` (the request broke this contract), `busy` (a checkout is already open) or
`no_screen` (no activity / key window to open over).

## SDK Core token

Request: `{"type": "card" | "cardInstant" | "binding" | "bindingInstant", "pubKey": "<PEM>", ...}`

| `type` | Fields |
|---|---|
| `card` | `mdOrder`, `pan`, `expiry` (`MM/YY`), `cvc`, `cardholder` (optional) |
| `cardInstant` | as `card`, without `mdOrder` |
| `binding` | `mdOrder`, `bindingId`, `cvc` (optional) |
| `bindingInstant` | `bindingId`, `cvc` (optional) |

Result: `{"token": "…"}`, or `{"errors": {"pan": "required", …}}` with sdk-core's codes (`required`, `invalid`,
`invalid-format`) keyed by `pan`, `expiry`, `cvc`, `cardholder`, `bindingId`, `mdOrder`, `pubKey`, or
`{"error": {"code": "invalid_request", "message": "…"}}`.

## Sample messages

`contract/` holds sample messages of every kind above: what the plugins send (`checkout-requests.json`,
`core-token-requests.json`), what the bridge sends back (`checkout-results.json`, `core-token-results.json`) and every
error code (`error-codes.json`). Three test suites read the same files: `ContractTest` here, `test/contract_test.dart`
of the Flutter plugin and `src/__tests__/contract.test.ts` of the React Native one. A change to the contract is a change
to these files; every side that does not follow it fails, and a case added to a file fails each side until it has an
answer for it.
