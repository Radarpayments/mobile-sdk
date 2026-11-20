package net.payrdr.mobile.payment.sdk.qa.ui

import net.payrdr.mobile.payment.sdk.qa.config.Currencies
import net.payrdr.mobile.payment.sdk.qa.config.OrderConfig
import net.payrdr.mobile.payment.sdk.qa.config.OrderSource
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.SdkOptions
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.config.minorToMajor
import java.net.URI

/** The row of [field], a path of the contract such as `order.mdOrder`. */
internal fun homeRow(field: String): HomeRow =
    when {
        field == "profile" || field.startsWith("profile.") -> HomeRow.STAND
        field == "order.amount" || field == "order.currency" -> HomeRow.AMOUNT
        field == "order.features" -> HomeRow.FEATURES
        field.startsWith("order.") -> HomeRow.ORDER
        field == "sdk.theme" -> HomeRow.THEME
        field.startsWith("sdk.googlePay.") -> HomeRow.GOOGLE_PAY
        field.startsWith("sdk.sslPinning.") -> HomeRow.SSL_PINNING
        else -> HomeRow.BEHAVIOUR
    }

/**
 * A problem as the value of a row: the message with the field's last name in front when the message needs a subject,
 * so `order.mdOrder` "is required" reads `mdOrder is required` and "select a stored profile" stays as it is.
 */
internal fun rowProblem(problem: Problem): String {
    val needsSubject = SUBJECTLESS.any { problem.message.startsWith("$it ") }
    return if (needsSubject) "${problem.field.substringAfterLast('.')} ${problem.message}" else problem.message
}

/** The rows of the home screen; each problem of the configuration is shown on the row whose screen fixes it. */
internal enum class HomeRow { STAND, ORDER, AMOUNT, FEATURES, THEME, BEHAVIOUR, GOOGLE_PAY, SSL_PINNING }

/** The fields the Order screen shows for [source]: the ones a run from that source uses. */
internal fun orderFields(source: OrderSource): List<String> =
    when (source) {
        OrderSource.REGISTER ->
            listOf(Tags.ORDER_TYPE, Tags.ORDER_AMOUNT, Tags.ORDER_CURRENCY, Tags.ORDER_FEATURES, Tags.ORDER_CLIENT_ID, Tags.ORDER_EXTRA)
        OrderSource.MD_ORDER -> listOf(Tags.ORDER_MD_ORDER)
        OrderSource.SESSION_ID -> listOf(Tags.ORDER_SESSION_ID)
    }

/** `register · payment`, `mdOrder · 01ea8a09-…`, `sessionId · ps_…`; the id is `empty` while there is none. */
internal fun orderSummary(order: OrderConfig): String =
    when (order.source) {
        OrderSource.REGISTER -> "${order.source.json} · ${order.type.json}"
        OrderSource.MD_ORDER -> "${order.source.json} · ${order.mdOrder ?: EMPTY}"
        OrderSource.SESSION_ID -> "${order.source.json} · ${order.sessionId ?: EMPTY}"
    }

/** The amount in major units with its currency, `10.00 EUR`. */
internal fun amountSummary(order: OrderConfig): String =
    "${minorToMajor(order.amount, Currencies.minorDigits(order.currency))} ${order.currency}"

internal fun featuresSummary(order: OrderConfig): String = order.features.joinToString(", ").ifEmpty { NONE }

/** The extra parameters' names, `none` without any. */
internal fun extraSummary(order: OrderConfig): String =
    order.extra.keys
        .joinToString(", ")
        .ifEmpty { NONE }

internal fun clientIdSummary(order: OrderConfig): String = order.clientId ?: NONE

/** `en · result · scanner · key provider`: the locale or `system locale`, then the switches that are on. */
internal fun behaviourSummary(sdk: SdkOptions): String =
    listOfNotNull(
        sdk.locale ?: "system locale",
        "result".takeIf { sdk.showResultScreen },
        "scanner".takeIf { sdk.scanner },
        "key provider".takeIf { sdk.keyProviderUrl != null },
    ).joinToString(" · ")

/** `off`, `on`, or `on · test` when an environment is set. */
internal fun googlePaySummary(sdk: SdkOptions): String {
    val googlePay = sdk.googlePay
    if (!googlePay.enabled) return OFF
    return listOfNotNull("on", googlePay.environment).joinToString(" · ")
}

/** `off`, `1 pin`, `2 pins`. */
internal fun sslPinningSummary(sdk: SdkOptions): String {
    val pinning = sdk.sslPinning
    if (!pinning.enabled) return OFF
    val count = pinning.pins.size
    return if (count == 1) "1 pin" else "$count pins"
}

/** The second line of a stand: `dev.bpcbt.com · API v1`. */
internal fun standSummary(profile: StandProfile): String = "${hostOf(profile.gatewayUrl)} · API ${profile.api.json}"

private fun hostOf(url: String): String = runCatching { URI(url).host }.getOrNull() ?: url

// Messages of the checks that read as a predicate of the field: "is required", "must be digits …".
private val SUBJECTLESS = listOf("is", "must", "has", "are", "cannot")
private const val NONE = "none"
private const val OFF = "off"
private const val EMPTY = "empty"
