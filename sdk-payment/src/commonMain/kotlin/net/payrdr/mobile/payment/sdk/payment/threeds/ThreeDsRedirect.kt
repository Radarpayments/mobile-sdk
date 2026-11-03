package net.payrdr.mobile.payment.sdk.payment.threeds

/** The outcome a return redirect from the bank's page carries, read the same way on both platforms. */
internal object ThreeDsRedirect {
    /**
     * [query] of the redirect [ThreeDsChallenge.isReturnUrl] accepted: by the ACS convention an `error` parameter (any
     * case) is a refusal with its value as the reason, anything else a success.
     */
    fun result(query: List<Pair<String, String>>): ThreeDsResult {
        val error = query.firstOrNull { (name, _) -> name.equals("error", ignoreCase = true) }
        return if (error != null) {
            ThreeDsResult.Failed(error.second.ifEmpty { "unknown" })
        } else {
            ThreeDsResult.Succeeded(query.toMap())
        }
    }
}
