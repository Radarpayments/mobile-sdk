package net.payrdr.mobile.payment.sdk.core.validation.rules

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import net.payrdr.mobile.payment.sdk.core.utils.toExpDate
import net.payrdr.mobile.payment.sdk.core.validation.BaseValidationRule
import net.payrdr.mobile.payment.sdk.core.validation.ValidationResult
import net.payrdr.mobile.payment.sdk.core.validation.ValidationResult.Companion.VALID

/**
 * Rule that fails an `MM/YY` expiry whose month is before [today]'s; a card is valid through the last day of its
 * expiry month. Runs only on a value that passed [ExpiryRule].
 */
internal class ExpiryNotPastRule(
    private val code: String,
    private val message: String,
    private val today: LocalDate,
) : BaseValidationRule<String> {
    override fun validateForError(data: String): ValidationResult {
        val expiry = data.toExpDate()
        val past = expiry.expYear < today.year || (expiry.expYear == today.year && expiry.expMonth < today.month.number)
        return if (past) ValidationResult.error(code, message) else VALID
    }
}
