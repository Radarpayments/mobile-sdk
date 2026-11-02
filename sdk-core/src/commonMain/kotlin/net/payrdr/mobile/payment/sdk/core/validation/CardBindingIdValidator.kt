package net.payrdr.mobile.payment.sdk.core.validation

import net.payrdr.mobile.payment.sdk.core.validation.rules.RegExRule
import net.payrdr.mobile.payment.sdk.core.validation.rules.StringRequiredRule

/**
 * Validator for a binding id — non-blank, no whitespace.
 */
public class CardBindingIdValidator : BaseValidator<String>() {
    init {
        addRules(
            StringRequiredRule(code = ValidationCodes.required, message = "Binding id is required"),
            RegExRule(code = ValidationCodes.invalid, message = "Binding id must not contain whitespace", regex = PATTERN),
        )
    }

    private companion object {
        val PATTERN = Regex("\\S+")
    }
}
