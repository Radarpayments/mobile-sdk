package net.payrdr.mobile.payment.sdk.qa.ui

import net.payrdr.mobile.payment.sdk.qa.config.OrderConfig
import net.payrdr.mobile.payment.sdk.qa.config.OrderSource
import net.payrdr.mobile.payment.sdk.qa.journal.ClockPrecision
import net.payrdr.mobile.payment.sdk.qa.journal.JournalEntry
import net.payrdr.mobile.payment.sdk.qa.journal.timeOfDay
import net.payrdr.mobile.payment.sdk.qa.launcher.ResultType
import net.payrdr.mobile.payment.sdk.qa.merchant.OrderStatus
import java.util.TimeZone

/** What a run of [type] came to, as a word for people; `qa_result_type` keeps the contract's id. */
internal fun outcomeTitle(type: ResultType): String =
    when (type) {
        ResultType.COMPLETED -> "Completed"
        ResultType.FAILED -> "Failed"
        ResultType.CANCELED -> "Canceled"
        ResultType.UNKNOWN -> "Unknown"
        ResultType.REGISTRATION_FAILED -> "Registration failed"
        ResultType.SDK_ERROR -> "SDK error"
    }

/** How an outcome is coloured: the only colour of the app besides field errors. */
internal enum class Tone { OK, BAD, NEUTRAL }

internal fun outcomeTone(type: ResultType): Tone =
    when (type) {
        ResultType.COMPLETED -> Tone.OK
        ResultType.FAILED, ResultType.REGISTRATION_FAILED, ResultType.SDK_ERROR -> Tone.BAD
        ResultType.CANCELED, ResultType.UNKNOWN -> Tone.NEUTRAL
    }

/** The order's status in short: `status 2 · action 0`, `status error`, `status not queried`. */
internal fun statusSummary(status: OrderStatus): String =
    when (status) {
        is OrderStatus.Known -> "status ${status.orderStatus ?: MISSING} · action ${status.actionCode ?: MISSING}"
        is OrderStatus.Failed -> "status error"
        OrderStatus.NotQueried -> "status not queried"
    }

/** The result banner's first line of detail: `orderStatus 2 · actionCode 0`, `status error: <message>`, `not queried`. */
internal fun statusDetail(status: OrderStatus): List<String> =
    when (status) {
        is OrderStatus.Known ->
            listOfNotNull(
                "orderStatus ${status.orderStatus ?: MISSING} · actionCode ${status.actionCode ?: MISSING}",
                status.actionCodeDescription,
            )
        is OrderStatus.Failed -> listOf("status error: ${status.message}")
        OrderStatus.NotQueried -> listOf("status not queried")
    }

/** The second line of the home screen's last run: `10:50 · status 2 · action 0`. */
internal fun lastRunSummary(
    timeMillis: Long,
    status: OrderStatus,
    zone: TimeZone = TimeZone.getDefault(),
): String = "${timeOfDay(timeMillis, zone, ClockPrecision.MINUTES)} · ${statusSummary(status)}"

/** `payment · 10.00 EUR` for a registered order; the source and its id otherwise. */
internal fun launchedOrderSummary(order: OrderConfig): String =
    if (order.source == OrderSource.REGISTER) "${order.type.json} · ${amountSummary(order)}" else orderSummary(order)

/**
 * `2 entries`: the items the journal screen lists, a gateway request and its answer counting once, so the number matches
 * what the Journal row opens.
 */
internal fun entriesSummary(entries: List<JournalEntry>): String {
    val count = journalItems(entries).size
    return if (count == 1) "1 entry" else "$count entries"
}

// A status field the gateway left out.
private const val MISSING = "-"
