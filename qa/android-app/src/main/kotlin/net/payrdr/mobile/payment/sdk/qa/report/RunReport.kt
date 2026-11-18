package net.payrdr.mobile.payment.sdk.qa.report

import net.payrdr.mobile.payment.sdk.qa.config.ApiVersion
import net.payrdr.mobile.payment.sdk.qa.config.QaConfigJson
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.journal.JournalEntry
import net.payrdr.mobile.payment.sdk.qa.journal.line
import net.payrdr.mobile.payment.sdk.qa.launcher.ResultType
import net.payrdr.mobile.payment.sdk.qa.launcher.RunOutcome
import net.payrdr.mobile.payment.sdk.qa.launcher.RunState
import net.payrdr.mobile.payment.sdk.qa.merchant.OrderStatus
import java.util.TimeZone
import kotlin.math.abs

/** The `qa_result_order_status` text of the contract: the status, `error: <message>` or `not queried`. */
internal fun OrderStatus.text(): String =
    when (this) {
        is OrderStatus.Known ->
            listOfNotNull("orderStatus=${orderStatus ?: NONE}", "actionCode=${actionCode ?: NONE}", actionCodeDescription)
                .joinToString(", ")
        is OrderStatus.Failed -> "error: $message"
        OrderStatus.NotQueried -> "not queried"
    }

/**
 * The plain text QA attaches to a bug, built from the [finished] run alone: versions, device, the run's profile without
 * its credentials, the configuration it was launched with, the result, the order status and the journal of the run.
 * The format is the contract's "Run report" (qa/README.md); both apps print the same text.
 */
internal fun runReport(
    app: AppInfo,
    finished: RunState.Finished,
    entries: List<JournalEntry>,
    zone: TimeZone = TimeZone.getDefault(),
): String {
    val outcome = finished.outcome
    val profile = finished.profile
    return listOf(
        section("Versions", "QA app: ${app.appVersion}", "SDK: ${app.sdkVersion}"),
        section("Device", app.device, app.os),
        section("Profile", "Name: ${profile.name}", "Gateway URL: ${profile.gatewayUrl}", "API: ${profile.api.json}"),
        section("Configuration", QaConfigJson.encode(finished.config)),
        section(
            "Result",
            "qa_result_type: ${outcome.type.id}",
            "mdOrder: ${outcome.mdOrder ?: NONE}",
            "Session id: ${outcome.sessionId ?: NONE}",
            "Error: ${outcome.error ?: NONE}",
            "HTTP status: ${outcome.httpStatus ?: NONE}",
        ),
        section("Order status", statusLine(outcome, profile)),
        section("Journal", listOf(journalTimes(zone, entries)) + entries.map { it.line(zone) }),
    ).joinToString("\n\n", postfix = "\n")
}

// The zone of the first line's time (of now when there is none), so a reader in another zone can line the times up.
private fun journalTimes(
    zone: TimeZone,
    entries: List<JournalEntry>,
): String = "Time zone: ${utcOffset(zone, entries.firstOrNull()?.timeMillis ?: System.currentTimeMillis())}"

/** `UTC+03:00`, `UTC-05:30`, `UTC+00:00`: built by hand, so no locale changes the digits or the sign. */
internal fun utcOffset(
    zone: TimeZone,
    atMillis: Long,
): String {
    val offsetMinutes = zone.getOffset(atMillis) / MILLIS_PER_MINUTE
    val sign = if (offsetMinutes < 0) "-" else "+"
    val minutes = abs(offsetMinutes)
    return "UTC$sign${(minutes / MINUTES_PER_HOUR).pad()}:${(minutes % MINUTES_PER_HOUR).pad()}"
}

private fun Int.pad(): String = toString().padStart(2, '0')

private fun section(
    title: String,
    vararg lines: String,
): String = section(title, lines.asList())

private fun section(
    title: String,
    lines: List<String>,
): String = (listOf("== $title ==") + lines).joinToString("\n")

private fun statusLine(
    outcome: RunOutcome,
    profile: StandProfile,
): String {
    val text = outcome.status.text()
    if (outcome.status != OrderStatus.NotQueried) return text
    val reason =
        when {
            outcome.type == ResultType.REGISTRATION_FAILED || outcome.type == ResultType.SDK_ERROR -> "the checkout did not run"
            profile.api == ApiVersion.V2 -> "API v2 has no status query"
            profile.login.isBlank() || profile.password.isBlank() -> "the profile has no login and password"
            else -> "the checkout named no order"
        }
    return "$text: $reason"
}

private const val NONE = "-"
private const val MILLIS_PER_MINUTE = 60_000
private const val MINUTES_PER_HOUR = 60
