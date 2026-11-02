package net.payrdr.mobile.payment.sdk.core.processor

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import net.payrdr.mobile.payment.sdk.core.model.CardBindingIdIdentifier
import net.payrdr.mobile.payment.sdk.core.model.CardInfo
import net.payrdr.mobile.payment.sdk.core.model.CardPanIdentifier
import net.payrdr.mobile.payment.sdk.core.model.ExpiryDate
import net.payrdr.mobile.payment.sdk.core.model.MsdkRegisteredFrom
import kotlin.time.Instant

/**
 * Default [PaymentStringProcessor] implementation matching the android-sdk 3.x wire format.
 *
 * @param zone time zone used for timestamp formatting — defaults to system default for production;
 *             tests override to a fixed zone for stable fixtures.
 */
public class DefaultPaymentStringProcessor(
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : PaymentStringProcessor {
    override fun createPaymentString(
        order: String,
        timestamp: Long,
        uuid: String,
        cardInfo: CardInfo,
        registeredFrom: MsdkRegisteredFrom,
    ): String {
        val identifier = cardInfo.identifier
        return buildString {
            // 1. timestamp formatted as yyyy-MM-ddTHH:mm:ss±HH:MM
            append(formatTimestamp(timestamp))
            append(SPLASH)
            // 2. uuid
            append(uuid)
            append(SPLASH)
            // 3. pan (empty if saved card)
            append(if (identifier is CardPanIdentifier) identifier.value else "")
            append(SPLASH)
            // 4. cvv
            append(cardInfo.cvv.orEmpty())
            append(SPLASH)
            // 5. expdate as yyyyMM, the month zero-padded: the P2P gateway reads it by position (02/28 → 202802, not 20282)
            append(cardInfo.expDate?.format().orEmpty())
            append(SPLASH)
            // 6. mdOrder
            append(order)
            append(SPLASH)
            // 7. bindingId (empty if new card)
            append(if (identifier is CardBindingIdIdentifier) identifier.value else "")
            append(SPLASH)
            // 8. cardholder
            append(cardInfo.cardHolder.orEmpty())
            append(SPLASH)
            // 9. registeredFrom wire value
            append(registeredFrom.registeredFromValue)
        }
    }

    private fun formatTimestamp(epochMs: Long): String {
        val instant = Instant.fromEpochMilliseconds(epochMs)
        val local = instant.toLocalDateTime(zone)
        val offsetSeconds = zone.offsetAt(instant).totalSeconds
        val sign = if (offsetSeconds >= 0) "+" else "-"
        val offsetMinutesAbs = kotlin.math.abs(offsetSeconds) / SECONDS_PER_MINUTE
        val offsetHours = offsetMinutesAbs / MINUTES_PER_HOUR
        val offsetMinutes = offsetMinutesAbs % MINUTES_PER_HOUR

        return buildString {
            append(local.year.toString().padStart(YEAR_WIDTH, '0'))
            append('-')
            append(local.month.number.pad2())
            append('-')
            append(local.day.pad2())
            append('T')
            append(local.hour.pad2())
            append(':')
            append(local.minute.pad2())
            append(':')
            append(local.second.pad2())
            append(sign)
            append(offsetHours.pad2())
            append(':')
            append(offsetMinutes.pad2())
        }
    }

    private fun Int.pad2(): String = toString().padStart(2, '0')

    private fun ExpiryDate.format(): String = "$expYear${expMonth.toString().padStart(2, '0')}"

    private companion object {
        const val SPLASH = "/"
        const val SECONDS_PER_MINUTE = 60
        const val MINUTES_PER_HOUR = 60
        const val YEAR_WIDTH = 4
    }
}
