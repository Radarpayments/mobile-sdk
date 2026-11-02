package net.payrdr.mobile.payment.sdk.core.model

/**
 * Source of token generation. Serialized as `registeredFromValue` into the payment string.
 *
 * @property registeredFromValue wire value used in the token payload
 */
public enum class MsdkRegisteredFrom(
    public val registeredFromValue: String,
) {
    MSDK_CORE("MSDK_CORE"),
    MSDK_FORMS("MSDK_FORMS"),
    MSDK_PAYMENT("MSDK_PAYMENT"),
}
