package net.payrdr.mobile.payment.sdk.payment.scanner

/**
 * Supplies the card scanner of the checkout. Lives in `sdk-payment` so that `sdk-payment` can depend on the
 * abstraction alone; concrete implementations ship in separate modules (e.g. `sdk-payment-scanner`) so the scanner
 * can be fully omitted from a build.
 *
 * The scanner is responsible for: camera permission, preview, OCR, state machine, and UX. It MUST eventually report
 * exactly one [CardScannerResult]. What a provider implements differs per platform: a Composable screen on Android,
 * the native scanner contract on iOS.
 */
public expect interface CardScannerProvider
