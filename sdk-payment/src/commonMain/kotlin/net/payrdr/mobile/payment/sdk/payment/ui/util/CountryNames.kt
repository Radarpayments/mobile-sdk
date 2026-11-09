package net.payrdr.mobile.payment.sdk.payment.ui.util

/**
 * Country name for an ISO 3166-1 alpha-2 [code] in [language] (`en`, `ru`, …), from the platform;
 * falls back to the code itself.
 */
internal expect fun countryDisplayName(
    code: String,
    language: String,
): String
