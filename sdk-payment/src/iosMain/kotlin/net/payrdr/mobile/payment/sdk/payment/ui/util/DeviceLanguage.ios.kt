package net.payrdr.mobile.payment.sdk.payment.ui.util

import net.payrdr.mobile.payment.sdk.payment.presentation.languageOfTag
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

// The first preferred language, not the current locale's: an app without a German localisation still runs in a
// German-first system with the current locale in English.
internal actual fun deviceLanguage(): String = languageOfTag(NSLocale.preferredLanguages.firstOrNull() as? String) ?: "en"
