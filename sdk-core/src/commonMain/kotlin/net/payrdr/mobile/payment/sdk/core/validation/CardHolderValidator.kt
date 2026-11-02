package net.payrdr.mobile.payment.sdk.core.validation

import net.payrdr.mobile.payment.sdk.core.validation.rules.RegExRule
import net.payrdr.mobile.payment.sdk.core.validation.rules.StringLengthRule
import net.payrdr.mobile.payment.sdk.core.validation.rules.StringRequiredRule

/**
 * Validator for the printed cardholder name — ASCII letters + spaces, max 30 chars.
 */
public class CardHolderValidator : BaseValidator<String>() {
    init {
        addRules(
            StringRequiredRule(code = ValidationCodes.required, message = "Card holder is required"),
            StringLengthRule(code = ValidationCodes.invalid, message = "Card holder must be at most 30 characters", maxLength = MAX_LENGTH),
            RegExRule(code = ValidationCodes.invalidFormat, message = "Card holder allows ASCII letters and spaces only", regex = PATTERN),
        )
    }

    private companion object {
        const val MAX_LENGTH = 30
        val PATTERN = Regex("[a-zA-Z ]+")
    }
}
