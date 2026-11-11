package net.payrdr.mobile.payment.sdk.payment.ui.loading

import platform.Foundation.NSUserDefaults

/** The app's user defaults. */
internal class UserDefaultsStore : KeyValueStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getInt(key: String): Int? = if (defaults.objectForKey(key) != null) defaults.integerForKey(key).toInt() else null

    override fun putInt(
        key: String,
        value: Int,
    ) {
        defaults.setInteger(value.toLong(), forKey = key)
    }
}
