package net.payrdr.mobile.payment.sdk.payment.scanner

/**
 * Supplies the card scanner of the checkout; see the common declaration for the contract. The checkout shows a scan
 * button only for a provider that also implements `NativeCardScanner`, which the provider of `sdk-payment-scanner`
 * does.
 */
public actual interface CardScannerProvider
