package net.payrdr.mobile.payment.sdk.core.model

/**
 * Input for a single token-generation call.
 *
 * @property cardParams card-payment variant
 * @property timestamp unix epoch milliseconds of the payment; embedded into the token payload, and a new card's
 * expiry is checked against its date
 * @property registeredFrom source of generation (wire-value included in payload)
 */
public data class SdkCoreConfig(
    val cardParams: PaymentCardParams,
    val timestamp: Long,
    val registeredFrom: MsdkRegisteredFrom = MsdkRegisteredFrom.MSDK_CORE,
) {
    /** For Swift, which doesn't see the default of [registeredFrom]. */
    public constructor(cardParams: PaymentCardParams, timestamp: Long) :
        this(cardParams, timestamp, MsdkRegisteredFrom.MSDK_CORE)
}
