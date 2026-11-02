package net.payrdr.mobile.payment.sdk.payment.ui.loading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberKeyValueStore(): KeyValueStore =
    remember {
        val defaults = NSUserDefaults.standardUserDefaults
        object : KeyValueStore {
            override fun getInt(key: String): Int? = if (defaults.objectForKey(key) != null) defaults.integerForKey(key).toInt() else null

            override fun putInt(
                key: String,
                value: Int,
            ) {
                defaults.setInteger(value.toLong(), forKey = key)
            }
        }
    }
