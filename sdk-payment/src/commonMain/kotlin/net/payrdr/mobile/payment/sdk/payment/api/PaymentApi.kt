package net.payrdr.mobile.payment.sdk.payment.api

import net.payrdr.mobile.payment.sdk.payment.api.dto.request.ApplePayProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.GooglePayProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.P2PPerformRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.ProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ApplePayProcessFormResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.FinishedPaymentInfoResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.PaymentSettings
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ProcessFormGPayResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ProcessFormResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.SessionStatusResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.UnbindCardResponse

/** Payment gateway API. All methods throw [net.payrdr.mobile.payment.sdk.payment.exception.SdkPaymentApiException] on failure.
 *
 * Implemented in Swift: SKIE names a `suspend` function of a Kotlin interface with a `__` prefix and a completion
 * handler there (e.g. `__performChallenge(challenge:completionHandler:)`).
 */
@Suppress("TooManyFunctions")
internal interface PaymentApi {
    /** `POST /rest/getSessionStatus.do` — returns session/order snapshot. */
    suspend fun getSessionStatus(mdOrder: String): SessionStatusResponse

    /** `POST /rest/processform.do` — submit a new-card cryptogram. */
    suspend fun processForm(request: ProcessFormRequest): ProcessFormResponse

    /** `POST /rest/processBindingForm.do` — submit a saved-card cryptogram. */
    suspend fun processBindingForm(request: ProcessFormRequest): ProcessFormResponse

    /** `POST /google/paymentOrder.do` — submit a Google Pay token. */
    suspend fun googlePayProcessForm(request: GooglePayProcessFormRequest): ProcessFormGPayResponse

    /** `POST /applepay/paymentOrder.do` — pay the registered order with an Apple Pay token. */
    suspend fun applePayProcessForm(request: ApplePayProcessFormRequest): ApplePayProcessFormResponse

    /** `POST /rest/getFinishedPaymentInfo.do` — polling endpoint for finalized status. */
    suspend fun getFinishedPaymentInfo(orderId: String): FinishedPaymentInfoResponse

    /**
     * `GET /rest/getPaymentSettings.do?login={login}` — the merchant's checkout settings (Google Pay among them);
     * [login] is the one the order's status names. A merchant without settings gets an empty set.
     */
    suspend fun getPaymentSettings(login: String): PaymentSettings

    /** `POST /rest/unbindcardanon.do` — detach a saved binding. */
    suspend fun unbindCardAnonymous(
        bindingId: String,
        mdOrder: String,
    ): UnbindCardResponse

    /**
     * `POST /rest/p2p/perform.do` — a one-leg AFT transfer of the order from a new card. Answers like [processForm]: a bank
     * page to open, a refusal, or neither (the order status decides).
     */
    suspend fun p2pPerform(request: P2PPerformRequest): ProcessFormResponse

    /** `POST /rest/p2p/performByBinding.do` — a one-leg AFT transfer of the order from a saved (debit) card. */
    suspend fun p2pPerformByBinding(request: P2PPerformRequest): ProcessFormResponse
}
