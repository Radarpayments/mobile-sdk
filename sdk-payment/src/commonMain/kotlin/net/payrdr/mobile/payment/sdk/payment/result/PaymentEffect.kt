package net.payrdr.mobile.payment.sdk.payment.result

import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsChallenge

/**
 * One-shot side-effect request emitted by `PaymentFlowController`. UI layer consumes these
 * as a `Flow`. Effects are NOT replayed on state-observer reconnection.
 */
public sealed interface PaymentEffect {
    public data class ShowThreeDs(
        val challenge: ThreeDsChallenge,
    ) : PaymentEffect

    public data object Dismiss : PaymentEffect
}
