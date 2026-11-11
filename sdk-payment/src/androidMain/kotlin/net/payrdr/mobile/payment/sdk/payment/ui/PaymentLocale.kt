package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.LayoutDirection
import net.payrdr.mobile.payment.sdk.core.InternalPayrdrApi
import net.payrdr.mobile.payment.sdk.payment.presentation.RtlLanguages
import net.payrdr.mobile.payment.sdk.payment.presentation.languageOfTag

/** Languages the SDK's screens are translated into; any other falls back to English. */
internal val SupportedPaymentLanguages: List<String> = listOf("en", "ru", "ro", "el", "bg", "de", "fr", "es", "ar")

/**
 * Shows the SDK's screens inside [content] in [locale] — a language tag such as `"de"` or `"ar-AE"` — instead of
 * the system language, and lays them out right to left for right-to-left languages (Arabic). Only the SDK's own
 * texts change: the app around them keeps its language. `null` or a blank tag follows the system.
 *
 * [PaymentCheckout] applies [net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig.locale] itself; wrap
 * the standalone screens (`CardFormScreen`, `ResultScreen`, …) in this when you show them on their own.
 */
@Composable
internal fun PaymentLocaleProvider(
    locale: String?,
    content: @Composable () -> Unit,
) {
    val language = languageOfTag(locale)
    // One provider whether a language is set or not: two call sites would drop the screens' state on a change.
    CompositionLocalProvider(
        LocalLayoutDirection provides
            when (language) {
                null -> LocalLayoutDirection.current
                in RtlLanguages -> LayoutDirection.Rtl
                else -> LayoutDirection.Ltr
            },
        LocalSdkLanguage provides (language ?: Locale.current.language),
        content = content,
    )
}

/**
 * The language the SDK's texts are shown in, its own strings and the names it builds (countries). Public for the
 * scanner module, whose screen shows inside the checkout.
 */
@InternalPayrdrApi
public val LocalSdkLanguage: ProvidableCompositionLocal<String> = staticCompositionLocalOf { Locale.current.language }
