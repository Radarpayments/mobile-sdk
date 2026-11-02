package net.payrdr.mobile.payment.sdk.core.validation

import net.payrdr.mobile.payment.sdk.core.validation.rules.LuhnStringRule
import net.payrdr.mobile.payment.sdk.core.validation.rules.RegExRule
import net.payrdr.mobile.payment.sdk.core.validation.rules.StringLengthRule
import net.payrdr.mobile.payment.sdk.core.validation.rules.StringRequiredRule

/**
 * Validator for the card number (PAN).
 *
 * Rules (in order): non-blank → digits only → length 16..19 → Luhn checksum.
 */
public class CardNumberValidator : BaseValidator<String>() {
    init {
        addRules(
            StringRequiredRule(code = ValidationCodes.required, message = "Card number is required"),
            RegExRule(code = ValidationCodes.invalidFormat, message = "Card number must contain digits only", regex = PATTERN),
            StringLengthRule(
                code = ValidationCodes.invalid,
                message = "Card number length must be 16-19 digits",
                minLength = MIN_LENGTH,
                maxLength = MAX_LENGTH,
            ),
            LuhnStringRule(code = ValidationCodes.invalid, message = "Card number fails Luhn check"),
        )
    }

    private companion object {
        const val MIN_LENGTH = 16
        const val MAX_LENGTH = 19
        val PATTERN = Regex("[0-9]+")
    }
}
