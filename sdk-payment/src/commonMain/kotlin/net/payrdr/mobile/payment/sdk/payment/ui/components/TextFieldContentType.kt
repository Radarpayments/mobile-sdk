package net.payrdr.mobile.payment.sdk.payment.ui.components

/**
 * Platform-hinted content type for [NativeTextField]. Maps to the autofill `ContentType` on Android and
 * `UITextContentType` on iOS so that Keychain / Google Autofill / card scanners can populate
 * the field automatically. It also picks the keyboard, tabular digits and CVV masking.
 */
internal enum class TextFieldContentType {
    CREDIT_CARD_NUMBER,
    CREDIT_CARD_EXPIRATION,
    CREDIT_CARD_SECURITY_CODE,
    CREDIT_CARD_HOLDER,
    EMAIL,
    PHONE,

    /** Street address line; Latin keyboard. */
    ADDRESS_LINE,

    /** City; Latin keyboard. */
    CITY,

    /** State / region; Latin keyboard. */
    STATE,

    /** Postal / ZIP code; Latin keyboard. */
    POSTAL_CODE,
    NONE,
}

/**
 * Typed in Latin script (card data, email, phone, address): the text runs left to right even in an Arabic layout —
 * otherwise the groups of a card or phone number come out in reverse order — and still sits at the layout's start.
 */
internal val TextFieldContentType.isLatin: Boolean
    get() = this != TextFieldContentType.STATE && this != TextFieldContentType.NONE

/** Digits-only content: rendered with tabular figures and a number pad. */
internal val TextFieldContentType.isNumeric: Boolean
    get() =
        this == TextFieldContentType.CREDIT_CARD_NUMBER ||
            this == TextFieldContentType.CREDIT_CARD_EXPIRATION ||
            this == TextFieldContentType.CREDIT_CARD_SECURITY_CODE
