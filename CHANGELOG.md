# Changelog

Notable changes to the PAYRDR Mobile SDK. Versions follow [Semantic Versioning](https://semver.org); the format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- `PaymentTextStyle.fontFamily` takes a font the app ships (`PaymentFontFamily`); `null` keeps the system font.

### Changed

- iOS: the checkout and the card scanner screens are SwiftUI instead of Compose Multiplatform. The framework no longer
  carries the Compose and Skia runtime: the stripped `sdk_payment_scanner` binary drops from about 39 MB to about 9 MB
  and the zipped XCFramework from 71 MB to 22 MB. `PayrdrCheckout`, the SwiftUI modifier and the results are unchanged.
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
