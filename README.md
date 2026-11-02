# PAYRDR Mobile SDK (Kotlin Multiplatform)

Unified Kotlin Multiplatform implementation of the PAYRDR Mobile SDK for Android and iOS.
Supersedes the separate `android-sdk` and `ios-sdk` repositories starting from version `4.0.0`.

## Modules

- **sdk-core** — headless tokenization library (card validation, RSA cryptogram, payment-string processor).
- **sdk-payment** — payment SDK with UI (Compose Multiplatform), PaymentApi, 3DS web challenge, Apple Pay / Google Pay.
- **sdk-payment-scanner** — optional card scanner (ML Kit on Android, Vision framework on iOS).

## Languages

The checkout's screens are translated into English, Russian, Romanian, Greek, Bulgarian, German, French, Spanish
and Arabic (laid out right to left). By default they follow the device language; to fix one, pass it in the
config — `SdkPaymentConfig(baseUrl = …, locale = "de")` — or wrap standalone screens in `PaymentLocaleProvider`.
The order amount is written the way that language writes amounts (`€1,234.50`, `1.234,50 €`).

## Build

```bash
./gradlew build                           # Build all modules + run all tests
./gradlew :sdk-core:allTests              # Run sdk-core tests on all targets
./gradlew :sdk-core:iosSimulatorArm64Test # Run sdk-core tests on iOS simulator (requires macOS)
```

## Documentation

- [Card scanner](sdk-payment-scanner/README.md)
- [Bridge for Flutter and React Native: the JSON contract](sdk-bridge/README.md)
- [Flutter plugin](plugins/flutter/README.md), [React Native plugin](plugins/react-native/README.md)
- [Sample apps](samples/README.md)

## License

[MIT](LICENSE.md) © BPC
