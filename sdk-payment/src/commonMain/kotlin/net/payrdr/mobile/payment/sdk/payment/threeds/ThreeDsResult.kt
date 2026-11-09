package net.payrdr.mobile.payment.sdk.payment.threeds

/** Outcome of a 3DS v1 web challenge. */
public sealed interface ThreeDsResult {
    /** A redirect [ThreeDsChallenge.isReturnUrl] accepts came and the ACS reported success. */
    public data class Succeeded(
        val queryParams: Map<String, String>,
    ) : ThreeDsResult

    /** Redirect matched the prefix but contained error/declined params. */
    public data class Failed(
        val reason: String,
    ) : ThreeDsResult

    /**
     * The bank page stopped before any redirect (no network, the page did not load): whether the buyer confirmed is
     * not known, so the order status decides.
     */
    public data class Interrupted(
        val reason: String,
    ) : ThreeDsResult

    /** Neither redirect nor error within [ThreeDsChallenge.timeoutMs]. */
    public data object TimedOut : ThreeDsResult

    /** User dismissed the challenge UI. */
    public data object Cancelled : ThreeDsResult
}
