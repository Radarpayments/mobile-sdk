package net.payrdr.mobile.payment.sdk.payment.ui.payer.phone

import net.payrdr.mobile.payment.sdk.payment.ui.util.FieldError

/** Phone number = selected country + national digits. Formatting, parsing and checks of the phone field. */
internal object PhoneNumbers {
    /** E.164 limit: 15 digits including the calling code. */
    private const val MAX_E164_DIGITS = 15

    /**
     * What the user typed, pasted or autofilled into the national part, applied to [country].
     * An international number (`+44 7700…`, `0044 7700…`) switches the country; a leading national
     * prefix (`07700…` in the UK) is dropped; the result is capped at the longest mobile number.
     *
     * @return the country to use (may be the detected one) and the national digits
     */
    fun input(
        text: String,
        country: PhoneCountry?,
    ): Pair<PhoneCountry?, String> {
        val trimmed = text.trim()
        val digits = trimmed.filter(Char::isDigit)
        val international =
            when {
                trimmed.startsWith("+") -> digits
                digits.startsWith("00") && digits.length > 4 -> digits.drop(2)
                else -> null
            }
        if (international != null) {
            PhoneCountries.detect(international, preferred = country?.iso)?.let { (detected, rest) ->
                return detected to national(rest, detected)
            }
        }
        return country to national(digits, country)
    }

    /** National digits for [country]: no trunk prefix, no more than the longest possible number. */
    fun national(
        digits: String,
        country: PhoneCountry?,
    ): String {
        if (country == null) return digits.take(MAX_E164_DIGITS)
        val prefix = country.nationalPrefix
        // Strip the trunk prefix only where real numbers never start with it (UK 0, France 0 …).
        val withoutPrefix =
            if (prefix.isNotEmpty() && digits.startsWith(prefix) && !country.example.startsWith(prefix)) {
                digits.drop(prefix.length)
            } else {
                digits
            }
        return withoutPrefix.take(country.lengths.max())
    }

    /** `7700900123` → `7700 900123` following the country's grouping; extra digits stay in the last group. */
    fun format(
        national: String,
        country: PhoneCountry?,
    ): String {
        if (country == null || national.isEmpty()) return national
        val parts = mutableListOf<String>()
        var rest = national
        for ((index, size) in country.groups.withIndex()) {
            if (rest.isEmpty()) break
            val take = if (index == country.groups.lastIndex) rest.length else size
            parts += rest.take(take)
            rest = rest.drop(take)
        }
        return parts.joinToString(" ")
    }

    /** Placeholder: the country's example mobile number, formatted. */
    fun placeholder(country: PhoneCountry?): String = country?.let { format(it.example, it) }.orEmpty()

    /** `+447700900123`, or the bare digits when no country is chosen yet (fails validation). */
    fun e164(
        country: PhoneCountry?,
        national: String,
    ): String =
        when {
            national.isEmpty() -> ""
            country == null -> national
            else -> "+${country.dialCode}$national"
        }

    /** Splits a stored number (`+447700900123`) into country and national digits. */
    fun parse(
        value: String,
        preferred: String? = null,
    ): Pair<PhoneCountry, String>? {
        val trimmed = value.trim()
        val digits = trimmed.filter(Char::isDigit)
        val international = if (trimmed.startsWith("+")) digits else digits.removePrefix("00").takeIf { digits.startsWith("00") }
        return international?.let { PhoneCountries.detect(it, preferred) }?.let { (country, rest) -> country to national(rest, country) }
    }

    /** Error of a non-empty E.164 [value]: no country, or a length the country's mobile numbers never have. */
    fun validate(value: String): FieldError? {
        if (!value.trim().startsWith("+")) return FieldError.PHONE_NO_CODE
        val (country, national) = PhoneCountries.detect(value.filter(Char::isDigit)) ?: return FieldError.PHONE_NO_CODE
        return when {
            national.length > country.lengths.max() -> FieldError.PHONE_LONG
            national.length !in country.lengths -> FieldError.PHONE_SHORT
            else -> null
        }
    }
}
