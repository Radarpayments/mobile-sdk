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

        private fun spellingsOf(secret: String): List<String> {
            val percent =
                listOf(
                    secret.encodeURLParameter(spaceToPlus = true),
                    secret.encodeURLParameter(),
                    // The gateway is a Java servlet: an echo of the form may come in its own encoding.
                    URLEncoder.encode(secret, Charsets.UTF_8.name()),
                )
            return listOf(secret) +
                percent +
                percent.map { encoded -> PERCENT_ESCAPE.replace(encoded) { it.value.lowercase() } } +
                jsonSpellings(secret) +
                secret
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;")
        }

        // Serializers differ: Gson escapes = & ' < > as \u003d and the like, PHP writes / as \/.
        private fun jsonSpellings(secret: String): List<String> {
            val plain = JsonPrimitive(secret).toString().removeSurrounding("\"")
            return listOf(plain, plain.replace("/", "\\/")).flatMap { json ->
                listOf(json, htmlSafe(json, "%04x"), htmlSafe(json, "%04X"))
            }
        }

        private fun htmlSafe(
            json: String,
            hex: String,
        ): String = HTML_UNSAFE.fold(json) { escaped, char -> escaped.replace(char.toString(), "\\u" + hex.format(char.code)) }

        private const val HTML_UNSAFE = "=&'<>"
        private val PERCENT_ESCAPE = Regex("%[0-9A-F]{2}")
    }
}
