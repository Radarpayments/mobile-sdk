package net.payrdr.mobile.payment.sdk.payment.threeds

/**
 * Platform contract for presenting the 3DS v1 web challenge. Implementations run the form-POST
 * to the ACS URL and listen for a redirect [ThreeDsChallenge.isReturnUrl] accepts (Android: WebView +
 * `shouldOverrideUrlLoading`; iOS: WKWebView + `decidePolicyFor`).
 *
 * Provided as a regular interface (not `expect class`) so unit tests can supply a fake.
 *
 * Implemented in Swift: SKIE names a `suspend` function of a Kotlin interface with a `__` prefix and a completion
 * handler there (e.g. `__performChallenge(challenge:completionHandler:)`).
 */
public interface WebChallengeHost {
    /** Runs the challenge and suspends until the FSM resolves. Never throws — errors come back via [ThreeDsResult]. */
    public suspend fun performChallenge(challenge: ThreeDsChallenge): ThreeDsResult
}
