package net.payrdr.mobile.payment.sdk.payment.ui.util

import java.util.Locale

internal actual fun countryDisplayName(
    code: String,
    language: String,
): String = Locale("", code).getDisplayCountry(Locale(language)).takeIf { it.isNotBlank() && it != code } ?: code
