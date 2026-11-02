package net.payrdr.mobile.payment.sdk.payment.ui.util

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.currentLocale

internal actual fun deviceRegion(): String? =
    (NSLocale.currentLocale.objectForKey(NSLocaleCountryCode) as? String)?.takeIf {
        it.length == 2
    }
