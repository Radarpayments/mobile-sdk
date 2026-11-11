package net.payrdr.mobile.payment.sdk.core.validation

import kotlinx.datetime.LocalDate
import net.payrdr.mobile.payment.sdk.core.validation.rules.ExpiryNotPastRule

/**
 * Validator for a well-formed `MM/YY` expiry against the payment's date. Kept apart from [CardExpiryValidator]:
 * the card form needs "wrong format" and "expired" as different messages.
 */
internal class CardExpiryNotPastValidator(
    today: LocalDate,
) : BaseValidator<String>() {
    init {
        addRules(ExpiryNotPastRule(code = ValidationCodes.invalid, message = "The card has expired", today = today))
    }
}
