# Changelog

Notable changes to the PAYRDR Mobile SDK. Versions follow [Semantic Versioning](https://semver.org); the format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- `PaymentTextStyle.fontFamily` takes a font the app ships (`PaymentFontFamily`); `null` keeps the system font.

### Changed

- iOS: the SDK ships as three Swift Package Manager products instead of the one `PayrdrSDK` / `sdk_payment_scanner`:
  `RadarpaymentsSDKPayment` (the payment sheet, with SDK Core inside), `RadarpaymentsSDKScanner` (the card scanner) and
  `RadarpaymentsSDKCore` (SDK Core alone, for an app with its own payment form). Replace `import sdk_payment_scanner`
  with `import RadarpaymentsSDKPayment`; an app that passes `MlKitVisionCardScannerProvider` also adds the
  `RadarpaymentsSDKScanner` product and imports it. An app takes either `RadarpaymentsSDKPayment` or
  `RadarpaymentsSDKCore`, never both. The release assets are `RadarpaymentsSDKPayment.xcframework.zip`,
  `RadarpaymentsSDKCore.xcframework.zip` and `RadarpaymentsSDKScanner-<version>.zip` (Swift sources).
- iOS: the payment framework no longer contains camera code, so an app without the scanner product links no capture
  API. `MlKitVisionCardScannerProvider(logTransport:)` now comes from `RadarpaymentsSDKScanner`, and `logTransport` may
  be left out.
- iOS: SDK Core types lose the `Sdk_core` prefix in Swift: `Sdk_coreTokenResult` is `TokenResult`,
  `Sdk_corePaymentCardParams` is `PaymentCardParams`.
- The Flutter plugin's release archive is `payrdr-flutter-sdk-<version>.tar.gz` (was `payrdr_sdk-<version>.tar.gz`);
  the folder inside is still `payrdr_sdk`.
- iOS: the checkout and the card scanner screens are SwiftUI instead of Compose Multiplatform. The framework no longer
  carries the Compose and Skia runtime: the stripped binary drops from about 39 MB to about 9 MB and the zipped
  XCFramework from 71 MB to 22 MB. `PayrdrCheckout`, the SwiftUI modifier and the results are unchanged.
- The theme no longer uses Compose types, so the same `PaymentTheme.Custom` works on both platforms: colours are
  `PaymentColor` (ARGB), text styles `PaymentTextStyle` (size, weight, line height, letter spacing, tabular figures)
  and corners `PaymentCorners` (`Radius` or `Pill`). On Android, `PaymentColors(...)` and `PaymentTypography(...)`
  overloads take Compose `Color` and `TextStyle`, and `Color.toPaymentColor()`, `PaymentColor.toColor()` and
  `TextStyle.toPaymentTextStyle()` convert single values; iOS apps build the theme from `PaymentColor` values.

### Fixed

- After a card was added (a save-card-only session), the next payment sheet opened as "New card" and switched to the
  saved cards a moment later: the add-card session overwrote the remembered card count used for the skeleton. It no
  longer does.
- iOS: a step whose content had not been measured yet no longer makes the sheet jump up for one frame.
