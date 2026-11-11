package net.payrdr.mobile.payment.sdk.payment

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import net.payrdr.mobile.payment.sdk.core.SdkCore
import net.payrdr.mobile.payment.sdk.payment.api.PaymentApi
import net.payrdr.mobile.payment.sdk.payment.api.PaymentApiImpl
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.applepay.NoOpApplePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.flow.PaymentFlowController
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.gpay.NoOpGooglePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.http.HttpClientFactory
import net.payrdr.mobile.payment.sdk.payment.http.SharedHttpClients
import net.payrdr.mobile.payment.sdk.payment.http.defaultHttpClientFactory
import net.payrdr.mobile.payment.sdk.payment.keyprovider.KeyProvider
import net.payrdr.mobile.payment.sdk.payment.keyprovider.RemoteKeyProvider
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.logger.NoopLogTransport
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.threeds.WebChallengeHost
import net.payrdr.mobile.payment.sdk.payment.ui.util.deviceLanguage

/**
 * Public entry point of the sdk-payment module, created from a [SdkPaymentConfig]. It builds its own network layer
 * (gateway API, key source) the first time a checkout needs it; the HTTP client underneath, with the config's SSL
 * pinning, is shared by all instances with the same pinning, so creating an instance per checkout costs nothing.
 *
 * Offers three levels of integration:
 *  - [newFlow] — low-level: caller drives a [PaymentFlowController] with their own intent/state loop.
 *  - [paymentFlow] — headless: returns a cold Flow of [PaymentState]; caller starts by subscribing.
 *  - [completePayment] — headless terminal: suspend-returns the [CheckoutResult] once the FSM finishes.
 *
 * Turnkey UI entry points (Android `SdkPaymentActivity`, iOS `SdkPaymentViewControllerFactory`)
 * are in the platform source sets; they ultimately drive the same [PaymentFlowController].
 */
public class SdkPayment internal constructor(
    public val config: SdkPaymentConfig,
    private val network: SdkPaymentNetwork?,
    internal val httpClientFactory: HttpClientFactory = defaultHttpClientFactory(),
) {
    // The HTTP client for the gateway and the key source, built only when the SDK's own network is used, and shared
    // with every other SdkPayment of the process that has the same SSL pinning.
    private val httpClient by lazy { SharedHttpClients.get(httpClientFactory, config) }

    /** The gateway calls of every checkout this SDK runs. */
    internal val api: PaymentApi by lazy { network?.api ?: PaymentApiImpl(baseUrl = config.baseUrl, httpClient = httpClient) }

    /** The RSA public-key source of every checkout this SDK runs. */
    internal val keyProvider: KeyProvider by lazy {
        network?.keyProvider ?: RemoteKeyProvider(url = config.effectiveKeyProviderUrl(), httpClient = httpClient)
    }

    /** The SDK version, as published: `4.0.0`, `4.0.0-beta.1`. */
    public fun version(): String = VERSION

    /**
     * Creates a new [PaymentFlowController] on this SDK's network.
     *
     * @param webChallengeHost shows the bank's 3DS page (Activity-specific on Android, `UIViewController`-specific on iOS)
     * @param scope the coroutine scope the controller dispatches on
     */
    public fun newFlow(
        webChallengeHost: WebChallengeHost,
        scope: CoroutineScope,
    ): PaymentFlowController = newFlow(webChallengeHost, scope, NoOpGooglePayTokenProvider, NoOpApplePayTokenProvider)

    /** [newFlow] with the wallet token sources of the turnkey checkout. */
    internal fun newFlow(
        webChallengeHost: WebChallengeHost,
        scope: CoroutineScope,
        googlePayTokenProvider: GooglePayTokenProvider,
        applePayTokenProvider: ApplePayTokenProvider,
    ): PaymentFlowController =
        PaymentFlowController(
            api = api,
            keyProvider = keyProvider,
            core = SdkCore(),
            webChallengeHost = webChallengeHost,
            logger = Logger(transports = listOf(config.logTransport ?: NoopLogTransport)),
            scope = scope,
            googlePayTokenProvider = googlePayTokenProvider,
            applePayTokenProvider = applePayTokenProvider,
            // The checkout's language, or the device's when the merchant leaves it to the system.
            language = config.locale?.takeIf { it.isNotBlank() } ?: deviceLanguage(),
            googlePay = config.googlePay,
            applePay = config.applePay,
        )

    /**
     * Headless helper: starts a checkout run by posting `PaymentIntent.Start(checkout)` and returns
     * the controller's state as a [Flow]. The consumer is responsible for collecting this flow.
     *
     * @param checkout the order/session to check out
     * @param webChallengeHost 3DS host
     * @param scope coroutine scope the controller dispatches on
     */
    public fun paymentFlow(
        checkout: CheckoutConfig,
        webChallengeHost: WebChallengeHost,
        scope: CoroutineScope,
    ): Flow<PaymentState> {
        val controller = newFlow(webChallengeHost, scope)
        controller.handle(PaymentIntent.Start(checkout))
        return controller.state
    }

    /**
     * Headless helper: starts a checkout run and suspends until the FSM reaches [PaymentState.Completed],
     * returning the [CheckoutResult]. Intent handling happens on [scope].
     *
     * Throws if [scope] is cancelled before completion.
     */
    public suspend fun completePayment(
        checkout: CheckoutConfig,
        webChallengeHost: WebChallengeHost,
        scope: CoroutineScope,
    ): CheckoutResult {
        val controller = newFlow(webChallengeHost, scope)
        val deferred =
            scope.async(start = CoroutineStart.LAZY) {
                controller.state
                    .filterIsInstance<PaymentState.Completed>()
                    .first()
                    .result
            }
        deferred.start()
        controller.handle(PaymentIntent.Start(checkout))
        return deferred.await()
    }

    /** SDK metadata + primary factory for [SdkPayment]. */
    public companion object {
        public const val VERSION: String = SDK_VERSION

        /** The SDK for [config]; it builds its own network layer from it. */
        public fun create(config: SdkPaymentConfig): SdkPayment = SdkPayment(config, network = null)

        /** The SDK for [config] on a replaced [network], for tests. */
        internal fun create(
            config: SdkPaymentConfig,
            network: SdkPaymentNetwork,
        ): SdkPayment = SdkPayment(config, network)
    }
}
