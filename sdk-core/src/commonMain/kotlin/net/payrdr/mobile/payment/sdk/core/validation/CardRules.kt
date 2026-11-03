package net.payrdr.mobile.payment.sdk.core.validation

import net.payrdr.mobile.payment.sdk.core.InternalPayrdrApi
import net.payrdr.mobile.payment.sdk.core.validation.rules.LuhnStringRule

/** The card checks of tokenization, for the card form and the scanner to apply the same rules. */
@InternalPayrdrApi
public object CardRules {
    public fun isValidNumber(digits: String): Boolean = CardNumberValidator().validate(digits).isValid

    /** [expiry] as `MM/YY`. */
    public fun isValidExpiry(expiry: String): Boolean = CardExpiryValidator().validate(expiry).isValid

    public fun isValidHolder(name: String): Boolean = CardHolderValidator().validate(name).isValid

    public fun passesLuhn(digits: String): Boolean = LUHN.validateForError(digits).isValid

    private val LUHN = LuhnStringRule(code = ValidationCodes.invalid, message = "Card number fails Luhn check")
}
