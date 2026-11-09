package net.payrdr.mobile.payment.sdk.core.validation.rules

import net.payrdr.mobile.payment.sdk.core.validation.BaseValidationRule
import net.payrdr.mobile.payment.sdk.core.validation.ValidationResult
import net.payrdr.mobile.payment.sdk.core.validation.ValidationResult.Companion.VALID
import net.payrdr.mobile.payment.sdk.core.validation.ValidationResult.Companion.error

/**
 * Rule for checking a string length range.
 */
internal class StringLengthRule(
    private val code: String,
    private val message: String,
    private val minLength: Int = 0,
    private val maxLength: Int = Int.MAX_VALUE,
) : BaseValidationRule<String> {
    override fun validateForError(data: String): ValidationResult =
        if (data.length < minLength || data.length > maxLength) error(code, message) else VALID
}
