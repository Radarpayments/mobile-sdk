package net.payrdr.mobile.payment.sdk.core.model

/**
 * Possible error locations — identifies which field produced a validation error.
 *
 * Stable enum; serialized as ordinal-independent `name`.
 */
public enum class ParamField {
    UNKNOWN,
    PAN,
    CVC,
    EXPIRY,
    CARDHOLDER,
    BINDING_ID,
    MD_ORDER,
    PUB_KEY,
    STORED_PAYMENT_ID,
}
