package net.payrdr.mobile.payment.sdk.core.validation.rules

import net.payrdr.mobile.payment.sdk.core.validation.BaseValidationRule
import net.payrdr.mobile.payment.sdk.core.validation.ValidationResult
import net.payrdr.mobile.payment.sdk.core.validation.ValidationResult.Companion.VALID

/**
 * Rule to check a numeric value is within the inclusive range [min]..[max].
 */
public class IntRangeRule(
    private val code: String,
    private val message: String,
    private val min: Int = 0,
    private val max: Int = Int.MAX_VALUE,
) : BaseValidationRule<Int> {
    override fun validateForError(data: Int): ValidationResult =
        if (data < min || data > max) ValidationResult.error(code, message) else VALID
}
