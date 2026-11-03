package net.payrdr.mobile.payment.sdk.payment.support

import kotlinx.coroutines.CompletableDeferred
import net.payrdr.mobile.payment.sdk.payment.api.PaymentApi
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.ApplePayProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.GooglePayProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.P2PPerformRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.ProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ApplePayProcessFormResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.FinishedPaymentInfoResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.MerchantCountry
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.MerchantInfo
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.PaymentSettings
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ProcessFormGPayResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ProcessFormResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.SessionStatusResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.UnbindCardResponse

/**
 * Programmable fake API. Each method returns whatever the caller sets in the public `nextXxx`
 * field, or the default baseline. Records all calls.
 */
internal class FakePaymentApi : PaymentApi {
    var nextSessionStatus: SessionStatusResponse =
        SessionStatusResponse(
            remainingSecs = 600L,
            amount = "4990",
            currencyAlphaCode = "EUR",
            merchantInfo =
                MerchantInfo(
                    merchantLogin = "m-login",
                    merchantFullName = "Test Merchant",
                    merchantCountry = MerchantCountry(countryCode = "276", countryCodeAlpha2 = "DE"),
                ),
            merchantOptions = listOf("CARD", "GOOGLEPAY", "APPLEPAY"),
        )
    var nextProcessFormResponse: ProcessFormResponse = ProcessFormResponse(errorCode = 0)
    var nextProcessBindingFormResponse: ProcessFormResponse = ProcessFormResponse(errorCode = 0)
    var nextGooglePayProcessFormResponse: ProcessFormGPayResponse = ProcessFormGPayResponse(success = true, data = null)
    var nextApplePayProcessFormResponse: ApplePayProcessFormResponse = ApplePayProcessFormResponse(success = true, data = null)
    var nextFinishedPaymentInfo: FinishedPaymentInfoResponse = FinishedPaymentInfoResponse(status = "APPROVED")
    var nextPaymentSettings: PaymentSettings =
        PaymentSettings(mapOf("googlePay.environment" to "TEST", "googlePay.merchantId" to "M-1", "googlePay.gateway" to "bpcpay"))

    /** Thrown by `getPaymentSettings` instead of answering, when set. */
    var paymentSettingsError: Throwable? = null

    /** `getPaymentSettings` answers only after this long, when set (virtual time). */
    var paymentSettingsDelayMs: Long = 0
    var nextUnbindCardResponse: UnbindCardResponse = UnbindCardResponse(errorCode = 0)

    /** Thrown by `getSessionStatus` instead of answering, when set. */
    var sessionStatusError: Throwable? = null

    /** Thrown by `processForm` instead of answering, when set. */
    var processFormError: Throwable? = null

    /** `processForm` waits for this before answering, when set. */
    var processFormGate: CompletableDeferred<Unit>? = null

    /** Thrown by `processBindingForm` instead of answering, when set. */
    var processBindingFormError: Throwable? = null

    /** Answers (or failures) for successive `getFinishedPaymentInfo` calls; once empty, [nextFinishedPaymentInfo]. */
    val finishedPaymentInfoQueue: ArrayDeque<Result<FinishedPaymentInfoResponse>> = ArrayDeque()

    /** Every `getFinishedPaymentInfo` call fails with this, when set (after the queue). */
    var finishedPaymentInfoError: Throwable? = null

    val getSessionStatusCalls: MutableList<String> = mutableListOf()
    val processFormCalls: MutableList<ProcessFormRequest> = mutableListOf()
    val processBindingFormCalls: MutableList<ProcessFormRequest> = mutableListOf()
    val googlePayCalls: MutableList<GooglePayProcessFormRequest> = mutableListOf()
    val applePayCalls: MutableList<ApplePayProcessFormRequest> = mutableListOf()
    val finish3dsCalls: MutableList<String> = mutableListOf()
    val getFinishedPaymentInfoCalls: MutableList<String> = mutableListOf()
    val getPaymentSettingsCalls: MutableList<String> = mutableListOf()
    val unbindCardCalls: MutableList<Pair<String, String>> = mutableListOf()
    var nextP2PPerformResponse: ProcessFormResponse = ProcessFormResponse(errorCode = 0)
    val p2pPerformCalls: MutableList<P2PPerformRequest> = mutableListOf()
    val p2pPerformByBindingCalls: MutableList<P2PPerformRequest> = mutableListOf()

    override suspend fun getSessionStatus(mdOrder: String): SessionStatusResponse {
        getSessionStatusCalls += mdOrder
        sessionStatusError?.let { throw it }
        return nextSessionStatus
    }

    override suspend fun processForm(request: ProcessFormRequest): ProcessFormResponse {
        processFormCalls += request
        processFormGate?.await()
        processFormError?.let { throw it }
        return nextProcessFormResponse
    }

    override suspend fun processBindingForm(request: ProcessFormRequest): ProcessFormResponse {
        processBindingFormCalls += request
        processBindingFormError?.let { throw it }
        return nextProcessBindingFormResponse
    }

    override suspend fun googlePayProcessForm(request: GooglePayProcessFormRequest): ProcessFormGPayResponse {
        googlePayCalls += request
        return nextGooglePayProcessFormResponse
    }

    override suspend fun applePayProcessForm(request: ApplePayProcessFormRequest): ApplePayProcessFormResponse {
        applePayCalls += request
        return nextApplePayProcessFormResponse
    }

    override suspend fun finish3dsVer2PaymentAnonymous(threeDsServerTransId: String) {
        finish3dsCalls += threeDsServerTransId
    }

    override suspend fun getFinishedPaymentInfo(orderId: String): FinishedPaymentInfoResponse {
        getFinishedPaymentInfoCalls += orderId
        finishedPaymentInfoQueue.removeFirstOrNull()?.let { return it.getOrThrow() }
        finishedPaymentInfoError?.let { throw it }
        return nextFinishedPaymentInfo
    }

    override suspend fun getPaymentSettings(login: String): PaymentSettings {
        getPaymentSettingsCalls += login
        if (paymentSettingsDelayMs > 0) kotlinx.coroutines.delay(paymentSettingsDelayMs)
        paymentSettingsError?.let { throw it }
        return nextPaymentSettings
    }

    override suspend fun unbindCardAnonymous(
        bindingId: String,
        mdOrder: String,
    ): UnbindCardResponse {
        unbindCardCalls += (bindingId to mdOrder)
        return nextUnbindCardResponse
    }

    override suspend fun p2pPerform(request: P2PPerformRequest): ProcessFormResponse {
        p2pPerformCalls += request
        return nextP2PPerformResponse
    }

    override suspend fun p2pPerformByBinding(request: P2PPerformRequest): ProcessFormResponse {
        p2pPerformByBindingCalls += request
        return nextP2PPerformResponse
    }
}
