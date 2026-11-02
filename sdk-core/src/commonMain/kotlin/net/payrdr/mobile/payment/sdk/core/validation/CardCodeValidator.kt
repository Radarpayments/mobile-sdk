package net.payrdr.mobile.payment.sdk.core.validation

import net.payrdr.mobile.payment.sdk.core.validation.rules.RegExRule
import net.payrdr.mobile.payment.sdk.core.validation.rules.StringLengthRule
import net.payrdr.mobile.payment.sdk.core.validation.rules.StringRequiredRule

/**
 * Validator for the card security code (CVC/CVV) — exactly 3 digits.
 */
public class CardCodeValidator : BaseValidator<String>() {
    init {
        addRules(
            StringRequiredRule(code = ValidationCodes.required, message = "CVC is required"),
            StringLengthRule(code = ValidationCodes.invalid, message = "CVC must be 3 digits", minLength = LENGTH, maxLength = LENGTH),
            RegExRule(code = ValidationCodes.invalid, message = "CVC must be digits only", regex = PATTERN),
        )
    }

    private companion object {
        const val LENGTH = 3
        val PATTERN = Regex("[0-9]+")
    }
}
