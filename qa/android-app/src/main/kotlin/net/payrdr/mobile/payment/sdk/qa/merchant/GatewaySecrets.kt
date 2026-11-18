package net.payrdr.mobile.payment.sdk.qa.merchant

import io.ktor.http.encodeURLParameter
import kotlinx.serialization.json.JsonPrimitive
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import java.net.URLEncoder

/**
 * The profile's password and API key in every spelling a gateway reply may quote them back in: an error page or a JSON
 * that echoes the request. Whatever leaves [Merchant] as text — a journal line, a failure message — goes through here.
 */
internal class GatewaySecrets private constructor(
    private val spellings: List<String>,
) {
    fun mask(text: String): String = spellings.fold(text) { masked, secret -> masked.replace(secret, MASK) }

    /**
     * Masks first and cuts afterwards: a cut through the middle of a secret would leave its first half, which no
     * longer matches anything to mask.
     */
    fun bounded(
        text: String,
        limit: Int,
        cutMarker: String = "",
    ): String {
        val masked = mask(text)
        return if (masked.length <= limit) masked else masked.take(limit) + cutMarker
    }

    override fun toString(): String = "GatewaySecrets(${spellings.size} spellings)"

    companion object {
        const val MASK = "***"

        fun of(profile: StandProfile): GatewaySecrets =
            GatewaySecrets(
                listOf(profile.password, profile.apiKey)
                    .filter { it.isNotBlank() }
                    .flatMap(::spellingsOf)
                    .distinct()
                    // A longer spelling may contain a shorter one; masking the shorter first would break the longer.
                    .sortedByDescending { it.length },
            )

        private fun spellingsOf(secret: String): List<String> =
            listOf(
                secret,
                secret.encodeURLParameter(spaceToPlus = true),
                secret.encodeURLParameter(),
                // The gateway is a Java servlet: an echo of the form may come in its own encoding.
                URLEncoder.encode(secret, Charsets.UTF_8.name()),
                JsonPrimitive(secret).toString().removeSurrounding("\""),
                secret
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;"),
            )
    }
}
