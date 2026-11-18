package net.payrdr.mobile.payment.sdk.qa.journal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.payrdr.mobile.payment.sdk.payment.logger.LogEvent
import net.payrdr.mobile.payment.sdk.payment.logger.LogLevel
import net.payrdr.mobile.payment.sdk.payment.logger.LogTransport
import java.util.TimeZone

internal data class JournalEntry(
    val timeMillis: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
)

/**
 * The SDK's log of one run, kept in memory as it was logged: nothing is filtered or redacted here. Holds the last
 * [capacity] entries, oldest first.
 */
internal class Journal(
    private val capacity: Int = 2000,
    private val clock: () -> Long = System::currentTimeMillis,
) : LogTransport {
    init {
        require(capacity > 0) { "capacity must be positive, was $capacity" }
    }

    private val lock = Any()
    private val buffer = ArrayDeque<JournalEntry>(capacity)
    private val state = MutableStateFlow<List<JournalEntry>>(emptyList())

    /** Each value is a copy that never changes afterwards. */
    val entries: StateFlow<List<JournalEntry>> = state.asStateFlow()

    override fun log(event: LogEvent) {
        val cause = event.cause
        record(event.level, event.tag, if (cause == null) event.message else event.message + cause.suffix())
    }

    fun record(
        level: LogLevel,
        tag: String,
        message: String,
    ) {
        // The SDK logs from any coroutine. One lock instead of a compare-and-set loop on the flow: a retry would copy the
        // whole list again, and publishing inside the lock keeps an older copy from overwriting a newer one.
        synchronized(lock) {
            if (buffer.size == capacity) buffer.removeFirst()
            buffer.addLast(JournalEntry(clock(), level, tag, message))
            state.value = buffer.toList()
        }
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            state.value = emptyList()
        }
    }

    // An anonymous class has an empty simple name; its JVM name still says where it was declared.
    private fun Throwable.suffix(): String {
        val name = javaClass.simpleName.ifEmpty { javaClass.name }
        return if (message == null) " ($name)" else " ($name: $message)"
    }
}

internal fun List<JournalEntry>.filtered(
    minLevel: LogLevel,
    query: String,
): List<JournalEntry> =
    filter { entry ->
        entry.level >= minLevel && (entry.tag.contains(query, ignoreCase = true) || entry.message.contains(query, ignoreCase = true))
    }

/**
 * `HH:mm:ss.SSS LEVEL tag: message` in [zone], the device's zone by default. Formatted by hand: `java.time` needs API 26
 * (minSdk is 24, no desugaring), a shared `SimpleDateFormat` is not thread-safe, and both would print the digits of an
 * Arabic locale.
 */
internal fun JournalEntry.line(zone: TimeZone = TimeZone.getDefault()): String {
    val millisOfDay = Math.floorMod(timeMillis + zone.getOffset(timeMillis), MILLIS_PER_DAY)
    val hours = millisOfDay / MILLIS_PER_HOUR
    val minutes = millisOfDay / MILLIS_PER_MINUTE % MINUTES_PER_HOUR
    val seconds = millisOfDay / MILLIS_PER_SECOND % SECONDS_PER_MINUTE
    val millis = millisOfDay % MILLIS_PER_SECOND
    return "${hours.pad(2)}:${minutes.pad(2)}:${seconds.pad(2)}.${millis.pad(3)} $level $tag: $message"
}

private fun Long.pad(width: Int): String = toString().padStart(width, '0')

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
private const val MILLIS_PER_MINUTE = MILLIS_PER_SECOND * SECONDS_PER_MINUTE
private const val MILLIS_PER_HOUR = MILLIS_PER_MINUTE * MINUTES_PER_HOUR
private const val MILLIS_PER_DAY = MILLIS_PER_HOUR * 24
