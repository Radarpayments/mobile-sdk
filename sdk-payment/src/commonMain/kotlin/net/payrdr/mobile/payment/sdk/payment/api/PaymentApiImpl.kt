package net.payrdr.mobile.payment.sdk.payment.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.ApplePayProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.GooglePayProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.P2PFromCard
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.P2PPerformRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.ProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ApplePayProcessFormResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.FinishedPaymentInfoResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.P2PPerformResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.PaymentSettings
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ProcessFormGPayResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ProcessFormResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.SessionStatusResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.UnbindCardResponse
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNetworkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkPaymentApiException

/**
 * Ktor-backed [PaymentApi]. Accepts a pre-configured client (caller installs `ContentNegotiation`
 * with a lenient `Json` configuration).
 */
@Suppress("TooGenericExceptionCaught", "TooManyFunctions")
public class PaymentApiImpl(
    private val baseUrl: String,
    private val httpClient: HttpClient,
) : PaymentApi {
    override suspend fun getSessionStatus(mdOrder: String): SessionStatusResponse =
        runApi {
            val params = Parameters.build { append("MDORDER", mdOrder) }
            httpClient
                .post("$baseUrl${Endpoints.GET_SESSION_STATUS}") {
                    setBody(FormDataContent(params))
                }.asBody()
        }

    override suspend fun processForm(request: ProcessFormRequest): ProcessFormResponse =
        runApi {
            val params =
                Parameters.build {
                    append("seToken", request.paymentToken)
                    append("MDORDER", request.mdOrder)
                    append("TEXT", request.holder)
                    append("bindingNotNeeded", (!request.saveCard).toString())
                    append("threeDSSDK", "false")
                    PayerDataFormParams.build(request.payerData).forEach { (name, value) -> append(name, value) }
                }
            httpClient
                .post("$baseUrl${Endpoints.PROCESS_FORM}") {
                    setBody(FormDataContent(params))
                }.asBody()
        }

    override suspend fun processBindingForm(request: ProcessFormRequest): ProcessFormResponse =
        runApi {
            val params =
                Parameters.build {
                    append("seToken", request.paymentToken)
                    append("MDORDER", request.mdOrder)
                    append("TEXT", request.holder)
                    append("threeDSSDK", "false")
                    PayerDataFormParams.build(request.payerData).forEach { (name, value) -> append(name, value) }
                }
            httpClient
                .post("$baseUrl${Endpoints.PROCESS_BINDING_FORM}") {
                    setBody(FormDataContent(params))
                }.asBody()
        }

    override suspend fun googlePayProcessForm(request: GooglePayProcessFormRequest): ProcessFormGPayResponse =
        runApi {
            httpClient
                .post("$baseUrl${Endpoints.GOOGLE_PAY_PROCESS_FORM}") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.asBody()
        }

    override suspend fun applePayProcessForm(request: ApplePayProcessFormRequest): ApplePayProcessFormResponse =
        runApi {
            httpClient
                .post("$baseUrl${Endpoints.APPLE_PAY_PROCESS_FORM}") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.asBody()
        }

    override suspend fun finish3dsVer2PaymentAnonymous(threeDsServerTransId: String): Unit =
        runApi {
            val params = Parameters.build { append("threeDSServerTransId", threeDsServerTransId) }
            val response =
                httpClient.post("$baseUrl${Endpoints.FINISH_3DS_VER2_PAYMENT}") {
                    setBody(FormDataContent(params))
                }
            assertSuccess(response)
        }

    override suspend fun getFinishedPaymentInfo(orderId: String): FinishedPaymentInfoResponse =
        runApi {
            val params = Parameters.build { append("orderId", orderId) }
            httpClient
                .post("$baseUrl${Endpoints.GET_FINISHED_PAYMENT_INFO}") {
                    setBody(FormDataContent(params))
                }.asBody()
        }

    override suspend fun getPaymentSettings(login: String): PaymentSettings =
        runApi {
            val response =
                httpClient.get("$baseUrl${Endpoints.GET_PAYMENT_SETTINGS}") {
                    parameter("login", login)
                }
            assertSuccess(response)
            // `{"settings": {"name": "value", …}}`; a merchant without settings gets `{"error": "Data not found"}`.
            val body: JsonObject = response.body()
            val settings = body["settings"] as? JsonObject
            PaymentSettings(
                settings
                    ?.mapNotNull { (name, value) -> (value as? JsonPrimitive)?.takeIf { it.isString }?.let { name to it.content } }
                    ?.toMap()
                    .orEmpty(),
            )
        }

    override suspend fun unbindCardAnonymous(
        bindingId: String,
        mdOrder: String,
    ): UnbindCardResponse =
        runApi {
            val params =
                Parameters.build {
                    append("mdOrder", mdOrder)
                    append("bindingId", bindingId)
                }
            httpClient
                .post("$baseUrl${Endpoints.UNBIND_CARD_ANON}") {
                    setBody(FormDataContent(params))
                }.asBody()
        }

    override suspend fun p2pPerform(request: P2PPerformRequest): ProcessFormResponse = p2p(Endpoints.P2P_PERFORM, request)

    override suspend fun p2pPerformByBinding(request: P2PPerformRequest): ProcessFormResponse =
        p2p(Endpoints.P2P_PERFORM_BY_BINDING, request)

    private suspend fun p2p(
        path: String,
        request: P2PPerformRequest,
    ): ProcessFormResponse =
        runApi {
            val body =
                buildJsonObject {
                    put("orderId", JsonPrimitive(request.mdOrder))
                    put(
                        "fromCard",
                        when (val card = request.fromCard) {
                            is P2PFromCard.SeToken -> buildJsonObject { put("seToken", JsonPrimitive(card.seToken)) }
                            is P2PFromCard.Binding -> buildJsonObject { put("bindingId", JsonPrimitive(card.bindingId)) }
                            is P2PFromCard.Wallet ->
                                buildJsonObject {
                                    put("vendor", JsonPrimitive(card.vendor.name))
                                    put("paymentToken", JsonPrimitive(card.paymentToken))
                                }
                        },
                    )
                    put("bindingNotNeeded", JsonPrimitive(request.bindingNotNeeded))
                    PayerDataFormParams.transfer(request.payerData).forEach { (key, value) -> put(key, value) }
                    request.language?.let { put("language", JsonPrimitive(it)) }
                }
            val response: P2PPerformResponse =
                httpClient
                    .post("$baseUrl$path") {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }.asBody()
            ProcessFormResponse(
                errorCode = response.errorCode,
                acsUrl = response.acsUrl,
                paReq = response.paReq,
                termUrl = response.termUrl,
                threeDSMethodURL = response.threeDSMethodURL,
                // A transfer paid without the bank page answers `0` with "Successful": only a non-zero code refuses one.
                error = if (response.errorCode != 0) response.errorMessage ?: response.error else null,
            )
        }

    // ── helpers ───────────────────────────────────────────────────────────────

    private suspend inline fun <reified T> HttpResponse.asBody(): T {
        assertSuccess(this)
        return body()
    }

    private fun assertSuccess(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            throw SdkPaymentApiException(
                httpStatus = response.status.value,
                message = "API call failed with HTTP ${response.status.value}",
            )
        }
    }

    /**
     * The gateway answered but the answer is unusable ([SdkPaymentApiException]: an HTTP error or a body that does not
     * parse), or no answer came at all ([SdkNetworkException]: unreachable, dropped, timed out). The difference matters
     * after a payment is sent: without an answer the payment may still have gone through.
     */
    private suspend inline fun <T> runApi(block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SdkPaymentApiException) {
            throw e
        } catch (e: SerializationException) {
            throw SdkPaymentApiException(message = e.message, cause = e)
        } catch (e: ContentConvertException) {
            throw SdkPaymentApiException(message = e.message, cause = e)
        } catch (e: Throwable) {
            throw SdkNetworkException(message = e.message, cause = e)
        }
}
