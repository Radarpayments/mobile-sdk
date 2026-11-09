package net.payrdr.mobile.payment.sdk.core.validation

import net.payrdr.mobile.payment.sdk.core.validation.rules.RegExRule
import net.payrdr.mobile.payment.sdk.core.validation.rules.StringLengthRule
import net.payrdr.mobile.payment.sdk.core.validation.rules.StringRequiredRule

/**
 * Validator for the card security code (CVC/CVV): 3 digits, or 4 as Amex prints on the front.
 */
internal class CardCodeValidator : BaseValidator<String>() {
    init {
        addRules(
            StringRequiredRule(code = ValidationCodes.required, message = "CVC is required"),
            StringLengthRule(
                code = ValidationCodes.invalid,
                message = "CVC must be 3 or 4 digits",
                minLength = MIN_LENGTH,
                maxLength = MAX_LENGTH,
            ),
            RegExRule(code = ValidationCodes.invalid, message = "CVC must be digits only", regex = PATTERN),
        )
    }

    private companion object {
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 4
        val PATTERN = Regex("[0-9]+")
    }
}
