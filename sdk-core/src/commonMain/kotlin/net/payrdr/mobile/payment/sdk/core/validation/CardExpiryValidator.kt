package net.payrdr.mobile.payment.sdk.core.validation

import net.payrdr.mobile.payment.sdk.core.validation.rules.ExpiryRule
import net.payrdr.mobile.payment.sdk.core.validation.rules.RegExRule
import net.payrdr.mobile.payment.sdk.core.validation.rules.StringRequiredRule

/**
 * Validator for the card expiry in `MM/YY` format.
 */
public class CardExpiryValidator : BaseValidator<String>() {
    init {
        addRules(
            StringRequiredRule(code = ValidationCodes.required, message = "Expiry is required"),
            RegExRule(code = ValidationCodes.invalidFormat, message = "Expiry must match MM/YY", regex = PATTERN),
            ExpiryRule(code = ValidationCodes.invalid, message = "Expiry numeric parts are invalid"),
        )
    }

    private companion object {
        val PATTERN = Regex("\\d{2}/\\d{2}")
    }
}
