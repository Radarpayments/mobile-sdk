package net.payrdr.mobile.payment.sdk.payment.ui.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import net.payrdr.mobile.payment.sdk.core.validation.CardExpiryValidator
import net.payrdr.mobile.payment.sdk.core.validation.CardNumberValidator
import kotlin.time.Clock

/** Specific, user-facing problem with one field of the card form. */
internal enum class FieldError {
    PAN_EMPTY,
    PAN_SHORT,
    PAN_INVALID,
    EXPIRY_FORMAT,
    EXPIRY_MONTH,
    EXPIRY_PAST,
    CVV_3,
    CVV_4,
    HOLDER_EMPTY,
    HOLDER_FORMAT,
    HOLDER_SHORT,
    EMAIL_EMPTY,
    EMAIL_INVALID,
    PHONE_EMPTY,
    PHONE_SHORT,
    PHONE_LONG,
    PHONE_NO_CODE,
    FIELD_REQUIRED,
    COUNTRY_EMPTY,
    LATIN_ONLY,
    POSTAL_CODE_INVALID,
    DATE_INVALID,
}

/**
 * Turns raw field input into a [FieldError] (or null when valid). The final pass/fail verdict for
 * card fields still comes from the sdk-core validators so the UI never accepts what tokenization rejects.
 */
internal object CardInputValidation {
    private val HOLDER = Regex("^[A-Za-z .'`-]+$")
    private const val MIN_HOLDER_LENGTH = 2
    private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun pan(digits: String): FieldError? {
        if (digits.isEmpty()) return FieldError.PAN_EMPTY
        // A length the network does not issue (e.g. 17 digits of Visa) is an unfinished number.
        if (digits.length !in CardFormatters.panLengths(CardNetworkResolver.resolve(digits))) return FieldError.PAN_SHORT
        return if (CardNumberValidator().validate(digits).isValid) null else FieldError.PAN_INVALID
    }

    /**
     * Name on the card: 2–26 characters as printed (ISO/IEC 7813), Latin letters, spaces and `. - ' \``
     * as on the gateway's payment page. Digits are rejected: the gateway refuses `JOHN123`.
     */
    fun holder(value: String): FieldError? {
        val name = value.trim()
        return when {
            name.isEmpty() -> FieldError.HOLDER_EMPTY
            !HOLDER.matches(name) || name.none(Char::isLetter) -> FieldError.HOLDER_FORMAT
            name.length < MIN_HOLDER_LENGTH -> FieldError.HOLDER_SHORT
            else -> null
        }
    }

    /**
     * @param digits up to 4 raw digits, `MMYY`
     * @param today current date; a card is valid through the last day of its expiry month
     */
    fun expiry(
        digits: String,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    ): FieldError? {
        if (digits.length < 4) return FieldError.EXPIRY_FORMAT
        val month = digits.take(2).toIntOrNull() ?: return FieldError.EXPIRY_FORMAT
        if (month !in 1..12) return FieldError.EXPIRY_MONTH
        if (!CardExpiryValidator().validate(CardFormatters.formatExpiry(digits)).isValid) return FieldError.EXPIRY_FORMAT
        val year = 2000 + (digits.takeLast(2).toIntOrNull() ?: return FieldError.EXPIRY_FORMAT)
        val expired = year < today.year || (year == today.year && month < today.month.number)
        return if (expired) FieldError.EXPIRY_PAST else null
    }

    fun cvv(
        digits: String,
        network: CardNetwork,
    ): FieldError? {
        val need = CardFormatters.cvvLength(network)
        return when {
            digits.length == need -> null
            need == 4 -> FieldError.CVV_4
            else -> FieldError.CVV_3
        }
    }

    fun email(value: String): FieldError? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> FieldError.EMAIL_EMPTY
            EMAIL.matches(trimmed) -> null
            else -> FieldError.EMAIL_INVALID
        }
    }
}
