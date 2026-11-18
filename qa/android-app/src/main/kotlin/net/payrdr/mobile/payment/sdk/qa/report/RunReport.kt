package net.payrdr.mobile.payment.sdk.qa.report

import net.payrdr.mobile.payment.sdk.qa.config.ApiVersion
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.QaConfigJson
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.journal.JournalEntry
import net.payrdr.mobile.payment.sdk.qa.journal.line
import net.payrdr.mobile.payment.sdk.qa.launcher.ResultType
import net.payrdr.mobile.payment.sdk.qa.launcher.RunOutcome
import net.payrdr.mobile.payment.sdk.qa.merchant.OrderStatus
import java.util.TimeZone

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
 * The plain text QA attaches to a bug: versions, device, the profile without its credentials, the configuration the
 * sheet was opened with, the result, the order status and the journal of the run.
 */
internal fun runReport(
    app: AppInfo,
    config: QaConfig,
    profile: StandProfile,
    outcome: RunOutcome,
    entries: List<JournalEntry>,
    zone: TimeZone = TimeZone.getDefault(),
): String =
    listOf(
        section("Versions", "QA app: ${app.appVersion}", "SDK: ${app.sdkVersion}"),
        section("Device", app.device, app.os),
        section("Profile", "Name: ${profile.name}", "Gateway URL: ${profile.gatewayUrl}", "API: ${profile.api.json}"),
        section("Configuration", QaConfigJson.encode(config)),
        section(
            "Result",
            "qa_result_type: ${outcome.type.id}",
            "mdOrder: ${outcome.mdOrder ?: NONE}",
            "Session id: ${outcome.sessionId ?: NONE}",
            "Error: ${outcome.error ?: NONE}",
            "HTTP status: ${outcome.httpStatus ?: NONE}",
        ),
        section("Order status", statusLine(outcome, profile)),
        section("Journal", entries.map { it.line(zone) }),
    ).joinToString("\n\n", postfix = "\n")

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
