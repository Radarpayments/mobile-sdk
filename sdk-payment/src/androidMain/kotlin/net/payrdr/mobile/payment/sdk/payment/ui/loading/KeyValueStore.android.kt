package net.payrdr.mobile.payment.sdk.payment.ui.loading

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val PREFS = "net.payrdr.mobile.payment.sdk"

/** The app's own on-device store, in shared preferences. */
@Composable
internal fun rememberKeyValueStore(): KeyValueStore {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        object : KeyValueStore {
            override fun getInt(key: String): Int? = if (prefs.contains(key)) prefs.getInt(key, 0) else null

            override fun putInt(
                key: String,
                value: Int,
            ) {
                prefs.edit().putInt(key, value).apply()
            }
        }
    }
}
