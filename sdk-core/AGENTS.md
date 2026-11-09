# sdk-core — agent notes

Read the root `AGENTS.md` first.

## What this module is

Headless card tokenization for merchants with their own (PCI DSS) card form: it validates card data and encrypts it on
the device into a token the merchant's server passes to the gateway as `seToken`. No UI, no Android `Context`, no state.
Its only extra dependency is kotlinx-datetime. `sdk-payment` uses it for its own card form, so a change here changes the
checkout too.

## Public API

Package `net.payrdr.mobile.payment.sdk.core`:

- `SdkCore().generate(SdkCoreConfig): TokenResult` and `version()`.
- `SdkCoreConfig(cardParams, timestamp /* epoch ms */, registeredFrom = MSDK_CORE)`; `MsdkRegisteredFrom` is
  `MSDK_CORE` / `MSDK_FORMS` / `MSDK_PAYMENT` and goes into the payload.
- `TokenResult`: exactly one of `token` / `errors` is set (`withToken` / `withErrors`), `isSuccessful` says which.
- Params, all under the sealed `PaymentCardParams` (`model/`):
  - new card: `CardParams` (with mdOrder), `CardInstantParams` (no order), `NewPaymentMethodCardParams`;
  - saved card: `BindingParams` (bindingID + mdOrder), `BindingInstantParams`, `NewPaymentMethodStoredCardParams`
    (`pm_{base58}` id, decoded by `utils/BindingUtils.kt`).
- Errors: a map `ParamField → code`, codes in `validation/ValidationCodes` (`required`, `invalid`, `invalid-format`;
  the lowercase property names are deliberate).

Swift cannot see subclasses nothing references, so iOS gets factories in
`sdk-payment/src/iosMain/.../ios/IosSdkCoreSurface.kt` (`iosCardParams`, `iosCardInstantParams`, `iosBindingParams`). A
new params class needs one there. The bridge maps its wire types (`card`, `cardInstant`, `binding`, `bindingInstant`) in
`sdk-bridge/.../PayrdrBridge.kt`.

Everything else is `internal`; what `sdk-payment` and the scanner share from here (`CardRules`, `Base58Coder`, `Key`) is
`@InternalPayrdrApi`, defined in `InternalPayrdrApi.kt` (opt-in at ERROR level, hidden from Objective-C).

## Map

| Package | What it does |
|---|---|
| root | `SdkCore` (validation order, payload, encryption), `TokenResult`, `InternalPayrdrApi` |
| `crypto/` | `RsaCryptogramCipher` (Base64 of RSA PKCS#1 v1.5), `PemKeyParser`, `expect class Rsa` |
| `model/` | public params and `ParamField`; internal `CardInfo`, `ExpiryDate`, … |
| `processor/` | `DefaultPaymentStringProcessor`: builds the plain-text payload |
| `validation/`, `validation/rules/` | validators per field; `CardRules` shared with the form and the scanner |
| `utils/` | `Base58Coder`, `BindingUtils`, string helpers |

Only `Rsa` is platform code: Android `KeyFactory` + `Cipher("RSA/ECB/PKCS1Padding")`; iOS Security framework
(`SecKeyCreateWithData`, `SecKeyCreateEncryptedData`) with a `CFRelease` on every path — keep it that way when editing.

## Rules

- **The payload format is a wire contract.** `timestamp/uuid/pan/cvv/expdate/mdOrder/bindingId/cardholder/registeredFrom`,
  the same as android-sdk 3.x. The gateway reads it by position. Do not reorder, rename or reformat a field.
  - timestamp `yyyy-MM-ddTHH:mm:ss±HH:MM` in the device's zone; a fresh UUID per call;
  - expdate `yyyyMM` with the month zero-padded, because the P2P gateway reads it by position (`02/28` → `202802`, not
    `20282`); `MM/YY` → `20YY`;
  - pan empty for a saved card, bindingId empty for a new card; mdOrder empty for instant / new-payment-method params.
- **Validation order is the form's order** and stops at the first failure, so `errors` has one entry. New card: PAN,
  EXPIRY, CVC, CARDHOLDER, MD_ORDER, PUB_KEY; saved card: BINDING_ID / STORED_PAYMENT_ID, CVC, MD_ORDER, PUB_KEY. Null
  optional fields are skipped. The checks are a list, not a map, on purpose: two fields with equal values must both be
  checked (there is a regression test).
- Encryption failures: `IllegalArgumentException` → `PUB_KEY: invalid`, anything else → `UNKNOWN: unknown`.
- **Card data never leave except in the token.** Params are `data class`es, so `toString()` prints the PAN and CVC: never
  log or interpolate them. Only error codes leave the module; validators' messages are internal debug text.
- The RSA output is randomised: never assert a fixed token in a test; decrypt or check the shape.
- `SdkCore.VERSION` is hardcoded and must follow `version` in `gradle.properties` (see the root notes).

## Tests

```bash
./gradlew :sdk-core:testDebugUnitTest :sdk-core:iosSimulatorArm64Test
./gradlew :sdk-core:apiCheck        # :sdk-core:apiDump after an intended API change
```

commonTest runs on the Android JVM and on the iOS simulator (there is no `jvm()` target). Fixtures were ported from
android-sdk 3.x; tests that build a payload pin the time zone (Europe/Nicosia) so the timestamp is stable. `TokenRoundTripTest` opens
the token with the private half of `TestRsaKeys` (`decryptWithTestKey`: javax.crypto on the JVM, Security.framework in
iosTest) and checks every field the gateway reads. The test private key is Base64 DER without PEM armour, so secret
scanners do not flag the public repository; generate a new pair rather than reuse a real key. There is no
detekt baseline here: every finding fails the build.
