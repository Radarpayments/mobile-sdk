package net.payrdr.mobile.payment.sdk.core.validation.rules

import net.payrdr.mobile.payment.sdk.core.utils.digitsOnly
import net.payrdr.mobile.payment.sdk.core.validation.BaseValidationRule
import net.payrdr.mobile.payment.sdk.core.validation.ValidationResult
import net.payrdr.mobile.payment.sdk.core.validation.ValidationResult.Companion.VALID

/**
 * Rule that validates the numeric parts of an `MM/YY` expiry format.
 */
public class ExpiryRule(
    private val code: String,
    private val message: String,
) : BaseValidationRule<String> {
    private val monthChecker = IntRangeRule(code, message, MONTH_MIN, MONTH_MAX)
    private val yearChecker = IntRangeRule(code, message, YEAR_MIN, YEAR_MAX)

    override fun validateForError(data: String): ValidationResult {
        val digits = data.digitsOnly()
        val month = digits.take(2).toIntOrNull() ?: INVALID_FIELD_VALUE
        val year = digits.takeLast(2).toIntOrNull() ?: INVALID_FIELD_VALUE
        val monthResult = monthChecker.validateForError(month)
        val yearResult = yearChecker.validateForError(year)
        return when {
            !monthResult.isValid -> monthResult
            !yearResult.isValid -> yearResult
            else -> VALID
        }
    }

    private companion object {
        const val MONTH_MIN = 1
        const val MONTH_MAX = 12
        const val INVALID_FIELD_VALUE = -1
        const val YEAR_MIN = 0
        const val YEAR_MAX = 99
    }
}
