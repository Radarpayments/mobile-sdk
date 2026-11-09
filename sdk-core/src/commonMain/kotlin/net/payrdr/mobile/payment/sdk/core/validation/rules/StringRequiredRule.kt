package net.payrdr.mobile.payment.sdk.core.validation.rules

import net.payrdr.mobile.payment.sdk.core.validation.BaseValidationRule
import net.payrdr.mobile.payment.sdk.core.validation.ValidationResult

/**
 * Rule that rejects blank / empty string values.
 */
internal class StringRequiredRule(
    private val code: String,
    private val message: String,
) : BaseValidationRule<String> {
    override fun validateForError(data: String): ValidationResult =
        if (data.isBlank()) ValidationResult.error(code, message) else ValidationResult.VALID
}
