package net.payrdr.mobile.payment.sdk.core.validation

import net.payrdr.mobile.payment.sdk.core.validation.rules.RegExRule
import net.payrdr.mobile.payment.sdk.core.validation.rules.StringLengthRule
import net.payrdr.mobile.payment.sdk.core.validation.rules.StringRequiredRule

/**
 * Validator for the `mdOrder` value — non-blank, no whitespace, min length 1.
 */
internal class OrderNumberValidator : BaseValidator<String>() {
    init {
        addRules(
            StringRequiredRule(code = ValidationCodes.required, message = "Order number is required"),
            StringLengthRule(code = ValidationCodes.invalid, message = "Order number must be at least 1 character", minLength = MIN_LENGTH),
            RegExRule(code = ValidationCodes.invalid, message = "Order number must not contain whitespace", regex = PATTERN),
        )
    }

    private companion object {
        const val MIN_LENGTH = 1
        val PATTERN = Regex("\\S+")
    }
}
