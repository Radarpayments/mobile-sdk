package net.payrdr.mobile.payment.sdk.core.validation

import net.payrdr.mobile.payment.sdk.core.validation.rules.StringRequiredRule

/**
 * Validator for the RSA public key — only checks that it is non-blank.
 *
 * Full RSA structural validation happens implicitly at encryption time.
 */
internal class PubKeyValidator : BaseValidator<String>() {
    init {
        addRules(
            StringRequiredRule(code = ValidationCodes.required, message = "Public key is required"),
        )
    }
}
