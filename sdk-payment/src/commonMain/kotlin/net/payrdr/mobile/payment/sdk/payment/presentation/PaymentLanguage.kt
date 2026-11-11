package net.payrdr.mobile.payment.sdk.payment.presentation

internal val RtlLanguages: Set<String> = setOf("ar", "fa", "he", "iw", "ur")

/** The language of a tag such as `"de"`, `"ar-AE"` or `"pt_BR"`, in lower case; null for a null or blank tag. */
internal fun languageOfTag(tag: String?): String? =
    tag
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.replace('_', '-')
        ?.split('-')
        ?.first()
        ?.lowercase()

/**
 * The language the checkout speaks: the one of [localeTag] (`SdkPaymentConfig.locale`), or [systemLanguage] when
 * the tag is null or blank. It may be one the SDK has no texts for: the texts then fall back to English while
 * the layout direction and the number formats still follow the language.
 */
@PayrdrUiApi
public fun resolveLanguage(
    localeTag: String?,
    systemLanguage: String,
): String = languageOfTag(localeTag) ?: languageOfTag(systemLanguage) ?: "en"

/** The screens of [language] are laid out right to left. */
@PayrdrUiApi
public fun isRtl(language: String): Boolean = language in RtlLanguages
