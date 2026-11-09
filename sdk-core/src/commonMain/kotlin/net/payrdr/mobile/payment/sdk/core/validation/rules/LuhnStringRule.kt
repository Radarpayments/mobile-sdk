package net.payrdr.mobile.payment.sdk.core.validation.rules

import net.payrdr.mobile.payment.sdk.core.validation.BaseValidationRule
import net.payrdr.mobile.payment.sdk.core.validation.ValidationResult
import net.payrdr.mobile.payment.sdk.core.validation.ValidationResult.Companion.VALID
import net.payrdr.mobile.payment.sdk.core.validation.ValidationResult.Companion.error

/**
 * Rule that checks the string value of the card number against the Luhn algorithm.
 *
 * @property code stable error code returned on failure
 * @property message debug message returned on failure
 */
internal class LuhnStringRule(
    private val code: String,
    private val message: String,
) : BaseValidationRule<String> {
    override fun validateForError(data: String): ValidationResult {
        val isValid =
            data
                .reversed()
                .map { char -> char.digitToIntOrNull() ?: return error(code, message) }
                .mapIndexed { index, digit ->
                    when {
                        index % 2 == 0 -> digit
                        digit < 5 -> digit * 2
                        else -> digit * 2 - 9
                    }
                }.sum() % 10 == 0
        return if (!isValid) error(code, message) else VALID
    }
}
