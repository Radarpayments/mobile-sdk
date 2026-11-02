package net.payrdr.mobile.payment.samples.android

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

/** Languages the demo and the SDK are translated into, each named in itself; the setting lists them in this order. */
val DemoLanguages: List<Pair<String, String>> =
    listOf(
        "en" to "English",
        "ru" to "Русский",
        "ro" to "Română",
        "el" to "Ελληνικά",
        "bg" to "Български",
        "de" to "Deutsch",
        "fr" to "Français",
        "es" to "Español",
        "ar" to "العربية",
    )

/**
 * The language the demo shows its own screens in; the SDK gets the same one through `SdkPaymentConfig.locale`
 * (stored in `AppPrefs`). Kept in plain SharedPreferences because the activity needs it synchronously, before its
 * first frame, in [attachBaseContext][MainActivity.attachBaseContext]. `null` follows the system.
 */
object AppLanguage {
    private const val FILE = "app_language"
    private const val KEY = "code"

    fun get(context: Context): String? = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)

    /** Saves [code] and recreates [activity] so that every screen, dialogs and sheets too, reads it. */
    fun set(
        activity: Activity,
        code: String?,
    ) {
        save(activity, code)
        activity.recreate()
    }

    /** Saves [code] for the next activity start; null follows the system. */
    fun save(
        context: Context,
        code: String?,
    ) {
        context
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, code)
            .commit()
    }

    /**
     * [base] with the chosen language, right to left for Arabic. Latin digits in Arabic too: card data and the
     * SDK's texts use them, prices should match.
     */
    fun wrap(base: Context): Context {
        val code = get(base) ?: return base
        val locale = Locale.forLanguageTag(if (code == "ar") "ar-u-nu-latn" else code)
        val configuration =
            Configuration(base.resources.configuration).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
        return base.createConfigurationContext(configuration)
    }
}

/** The activity behind a Compose context, through any wrappers. */
fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
