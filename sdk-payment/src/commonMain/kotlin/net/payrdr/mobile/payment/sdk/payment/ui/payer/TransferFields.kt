package net.payrdr.mobile.payment.sdk.payment.ui.payer

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import kotlin.time.Clock

/**
 * The sender (`PAYER_*`) and recipient (`RECIPIENT_*`) fields of a transfer: how each is entered and in which order,
 * following the gateway's own payment page.
 */
internal object TransferFields {
    /** How a transfer field is entered. */
    enum class Kind {
        TEXT,

        /** `DD.MM.YYYY` on screen, `YYYYMMDD` for the gateway. */
        DATE,

        /** One of [ACCOUNT_TYPES]. */
        ACCOUNT_TYPE,

        /** One of [ID_TYPES]. */
        ID_TYPE,

        /** ISO 3166-1 alpha-2, from the country picker. */
        COUNTRY,
    }

    /** Sender fields in display order: the name first, the document last. */
    val SENDER: List<PayerField> =
        listOf(
            PayerField.PAYER_FIRST_NAME,
            PayerField.PAYER_MIDDLE_NAME,
            PayerField.PAYER_LAST_NAME,
            PayerField.PAYER_BIRTHDAY,
            PayerField.PAYER_ACCOUNT,
            PayerField.PAYER_ACCOUNT_NUMBER_TYPE,
            PayerField.PAYER_ID_TYPE,
            PayerField.PAYER_ID_NUMBER,
        )

    /** Recipient fields in display order: the name, the account, the address, the document. */
    val RECIPIENT: List<PayerField> =
        listOf(
            PayerField.RECIPIENT_FIRST_NAME,
            PayerField.RECIPIENT_MIDDLE_NAME,
            PayerField.RECIPIENT_LAST_NAME,
            PayerField.RECIPIENT_BIRTHDAY,
            PayerField.RECIPIENT_ACCOUNT,
            PayerField.RECIPIENT_ACCOUNT_NUMBER_TYPE,
            PayerField.RECIPIENT_COUNTRY,
            PayerField.RECIPIENT_STATE,
            PayerField.RECIPIENT_CITY,
            PayerField.RECIPIENT_ADDRESS_LINE1,
            PayerField.RECIPIENT_POSTAL_CODE,
            PayerField.RECIPIENT_ID_TYPE,
            PayerField.RECIPIENT_ID_NUMBER,
        )

    /** Account number type codes the gateway takes, in the order of its page. */
    val ACCOUNT_TYPES: List<String> = listOf("1", "2", "3", "4", "5", "6", "7", "8", "100")

    /** Names `getSessionStatus` returns for the account number types the order was registered with. */
    private val ACCOUNT_TYPE_NAMES: Map<String, String> =
        mapOf(
            "RTN" to "1",
            "IBAN" to "2",
            "CARD_ACCOUNT" to "3",
            "EMAIL" to "4",
            "PHONE_NUMBER" to "5",
            "BAN_AND_BIC" to "6",
            "WALLET_ID" to "7",
            "SOCIAL_NETWORK_ID" to "8",
            "OTHER" to "100",
        )

    /**
     * The code of an account number type as the gateway hands it back: registered as a code, it comes back as a name
     * (`7` → `WALLET_ID`). Null when the picker has no such type.
     */
    fun accountTypeCode(value: String): String? {
        val trimmed = value.trim()
        return trimmed.takeIf { it in ACCOUNT_TYPES } ?: ACCOUNT_TYPE_NAMES[trimmed.uppercase()]
    }

    /** Identity document type codes the gateway takes. */
    val ID_TYPES: List<String> = (1..11).map { "IDTP$it" }

    fun kind(field: PayerField): Kind =
        when (field) {
            PayerField.PAYER_BIRTHDAY, PayerField.RECIPIENT_BIRTHDAY -> Kind.DATE
            PayerField.PAYER_ACCOUNT_NUMBER_TYPE, PayerField.RECIPIENT_ACCOUNT_NUMBER_TYPE -> Kind.ACCOUNT_TYPE
            PayerField.PAYER_ID_TYPE, PayerField.RECIPIENT_ID_TYPE -> Kind.ID_TYPE
            PayerField.RECIPIENT_COUNTRY -> Kind.COUNTRY
            else -> Kind.TEXT
        }

    /** Codes offered for a choice field; empty for the others. */
    fun options(field: PayerField): List<String> =
        when (kind(field)) {
            Kind.ACCOUNT_TYPE -> ACCOUNT_TYPES
            Kind.ID_TYPE -> ID_TYPES
            else -> emptyList()
        }

    /** `YYYYMMDD` from the gateway → `DD.MM.YYYY`; anything else stays as it is. */
    fun dateForInput(value: String): String =
        if (value.length == API_DATE_LENGTH && value.all(Char::isDigit)) {
            "${value.substring(6, 8)}.${value.substring(4, 6)}.${value.substring(0, 4)}"
        } else {
            value
        }

    /** `DD.MM.YYYY` → `YYYYMMDD` for the gateway; anything else stays as it is. */
    fun dateForApi(value: String): String =
        DATE_INPUT.matchEntire(value.trim())?.destructured?.let { (day, month, year) -> "$year$month$day" } ?: value

    /**
     * Typed date text as `DD.MM.YYYY`, keeping only what can still become a real day from [MIN_YEAR] to today
     * ([todayYyyyMmDd]): a digit no such day can take is left out (`32`, `31.04`, `29.02.2023`, a day after today); a
     * digit that can only be a day or a month on its own gets its zero (`4` → `04`); a separator after a lone digit
     * closes the day or the month (`1.` → `01`). The dots come in as the digits reach them.
     */
    fun formatDateInput(
        text: String,
        todayYyyyMmDd: Int,
    ): String {
        val digits = StringBuilder()
        for (char in text) {
            if (digits.length == API_DATE_LENGTH) break
            if (char.isDigit()) {
                when {
                    canBecomeDate("$digits$char", todayYyyyMmDd) -> digits.append(char)
                    digits.length in PART_STARTS && canBecomeDate("${digits}0$char", todayYyyyMmDd) ->
                        digits.append('0').append(char)
                }
            } else if (char in SEPARATORS && digits.length - 1 in PART_STARTS) {
                val closed = StringBuilder(digits).insert(digits.length - 1, '0')
                if (canBecomeDate(closed.toString(), todayYyyyMmDd)) digits.setLength(0).also { digits.append(closed) }
            }
        }
        return buildString {
            digits.forEachIndexed { index, digit ->
                if (index == 2 || index == 4) append('.')
                append(digit)
            }
        }
    }

    /** A real day written `DD.MM.YYYY`, not in the future and not before [MIN_YEAR]. */
    fun isValidDate(
        value: String,
        todayYyyyMmDd: Int,
    ): Boolean {
        val (day, month, year) =
            DATE_INPUT
                .matchEntire(value.trim())
                ?.destructured
                ?.toList()
                ?.map(String::toInt) ?: return false
        if (year < MIN_YEAR || month !in 1..12) return false
        return day in 1..daysIn(month, year) && year * 10_000 + month * 100 + day <= todayYyyyMmDd
    }

    /** Today on the device, as `YYYYMMDD`. */
    fun today(): Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).let { it.year * 10_000 + it.month.number * 100 + it.day }

    /** True when the first digits of a `DDMMYYYY` date can still be finished into a real day from [MIN_YEAR] to today. */
    private fun canBecomeDate(
        digits: String,
        todayYyyyMmDd: Int,
    ): Boolean {
        val day = digits.take(2)
        val month = digits.drop(2).take(2)
        val year = digits.drop(4)
        return (MIN_YEAR..todayYyyyMmDd / 10_000).any { y ->
            y.startsWith(year, width = 4) &&
                (1..12).any { m ->
                    m.startsWith(month, width = 2) &&
                        (1..daysIn(m, y)).any { d -> d.startsWith(day, width = 2) && y * 10_000 + m * 100 + d <= todayYyyyMmDd }
                }
        }
    }

    /** True when this number, written with [width] digits (leading zeros), starts with [prefix]. */
    private fun Int.startsWith(
        prefix: String,
        width: Int,
    ): Boolean {
        if (prefix.isEmpty()) return true
        var divisor = 1
        repeat(width - prefix.length) { divisor *= 10 }
        return this / divisor == prefix.toInt()
    }

    private fun daysIn(
        month: Int,
        year: Int,
    ): Int {
        val leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
        return intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)[month - 1]
    }

    private val DATE_INPUT = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
    private const val API_DATE_LENGTH = 8
    private const val MIN_YEAR = 1900

    /** Where the day and the month start among the digits. */
    private val PART_STARTS = setOf(0, 2)

    /** What a user may type between the parts: ours, and the ones of other date formats. */
    private const val SEPARATORS = "./-, "
}
