package net.payrdr.mobile.payment.sdk.payment.support

import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsChallenge
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsResult
import net.payrdr.mobile.payment.sdk.payment.threeds.WebChallengeHost

/**
 * Test fake that returns a preprogrammed [ThreeDsResult]. Also records every challenge
 * it was asked to perform, so tests can assert the flow asked for 3DS (or didn't).
 */
internal class FakeWebChallengeHost(
    private val result: ThreeDsResult = ThreeDsResult.Succeeded(queryParams = emptyMap()),
) : WebChallengeHost {
    val performedChallenges: MutableList<ThreeDsChallenge> = mutableListOf()

    override suspend fun performChallenge(challenge: ThreeDsChallenge): ThreeDsResult {
        performedChallenges += challenge
        return result
    }
}
