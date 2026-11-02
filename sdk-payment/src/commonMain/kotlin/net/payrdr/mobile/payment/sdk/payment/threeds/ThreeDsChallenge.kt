package net.payrdr.mobile.payment.sdk.payment.threeds

/**
 * Parameters for 3DS v1 web challenge. Consumer (or the WebView host) POSTs the four parameters
 * to [acsUrl] as `application/x-www-form-urlencoded` and waits for a redirect that [isReturnUrl] accepts.
 *
 * @property mdOrder order id (echoed back in the redirect query)
 * @property acsUrl Access Control Server endpoint
 * @property md the MD parameter, same as mdOrder
 * @property termUrl the TermUrl to embed in the form
 * @property returnUrlPrefix prefix the WebView client must recognize as success/failure redirect
 * @property timeoutMs maximum time to wait for the redirect, in milliseconds
 * @property sessionId the order's API V2 session id (`ps_…`): the gateway sends the buyer back to the session's
 * `successUrl` or `failureUrl` with `sessionId={it}` added to the query, so either page also ends the challenge
 */
public data class ThreeDsChallenge(
    val mdOrder: String,
    val acsUrl: String,
    val paReq: String,
    val md: String,
    val termUrl: String,
    val returnUrlPrefix: String = DEFAULT_RETURN_URL_PREFIX,
    val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    val sessionId: String? = null,
) {
    /**
     * Whether the bank page reaching [url] ends the challenge: it starts with [returnUrlPrefix], or it is the merchant's
     * page the gateway sends the session's buyer back to (its query names [sessionId]). Bank and gateway pages on the
     * way never name the session.
     */
    public fun isReturnUrl(url: String): Boolean {
        if (url.startsWith(returnUrlPrefix)) return true
        val session = sessionId?.takeIf { it.isNotEmpty() } ?: return false
        val query = url.substringBefore('#').substringAfter('?', missingDelimiterValue = "")
        return query.split('&').any { it == "$SESSION_ID_PARAM=$session" }
    }

    public companion object {
        public const val DEFAULT_RETURN_URL_PREFIX: String = "sdk://done"
        public const val DEFAULT_TIMEOUT_MS: Long = 5 * 60 * 1000L
        private const val SESSION_ID_PARAM = "sessionId"
    }
}
