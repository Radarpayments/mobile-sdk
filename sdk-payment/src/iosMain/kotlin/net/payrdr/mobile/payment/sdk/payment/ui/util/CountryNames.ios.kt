package net.payrdr.mobile.payment.sdk.payment.ui.util

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode

internal actual fun countryDisplayName(
    code: String,
    language: String,
): String = NSLocale(localeIdentifier = language).displayNameForKey(NSLocaleCountryCode, code)?.takeIf { it.isNotBlank() } ?: code
