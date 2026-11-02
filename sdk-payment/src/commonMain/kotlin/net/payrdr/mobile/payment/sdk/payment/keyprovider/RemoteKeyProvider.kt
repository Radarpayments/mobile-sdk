package net.payrdr.mobile.payment.sdk.payment.keyprovider

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import net.payrdr.mobile.payment.sdk.core.model.Key
import net.payrdr.mobile.payment.sdk.payment.keyprovider.dto.ActiveKeysResponse

/**
 * HTTP-backed [KeyProvider] that fetches `{url}` and returns the first key in the response.
 *
 * @param url remote endpoint returning `{"keys":[{"keyValue","protocolVersion","keyExpiration"}]}`
 * @param httpClient configured Ktor client (with `ContentNegotiation { json() }`)
 */
public class RemoteKeyProvider(
    private val url: String,
    private val httpClient: HttpClient,
) : KeyProvider {
    @Suppress("TooGenericExceptionCaught")
    override suspend fun provideKey(): Key {
        val response: HttpResponse =
            try {
                httpClient.get(url)
            } catch (e: Throwable) {
                throw KeyProviderException("Network failure while loading active keys", e)
            }
        if (!response.status.isSuccess()) {
            throw KeyProviderException("Active keys endpoint returned ${response.status.value}")
        }
        val parsed: ActiveKeysResponse =
            try {
                response.body()
            } catch (e: Throwable) {
                throw KeyProviderException("Malformed active keys response", e)
            }
        val first =
            parsed.keys.firstOrNull()
                ?: throw KeyProviderException("Keys for tokens are not configured on remote server")
        return Key(
            value = first.keyValue,
            protocol = first.protocolVersion,
            expiration = first.keyExpiration,
        )
    }
}
