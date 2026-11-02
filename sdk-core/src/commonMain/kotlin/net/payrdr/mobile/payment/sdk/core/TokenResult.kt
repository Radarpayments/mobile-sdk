package net.payrdr.mobile.payment.sdk.core

import net.payrdr.mobile.payment.sdk.core.model.ParamField

/**
 * Outcome of a token-generation call.
 *
 * Exactly one of `token` or `errors` is populated.
 *
 * @property token generated token string, or `null` on failure
 * @property errors per-field error codes — empty on success
 */
public data class TokenResult private constructor(
    val token: String?,
    val errors: Map<ParamField, String>,
) {
    public val isSuccessful: Boolean get() = token != null && errors.isEmpty()

    public companion object {
        public fun withToken(token: String): TokenResult = TokenResult(token = token, errors = emptyMap())

        public fun withErrors(errors: Map<ParamField, String>): TokenResult = TokenResult(token = null, errors = errors)
    }
}
