// Compose resources (still in 1.10) picks the language from the device only; its environment hook is internal.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(ExperimentalResourceApi::class, InternalResourceApi::class)

package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.LayoutDirection
import org.jetbrains.compose.resources.ComposeEnvironment
import org.jetbrains.compose.resources.DensityQualifier
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.LocalComposeEnvironment
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.ThemeQualifier

/** Languages the SDK's screens are translated into; any other falls back to English. */
public val SupportedPaymentLanguages: List<String> = listOf("en", "ru", "ro", "el", "bg", "de", "fr", "es", "ar")

/**
 * Shows the SDK's screens inside [content] in [locale] — a language tag such as `"de"` or `"ar-AE"` — instead of
 * the system language, and lays them out right to left for right-to-left languages (Arabic). Only the SDK's own
 * texts change: the app around them keeps its language. `null` or a blank tag follows the system.
 *
 * [PaymentCheckout] applies [net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig.locale] itself; wrap
 * the standalone screens (`CardFormScreen`, `ResultScreen`, …) in this when you show them on their own.
 */
@Composable
public fun PaymentLocaleProvider(
    locale: String?,
    content: @Composable () -> Unit,
) {
    val parts =
        locale
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.replace('_', '-')
            ?.split('-')
    val language = parts?.first()?.lowercase()
    val region =
        parts
            ?.drop(1)
            ?.firstOrNull { it.length == 2 }
            ?.uppercase()
            .orEmpty()
    val environment = remember(language, region) { language?.let { LanguageEnvironment(it, region) } }
    // One provider whether a language is set or not: two call sites would drop the screens' state on a change.
    CompositionLocalProvider(
        LocalComposeEnvironment provides (environment ?: LocalComposeEnvironment.current),
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

/** The language the SDK's texts are shown in: for names it builds itself (countries), not only its resources. */
internal val LocalSdkLanguage = staticCompositionLocalOf { Locale.current.language }

/** Resources in a fixed language; theme and density still follow the device. */
private class LanguageEnvironment(
    private val language: String,
    private val region: String,
) : ComposeEnvironment {
    @Composable
    override fun rememberEnvironment(): ResourceEnvironment {
        val dark = isSystemInDarkTheme()
        val density = LocalDensity.current.density
        return remember(dark, density) {
            ResourceEnvironment(
                LanguageQualifier(language),
                RegionQualifier(region),
                ThemeQualifier.selectByValue(dark),
                DensityQualifier.selectByDensity(density),
            )
        }
    }
}

private val RtlLanguages = setOf("ar", "fa", "he", "iw", "ur")
