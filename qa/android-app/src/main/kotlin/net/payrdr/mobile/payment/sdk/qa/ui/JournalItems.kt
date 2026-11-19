package net.payrdr.mobile.payment.sdk.qa.ui

import net.payrdr.mobile.payment.sdk.payment.logger.LogLevel
import net.payrdr.mobile.payment.sdk.qa.journal.JournalEntry
import net.payrdr.mobile.payment.sdk.qa.launcher.QaRun
import net.payrdr.mobile.payment.sdk.qa.merchant.Merchant
import java.net.URI

/** What the journal screen shows for one or two entries: a gateway exchange or a single line. */
internal sealed interface JournalItem {
    val entries: List<JournalEntry>

    /** A request of `merchant` and the answer to it, if one came. */
    class Exchange(
        val request: JournalEntry,
        val response: JournalEntry?,
    ) : JournalItem {
        override val entries: List<JournalEntry> get() = listOfNotNull(request, response)

        private val requestParts = request.message.removePrefix(REQUEST).split(' ', limit = 3)

        val method: String get() = requestParts.getOrElse(0) { "" }

        val url: String get() = requestParts.getOrElse(1) { "" }

        /** The last segment of the URL's path, `register.do`; the whole URL when it has none. */
        val path: String get() =
            url
                .substringBefore('?')
                .trimEnd('/')
                .substringAfterLast('/')
                .ifEmpty { url }

        val host: String get() = runCatching { URI(url).host }.getOrNull() ?: url

        /** The request as journalled after its URL: the masked headers and the body. */
        val requestDetail: String get() = requestParts.getOrElse(2) { "" }

        /** The HTTP status, or null when the answer is not in yet or never came. */
        val status: Int? get() = response?.message?.let(::statusOf)

        /** No answer at all: a timeout or a network error, journalled as `← <message>`. */
        val unanswered: Boolean get() = response != null && status == null

        val failed: Boolean get() = unanswered || status?.let { it !in HTTP_OK } == true

        /** The answer as journalled after its status, or the reason it never came. */
        val responseDetail: String?
            get() {
                val text = response?.message?.removePrefix(RESPONSE) ?: return null
                return if (status != null) text.drop(STATUS_LENGTH).trimStart() else text
            }

        /** `200 · 4.3 s`, `no answer · 20 s`, or `…` while the answer is awaited. */
        val outcome: String
            get() {
                val answer = response ?: return "…"
                val duration = durationText(answer.timeMillis - request.timeMillis)
                return "${status ?: "no answer"} · $duration"
            }
    }

    /** An entry of the SDK, of the app's run, or a gateway answer whose request is gone from the journal. */
    class Line(
        val entry: JournalEntry,
    ) : JournalItem {
        override val entries: List<JournalEntry> get() = listOf(entry)

        /** The short source shown in the line's second column. */
        val source: String
            get() =
                when (entry.tag) {
                    Merchant.TAG -> "GW"
                    QaRun.TAG -> "QA"
                    else -> "SDK"
                }
    }
}

/**
 * Pairs each request of `merchant` with the next answer of `merchant`, which is the answer to it: the app sends one
 * gateway request at a time and journals the answer, or its absence, before the next one. Any other entry is a line of
 * its own, and the entries stay in the order they were journalled. The journal itself is not changed, so the report
 * and the copied text keep one line per entry.
 */
internal fun journalItems(entries: List<JournalEntry>): List<JournalItem> {
    val items = mutableListOf<JournalItem>()
    var open = NONE
    entries.forEach { entry ->
        val gateway = entry.tag == Merchant.TAG
        when {
            gateway && entry.message.startsWith(REQUEST) -> {
                items += JournalItem.Exchange(entry, null)
                open = items.lastIndex
            }
            gateway && entry.message.startsWith(RESPONSE) && open != NONE -> {
                items[open] = JournalItem.Exchange((items[open] as JournalItem.Exchange).request, entry)
                open = NONE
            }
            else -> items += JournalItem.Line(entry)
        }
    }
    return items
}

/** The journal screen's segments. [id] is the text of `qa_journal_level` and the end of each segment's identifier. */
internal enum class JournalFilter(
    val id: String,
    val label: String,
) {
    ALL("all", "All"),
    GATEWAY("gateway", "Gateway"),
    SDK("sdk", "SDK"),
    ERRORS("errors", "Errors"),
    ;

    fun accepts(item: JournalItem): Boolean =
        when (this) {
            ALL -> true
            GATEWAY -> item.entries.all { it.tag == Merchant.TAG }
            SDK -> item.entries.none { it.tag == Merchant.TAG }
            ERRORS -> item.entries.any { it.level >= LogLevel.WARNING } || (item as? JournalItem.Exchange)?.failed == true
        }
}

/** The items [filter] keeps whose tag or message of some entry contains [query], ignoring case. */
internal fun List<JournalItem>.shown(
    filter: JournalFilter,
    query: String,
): List<JournalItem> =
    filter { item ->
        filter.accepts(item) &&
            item.entries.any { it.tag.contains(query, ignoreCase = true) || it.message.contains(query, ignoreCase = true) }
    }

/** Under ten seconds with one decimal (`4.3 s`), whole seconds from there (`20 s`); digits are ASCII in any locale. */
internal fun durationText(millis: Long): String {
    val safe = millis.coerceAtLeast(0)
    if (safe >= TEN_SECONDS) return "${(safe + HALF_SECOND) / ONE_SECOND} s"
    val tenths = (safe + HALF_TENTH) / ONE_TENTH
    return "${tenths / TENTHS}.${tenths % TENTHS} s"
}

private fun statusOf(message: String): Int? {
    val text = message.removePrefix(RESPONSE)
    val code = text.take(STATUS_LENGTH)
    val ended = text.length == STATUS_LENGTH || text.getOrNull(STATUS_LENGTH) == ' '
    return if (ended && code.length == STATUS_LENGTH && code.all { it in '0'..'9' }) code.toInt() else null
}

private const val REQUEST = "→ "
private const val RESPONSE = "← "
private const val STATUS_LENGTH = 3
private const val NONE = -1
private val HTTP_OK = 200..299
private const val ONE_SECOND = 1_000L
private const val HALF_SECOND = 500L
private const val TEN_SECONDS = 10_000L
private const val ONE_TENTH = 100L
private const val HALF_TENTH = 50L
private const val TENTHS = 10L
