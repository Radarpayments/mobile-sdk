package net.payrdr.mobile.payment.sdk.payment.ui.util

import java.util.Locale

internal actual fun deviceRegion(): String? = Locale.getDefault().country.takeIf { it.length == 2 }
