package net.payrdr.mobile.payment.sdk.payment.flow

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import net.payrdr.mobile.payment.sdk.payment.api.PaymentApi
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.ApplePayProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.GooglePayProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.WalletVendor
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.GPayData
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.PaymentSettings
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ProcessFormResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.SessionStatusResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.WalletError
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayTokenResult
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.exception.SdkApplePayException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkGooglePayException
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayTokenResult
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val TAG = "PaymentFlowController"

/** How long the card form waits for the merchant's payment settings (wallet parameters) before showing without them. */
private const val SETTINGS_TIMEOUT_MS = 3_000L

/** The wallet's name, as the buyer and the logs know it. */
internal val WalletVendor.title: String
    get() =
        when (this) {
            WalletVendor.GOOGLE -> "Google Pay"
            WalletVendor.APPLE -> "Apple Pay"
        }

/**
 * The wallet side of a checkout: which wallets an order offers, the buyer's payment token from the wallet's sheet,
 * and the payment through the gateway's wallet endpoint.
 *
 * [googlePay] and [applePay] are the merchant's wallet configs, completed from the gateway's settings and the order.
 */
@Suppress("LongParameterList")
internal class WalletPayments(
    private val api: PaymentApi,
    private val logger: Logger,
    private val googlePay: GooglePayConfig?,
    private val applePay: ApplePayConfig?,
    private val googlePayTokenProvider: GooglePayTokenProvider,
    private val applePayTokenProvider: ApplePayTokenProvider,
) {
    /** The two wallets [session] offers: each null when the gateway has not enabled it or a value it needs is missing. */
    suspend fun offered(session: SessionStatusResponse): Pair<GooglePayConfig?, ApplePayConfig?> {
        val settings = settings(session)
        return googlePay?.let { WalletResolver.googlePay(it, session, settings) } to
            applePay?.let { WalletResolver.applePay(it, session, settings) }
    }

    /**
     * The buyer's payment token from [vendor]'s sheet for the wallet [form] offers, as the gateway takes it; null when
     * the buyer closed the sheet.
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun token(
        vendor: WalletVendor,
        form: PaymentState.CardFormShown,
        session: SessionStatusResponse,
    ): String? {
        // Known: the resolver offers a wallet only when the order's amount can be read.
        val amountMinor = checkNotNull(WalletResolver.amountMinor(session)) { "Wallet offered for an amount it cannot read" }
        // Known too: it offers a wallet only with the order's currency and a country.
        val currency = session.currencyAlphaCode.orEmpty()
        return when (vendor) {
            WalletVendor.GOOGLE -> {
                val config = checkNotNull(form.googlePay)
                val result =
                    googlePayTokenProvider.requestPaymentToken(
                        config = config,
                        amountMinorUnits = amountMinor,
                        currencyCode = currency,
                        countryCode = config.countryCode.orEmpty(),
                    )
                when (result) {
                    // The gateway reads Google's token as base64, as its own payment page sends it.
                    is GooglePayTokenResult.Success -> Base64.encode(result.paymentToken.encodeToByteArray())
                    GooglePayTokenResult.Cancelled -> null
                    GooglePayTokenResult.NotAvailable -> throw SdkGooglePayException("Google Pay is not available on this device")
                    is GooglePayTokenResult.Failure -> throw SdkGooglePayException(result.reason, result.cause)
                }
            }
            WalletVendor.APPLE -> {
                val config = checkNotNull(form.applePay)
                val result =
                    applePayTokenProvider.requestPaymentToken(
                        config = config,
                        amountMinorUnits = amountMinor,
                        currencyCode = currency,
                        countryCode = config.countryCode.orEmpty(),
                        merchantDisplayName = session.merchantInfo.merchantFullName.orEmpty(),
                    )
                when (result) {
                    is ApplePayTokenResult.Success -> result.paymentToken
                    ApplePayTokenResult.Cancelled -> null
                    ApplePayTokenResult.NotAvailable -> throw SdkApplePayException("Apple Pay is not available on this device")
                    is ApplePayTokenResult.Failure -> throw SdkApplePayException(result.reason, result.cause)
                }
            }
        }
    }

    /** Pays [mdOrder] with a wallet [token]; the answer comes in the card form's shape, for the shared 3DS and refusal handling. */
    suspend fun pay(
        vendor: WalletVendor,
        token: String,
        mdOrder: String,
    ): ProcessFormResponse =
        when (vendor) {
            WalletVendor.GOOGLE ->
                api.googlePayProcessForm(GooglePayProcessFormRequest(paymentToken = token, mdOrder = mdOrder)).let {
                    answer(it.success, it.data, it.error)
                }
            WalletVendor.APPLE ->
                api.applePayProcessForm(ApplePayProcessFormRequest(paymentToken = token, mdOrder = mdOrder)).let {
                    answer(it.success, it.data, it.error)
                }
        }

    /**
     * The merchant's checkout settings, when a configured wallet the gateway enabled may need them (Google Pay's
     * merchant id, gateway and environment; hidden payment methods). Without them the wallets fall back to the
     * merchant's config: a failed request must not keep the buyer from paying by card.
     */
    private suspend fun settings(session: SessionStatusResponse): PaymentSettings? {
        val options = session.merchantOptions
        val wanted =
            (googlePay != null && WalletResolver.GOOGLE_PAY_OPTION in options) ||
                (applePay != null && WalletResolver.APPLE_PAY_OPTION in options)
        val login = session.merchantInfo.merchantLogin?.takeIf { it.isNotBlank() }
        if (!wanted || login == null) return null
        return try {
            // A slow answer must not hold the card form: without it the wallets use the merchant's config only.
            withTimeoutOrNull(SETTINGS_TIMEOUT_MS) { api.getPaymentSettings(login) }
                ?: null.also { logger.warning(TAG, "Payment settings did not arrive in time; wallets use the merchant's config only") }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SdkException) {
            logger.warning(TAG, "Payment settings unavailable; wallets use the merchant's config only", e)
            null
        }
    }

    /**
     * A wallet payment answer in the card form's shape: a challenge, or — `"success": false` — a refusal with the
     * gateway's reason (the buyer's wording first), so it ends the checkout as declined instead of being looked up.
     */
    private fun answer(
        success: Boolean,
        data: GPayData?,
        error: WalletError?,
    ): ProcessFormResponse {
        val refused = !success
        return ProcessFormResponse(
            errorCode = if (refused) error?.code?.toIntOrNull() ?: -1 else 0,
            acsUrl = data?.acsUrl,
            paReq = data?.paReq,
            termUrl = data?.termUrl,
            error = if (refused) error?.message ?: error?.description ?: "The gateway refused the wallet payment" else null,
            displayErrorMessage = if (refused) error?.displayErrorMessage else null,
            errorTypeName = data?.errorTypeName,
        )
    }
}
