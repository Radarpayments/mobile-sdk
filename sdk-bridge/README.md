# sdk-bridge

The SDK for apps written in neither Kotlin nor Swift. The [Flutter plugin](../plugins/flutter/README.md) and the
[React Native module](../plugins/react-native/README.md) send this module a JSON string and get a JSON string back.
Reading the JSON, building the SDK's types and writing the result all happen here, in one place, with tests in
`src/commonTest`. The plugins only carry strings.

You need this page if you work on a plugin or write your own. Apps using a plugin don't call the bridge directly.

## Entry points

| Platform | Call | Shipped as |
|---|---|---|
| Android | `PayrdrBridgeCheckout.start(activity, requestJson, onResult)`, then forward activity results to `handleActivityResult(requestCode, resultCode, data)` | `io.github.radarpayments:sdk-bridge-android` (AAR) |
| iOS | `PayrdrBridgeCheckout(scannerCamera:).present(requestJson:from:onResult:)`; `dismiss()` closes the sheet, and its result still arrives | `PayrdrSdkBridge.xcframework` |
| Both | `PayrdrBridge.coreToken(requestJson)` returns the result JSON at once | |

On Android, call `start` on the main thread. The sheet opens with `startActivityForResult` and request code
`PayrdrBridgeCheckout.REQUEST_CODE` (`0x7A01`); don't use that code for anything else in the app.

`scripts/build-bridge.sh [debug|release]` builds both: the Android libraries into `build/maven`, and the framework with
the scanner's camera sources into each plugin's iOS folder. Run it before building a plugin or a Flutter / React Native sample.

## Why it is built this way

- **One framework on iOS.** `PayrdrSdkBridge.xcframework` contains the whole SDK: core, payment and scanner. An app
  can hold only one Kotlin/Native runtime, so the SDK can't come as several frameworks. Only the scanner's camera is
  outside it: a few Swift files (`sdk-payment-scanner/ios/Sources/RadarpaymentsSDKScanner`) that the plugin compiles
  and passes in as `PayrdrBridgeCheckout(scannerCamera: VisionScannerCamera())`. Without a camera, `cardScanner` in a
  request shows no scan button. The sheet's texts and images are inside the framework, so the app needs no extra build
  phase, whether it uses Swift Package Manager, CocoaPods or plain Xcode.
- **The app's activity stays as it is.** Because the sheet starts with `startActivityForResult`, a plain
  `FlutterActivity` or `ReactActivity` works. The plugin only forwards activity results.
- **Google Pay works out of the box.** `sdk-payment` leaves Google Pay optional; the bridge depends on
  `play-services-wallet` and enables the Wallet API in its manifest. A plugin user touches neither Gradle nor the
  manifest.

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

Required: `config.baseUrl`, plus either `mdOrder` (API V1) or `sessionId` (API V2, `ps_…`). Everything else has the
SDK's default.

Unknown fields are ignored, so a newer plugin works with an older bridge. An unknown value is different: a colour
name, theme mode or card network the bridge doesn't know makes the request `invalid_request`.

**`theme`**
- `mode`: `system` (default), `light`, `dark` or `custom`.
- A `custom` theme starts from `base` (`light` or `dark`) and changes only the colours you list. Colours are
  `#RRGGBB` or `#AARRGGBB`. Names: `primary`, `onPrimary`, `background`, `onBackground`, `surface`, `onSurface`,
  `error`, `onError`, `outline`, `success`, `onSurfaceVariant`, `placeholder`, `outlineStrong`, `scrim`.
- `cornerRadius` rounds fields and cards, `buttonCornerRadius` rounds buttons; both are in dp / points. Without
  `buttonCornerRadius`, buttons take `cornerRadius`.

**`googlePay`** — every field is optional; the SDK takes what's missing from the gateway and the order. `{}` turns
Google Pay on with everything from the gateway.
- `merchantId`, `merchantName`, `gateway`, `gatewayMerchantId`, `countryCode`, `billingAddressRequired`
- `environment`: `TEST` or `PRODUCTION`
- `cardNetworks`: `AMEX`, `DISCOVER`, `INTERAC`, `JCB`, `MASTERCARD`, `VISA`
- `authMethods`: `PAN_ONLY`, `CRYPTOGRAM_3DS`

**`applePay`**
- `merchantId` (required), `countryCode`
- `networks`: `AMEX`, `DISCOVER`, `JCB`, `MASTERCARD`, `VISA`
- `capabilities`: `THREE_D_SECURE`, `EMV`, `CREDIT`, `DEBIT`

## Checkout result

```json
{ "status": "failed", "orderId": "…", "error": { "code": "declined", "message": "Insufficient funds", "declineCode": "116" } }
```

| `status` | Meaning |
|---|---|
| `completed` | The gateway confirmed the payment. |
| `failed` | The payment didn't happen, or the sheet couldn't open. `error` says why. |
| `canceled` | The buyer closed the sheet. |
| `unknown` | The payment was sent, but its outcome isn't known. `error` is the last error seen, if any. Check the order on your server. |

When the checkout was opened with a `sessionId`, `orderId` is an empty string.

`error.code` when the sheet opened:

| Code | What happened |
|---|---|
| `declined` | The bank declined the payment; `declineCode` has the gateway's code |
| `api` | The gateway answered with an error; extra fields `httpStatus` and `gatewayErrorCode` |
| `network` | The gateway couldn't be reached |
| `transaction` | The payment couldn't start, or the order was in an unexpected state |
| `order_not_found`, `session_not_found` | The gateway doesn't know this order or session |
| `session_expired` | The time to pay ran out; the order is declined, register a new one |
| `invalid_session_id` | The `sessionId` isn't a valid `ps_…` ID |
| `cryptogram` | The card data couldn't be encrypted |
| `not_configured` | The merchant is set up at the gateway for something this SDK doesn't support, such as the native 3DS2 SDK |
| `three_ds_failed`, `three_ds_timeout` | 3-D Secure failed or wasn't finished in time |
| `google_pay`, `apple_pay` | The wallet failed |
| `unknown` | Any other error |

`error.code` when nothing opened (status `failed`):

| Code | What happened |
|---|---|
| `invalid_request` | The request breaks this contract |
| `busy` | A checkout is already open |
| `no_screen` | There is no activity or key window to open the sheet over |

## SDK Core token

The request is a JSON object with a `type`, the gateway's public key `pubKey` (PEM) and the fields of that type:

| `type` | Fields |
|---|---|
| `card` | `mdOrder`, `pan`, `expiry` (`MM/YY`), `cvc`, `cardholder` (optional) |
| `cardInstant` | the same, without `mdOrder` |
| `binding` | `mdOrder`, `bindingId`, `cvc` (optional) |
| `bindingInstant` | `bindingId`, `cvc` (optional) |

The result is one of three:

- `{"token": "…"}` — the token for the gateway.
- `{"errors": {"pan": "required", …}}` — the data didn't pass validation. Keys: `pan`, `expiry`, `cvc`, `cardholder`,
  `bindingId`, `mdOrder`, `pubKey`, `storedPaymentId`, `unknown`. Values: `required`, `invalid`, `invalid-format`.
- `{"error": {"code": "invalid_request", "message": "…"}}` — the request breaks this contract.

## Contract files

`contract/` holds sample messages for everything above:

| File | Contents |
|---|---|
| `checkout-requests.json`, `core-token-requests.json` | What the plugins send |
| `checkout-results.json`, `core-token-results.json` | What the bridge sends back |
| `error-codes.json` | Every error code |

Three test suites read these files: `ContractTest` here, `test/contract_test.dart` in the Flutter plugin and
`src/__tests__/contract.test.ts` in the React Native module. To change the contract, change these files. A side that
doesn't follow the change fails its tests, and a new case fails on every side until that side handles it.
