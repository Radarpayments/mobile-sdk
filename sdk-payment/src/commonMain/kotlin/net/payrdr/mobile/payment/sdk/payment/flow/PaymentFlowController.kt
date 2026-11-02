package net.payrdr.mobile.payment.sdk.payment.flow

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.payrdr.mobile.payment.sdk.core.SdkCore
import net.payrdr.mobile.payment.sdk.core.model.CardParams
import net.payrdr.mobile.payment.sdk.core.model.MsdkRegisteredFrom
import net.payrdr.mobile.payment.sdk.core.model.SdkCoreConfig
import net.payrdr.mobile.payment.sdk.payment.api.PaymentApi
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.ApplePayProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.GooglePayProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.P2PFromCard
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.P2PPerformRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.ProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.WalletVendor
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.FinishedPaymentInfoResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.GPayData
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.PaymentSettings
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ProcessFormResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.SessionStatusResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.WalletError
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayTokenResult
import net.payrdr.mobile.payment.sdk.payment.applepay.NoOpApplePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.exception.SdkApplePayException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkCryptogramException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkDeclinedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkGooglePayException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNetworkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNotConfiguredException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkOrderNotFoundException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkPaymentApiException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionExpiredException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionNotFoundException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkThreeDsChallengeFailedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkThreeDsTimeoutException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkTransactionException
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayTokenResult
import net.payrdr.mobile.payment.sdk.payment.gpay.NoOpGooglePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.keyprovider.KeyProvider
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.result.PaymentEffect
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.PaymentReceipt
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.result.ProcessingStep
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsChallenge
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsResult
import net.payrdr.mobile.payment.sdk.payment.threeds.WebChallengeHost
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardFormatters
import net.payrdr.mobile.payment.sdk.payment.utils.MoneyFormatter
import net.payrdr.mobile.payment.sdk.payment.utils.ORDER_STATUS_FAILED
import net.payrdr.mobile.payment.sdk.payment.utils.ORDER_STATUS_PAID
import net.payrdr.mobile.payment.sdk.payment.utils.OrderStatus
import net.payrdr.mobile.payment.sdk.payment.utils.SessionIdConverter
import net.payrdr.mobile.payment.sdk.payment.utils.containsAnyOfKeywordIgnoreCase
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock

private const val TAG = "PaymentFlowController"

/** Paid, then taken back: the payment did not go through for the buyer. */
private val ORDER_STATUS_UNDONE = listOf(OrderStatus.REVERSED, OrderStatus.REFUNDED)

/** `actionCode` of an order with no payment attempt recorded while its session is alive ("Waiting for payment attempt"). */
private const val ACTION_CODE_NO_ATTEMPT_YET = -100

/**
 * Publicly documented `actionCode`s for an outcome the gateway does not know yet, so not a decline: -30001 the status
 * inquiry to the bank's processing has started, 90 response status unknown, 998 the request timed out, 151019 the
 * transfer was sent but the bank did not answer.
 */
private val ACTION_CODES_OUTCOME_UNKNOWN = setOf(-30001, 90, 998, 151019)

/** How long the card form waits for the merchant's payment settings (wallet parameters) before showing without them. */
private const val SETTINGS_TIMEOUT_MS = 3_000L

/** Order feature of an order that only saves a card: a zero payment the gateway takes back. */
private const val FEATURE_VERIFY = "VERIFY"

/** Pauses before each order-status recheck after a payment's answer was lost (1 s, 2 s, 4 s). */
private val RECHECK_DELAYS_MS = listOf(1_000L, 2_000L, 4_000L)

/**
 * MVI-style state machine for one checkout run. Consumers observe [state] and submit [PaymentIntent]s
 * via [handle]. One-shot side-effect requests are delivered through [effects].
 *
 * [googlePayTokenProvider] and [applePayTokenProvider] are injected for the Tap-GPay / Tap-ApplePay
 * handlers. On Android [applePayTokenProvider] is a no-op returning NotAvailable; on iOS
 * [googlePayTokenProvider] is a no-op returning NotAvailable. Callers wire whichever makes sense.
 * [language] formats the order amount the way the checkout's language writes it; null keeps `€1,234.50`.
 * [googlePay] and [applePay] are the merchant's wallet configs: the controller completes them from the gateway's
 * settings and the order on start and offers each wallet only when the gateway has enabled it for the merchant.
 */
@Suppress("TooGenericExceptionCaught", "LongParameterList")
public class PaymentFlowController(
    private val api: PaymentApi,
    private val keyProvider: KeyProvider,
    private val core: SdkCore,
    private val webChallengeHost: WebChallengeHost,
    private val logger: Logger,
    private val scope: CoroutineScope,
    private val googlePayTokenProvider: GooglePayTokenProvider = NoOpGooglePayTokenProvider,
    private val applePayTokenProvider: ApplePayTokenProvider = NoOpApplePayTokenProvider,
    private val language: String? = null,
    private val googlePay: GooglePayConfig? = null,
    private val applePay: ApplePayConfig? = null,
) {
    private val _state: MutableStateFlow<PaymentState> = MutableStateFlow(PaymentState.Initializing)
    public val state: StateFlow<PaymentState> = _state.asStateFlow()

    private val _effects: MutableSharedFlow<PaymentEffect> =
        MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    public val effects: Flow<PaymentEffect> = _effects.asSharedFlow()

    private val _order: MutableStateFlow<OrderSummary?> = MutableStateFlow(null)

    /** Amount / merchant / order number of the loaded session; null until the session is loaded. */
    public val order: StateFlow<OrderSummary?> = _order.asStateFlow()

    /**
     * Awaited between entering [PaymentState.ThreeDsChallenge] and opening the bank page: the checkout UI uses it to
     * open its sheet to full height first, so the page appears over it without an animation of its own. Without a UI
     * (headless use) it returns at once.
     */
    internal var beforeChallenge: suspend () -> Unit = {}

    private var mdOrder: String = ""
    private var originalCheckoutId: String = ""

    /** The order is a one-leg AFT transfer: paid through `p2p/perform.do`. Null for an e-commerce order. */
    private var transfer: Transfer? = null
    private var latestSession: SessionStatusResponse? = null
    private var cardFormSnapshot: PaymentState.CardFormShown? = null

    /**
     * The checkout is being rebuilt after Android ended the process while it was open: a payment may have been sent
     * before, so a start that cannot reach the gateway has an unknown outcome rather than a failed one.
     */
    internal var restored: Boolean = false

    /**
     * The outcome if the checkout were closed now without finishing (the merchant app dismissed it, or the buyer's
     * task was cancelled): its own result when it has one, unknown while a payment is in flight, a cancel otherwise.
     */
    internal fun resultIfClosedNow(): CheckoutResult =
        when (val current = _state.value) {
            is PaymentState.Completed -> current.result
            is PaymentState.Processing, PaymentState.ThreeDsChallenge ->
                if (paymentInFlight || current == PaymentState.ThreeDsChallenge) {
                    CheckoutResult.Unknown(orderId())
                } else {
                    CheckoutResult.Canceled(orderId())
                }
            else -> CheckoutResult.Canceled(orderId())
        }

    /**
     * A failed checkout can go back to the form (it has one to return to), so "try again" makes sense; not once the
     * session expired, as the gateway has declined the order.
     */
    internal val canReturnToForm: Boolean
        get() {
            val result = (_state.value as? PaymentState.Completed)?.result
            return cardFormSnapshot != null && (result as? CheckoutResult.Failed)?.error !is SdkSessionExpiredException
        }

    /** A payment intent is being handled; set synchronously in [handle], so a second tap is seen at once. */
    private var paymentInFlight = false

    /** The current payment intent has sent the payment to the gateway: from here a lost answer is not a failure. */
    private var paymentSent = false

    /** The bank page of the current payment reported success: the buyer confirmed, the gateway decides the rest. */
    private var challengeSucceeded = false

    /** The order status of a sent payment is being asked again: the outcome may already be decided, so no cancel. */
    private var resolving = false

    public fun handle(intent: PaymentIntent) {
        val pays =
            intent is PaymentIntent.SubmitNewCard ||
                intent is PaymentIntent.SubmitSavedCard ||
                intent == PaymentIntent.TapGooglePay ||
                intent == PaymentIntent.TapApplePay
        if (pays) {
            // One payment at a time, and only from the form: a second tap on Pay (the step leaving the sheet still
            // takes touches for a moment) sends nothing.
            if (paymentInFlight || _state.value !is PaymentState.CardFormShown) return
            paymentInFlight = true
            paymentSent = false
            challengeSucceeded = false
        }
        scope.launch {
            try {
                when (intent) {
                    is PaymentIntent.Start -> handleStart(intent.checkout)
                    is PaymentIntent.SubmitNewCard -> handleSubmitNewCard(intent)
                    is PaymentIntent.SubmitSavedCard -> handleSubmitSavedCard(intent)
                    is PaymentIntent.UnbindSavedCard -> handleUnbindSavedCard(intent)
                    PaymentIntent.TapGooglePay -> handleTapGooglePay()
                    PaymentIntent.TapApplePay -> handleTapApplePay()
                    is PaymentIntent.ThreeDsCompleted -> Unit // Handled inline in performChallenge
                    PaymentIntent.BackToPaymentMethods -> handleBackToPaymentMethods()
                    PaymentIntent.Cancel -> if (!resolving) complete(CheckoutResult.Canceled(orderId()))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // The type only: an intent may carry card data (its toString hides it, but the log should not depend on that).
                logger.error(TAG, "Intent ${intent::class.simpleName} failed", e)
                // No usable answer to a sent payment (no network, an HTTP error, a body that does not parse): it may still
                // have gone through, so the order status decides.
                val answerLost = e is SdkNetworkException || e is SdkPaymentApiException
                when {
                    pays && paymentSent && answerLost -> completeAfterLostAnswer(e as SdkException)
                    intent is PaymentIntent.Start && restored && answerLost ->
                        complete(
                            CheckoutResult.Unknown(orderId(), e as SdkException),
                        )
                    else -> completeWithError(asSdkException(e))
                }
            } finally {
                if (pays) paymentInFlight = false
            }
        }
    }

    private suspend fun handleStart(checkout: CheckoutConfig) {
        _state.value = PaymentState.Processing(ProcessingStep.LOADING_SESSION)
        mdOrder =
            when (checkout) {
                is CheckoutConfig.MdOrder -> checkout.value
                is CheckoutConfig.SessionId -> SessionIdConverter.sessionIdToMdOrder(checkout.value)
            }
        originalCheckoutId =
            when (checkout) {
                is CheckoutConfig.MdOrder -> checkout.value
                is CheckoutConfig.SessionId -> checkout.value
            }

        val session = api.getSessionStatus(mdOrder)
        latestSession = session
        // No countdown = no open payment session: the order is paid, declined, expired or unknown.
        if (session.remainingSecs == null) {
            complete(closedSessionResult(checkout))
            return
        }
        if (Transfer.isUnsupported(session)) {
            complete(CheckoutResult.Failed(orderId(), SdkNotConfiguredException("Only one-leg AFT transfers are supported")))
            return
        }
        val transfer = Transfer.of(session)
        this.transfer = transfer
        _order.value =
            OrderSummary(
                // An order that only saves a card pays nothing: its zero is not shown anywhere.
                formattedAmount =
                    if (transfer?.saveCardOnly ==
                        true
                    ) {
                        null
                    } else {
                        MoneyFormatter.format(session.amount, session.currencyAlphaCode, language)
                    },
                orderNumber = session.orderNumber,
            )
        val payerData = PayerDataMapper.request(session)
        val walletsOffered = transfer == null || transfer.paysByWallet(payerData)
        val settings = if (walletsOffered) walletSettings(session) else null
        val cardForm =
            PaymentState.CardFormShown(
                savedCards =
                    (if (transfer != null) Transfer.debitCards(session) else session.bindingItems).map { item ->
                        val (maskedPan, expiry) = CardFormatters.splitBindingLabel(item.label)
                        SavedCardInfo(
                            bindingId = item.id,
                            maskedPan = maskedPan,
                            paymentSystem = item.paymentSystem,
                            cardHolder = item.cardHolder,
                            canDelete = session.bindingDeactivationEnabled,
                            expiry = expiry,
                            payerPrefill = PayerDataMapper.bindingPrefill(item),
                        )
                    },
                allowNewCard = true,
                cvcRequired = if (transfer != null) !transfer.cvcBindingNotRequired else !session.cvcNotRequired,
                canSaveCard = if (transfer != null) transfer.saveCard.shown else session.bindingEnabled,
                payerData = payerData,
                googlePay = if (walletsOffered) googlePay?.let { WalletResolver.googlePay(it, session, settings) } else null,
                applePay = if (walletsOffered) applePay?.let { WalletResolver.applePay(it, session, settings) } else null,
                transfer = transfer != null,
                saveCardByDefault = transfer == null || transfer.saveCard == Transfer.SaveCard.LOCKED,
                saveCardLocked = transfer?.saveCard == Transfer.SaveCard.LOCKED,
                saveCardOnly = transfer?.saveCardOnly == true,
            )
        cardFormSnapshot = cardForm
        _state.value = cardForm
    }

    /**
     * The merchant's checkout settings, when a configured wallet the gateway enabled may need them (Google Pay's
     * merchant id, gateway and environment; hidden payment methods). Without them the wallets fall back to the
     * merchant's config: a failed request must not keep the buyer from paying by card.
     */
    private suspend fun walletSettings(session: SessionStatusResponse): PaymentSettings? {
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

    private suspend fun handleSubmitNewCard(intent: PaymentIntent.SubmitNewCard) {
        _state.value = PaymentState.Processing(ProcessingStep.LOADING_PUBLIC_KEY)
        val key = keyProvider.provideKey()

        _state.value = PaymentState.Processing(ProcessingStep.TOKENIZING_CARD)
        val tokenResult =
            core.generate(
                SdkCoreConfig(
                    cardParams =
                        CardParams(
                            mdOrder = mdOrder,
                            pan = intent.pan,
                            cvc = intent.cvc,
                            expiryMMYY = intent.expiryMMYY,
                            cardHolder =
                                if (transfer !=
                                    null
                                ) {
                                    transferCardholder(intent.cardHolder, intent.payerData)
                                } else {
                                    intent.cardHolder
                                },
                            pubKey = key.value,
                        ),
                    timestamp = epochMs(),
                    registeredFrom = MsdkRegisteredFrom.MSDK_PAYMENT,
                ),
            )
        val token =
            tokenResult.token
                ?: throw SdkCryptogramException("Tokenization failed: ${tokenResult.errors}")

        _state.value = PaymentState.Processing(ProcessingStep.SUBMITTING_PAYMENT)
        paymentSent = true
        val transfer = transfer
        val response =
            if (transfer != null) {
                api.p2pPerform(
                    P2PPerformRequest(
                        mdOrder = mdOrder,
                        fromCard = P2PFromCard.SeToken(token),
                        bindingNotNeeded = transfer.bindingNotNeeded(intent.saveCard),
                        payerData = intent.payerData,
                        language = language,
                    ),
                )
            } else {
                api.processForm(
                    ProcessFormRequest(
                        paymentToken = token,
                        mdOrder = mdOrder,
                        holder = intent.cardHolder?.ifBlank { "CARDHOLDER" } ?: "CARDHOLDER",
                        saveCard = intent.saveCard,
                        payerData = intent.payerData,
                    ),
                )
            }

        resolveProcessFormResponse(response)
    }

    private suspend fun handleSubmitSavedCard(intent: PaymentIntent.SubmitSavedCard) {
        val transfer = transfer
        if (transfer != null && transfer.cvcBindingNotRequired) {
            // The gateway wants a CVC inside a saved card's seToken: without one the card goes by its binding id.
            _state.value = PaymentState.Processing(ProcessingStep.SUBMITTING_PAYMENT)
            paymentSent = true
            resolveProcessFormResponse(api.p2pPerformByBinding(transferFromSavedCard(P2PFromCard.Binding(intent.bindingId), intent)))
            return
        }
        _state.value = PaymentState.Processing(ProcessingStep.LOADING_PUBLIC_KEY)
        val key = keyProvider.provideKey()

        _state.value = PaymentState.Processing(ProcessingStep.TOKENIZING_CARD)
        val tokenResult =
            core.generate(
                SdkCoreConfig(
                    cardParams =
                        net.payrdr.mobile.payment.sdk.core.model.BindingParams(
                            mdOrder = mdOrder,
                            bindingID = intent.bindingId,
                            cvc = intent.cvc,
                            pubKey = key.value,
                        ),
                    timestamp = epochMs(),
                    registeredFrom = MsdkRegisteredFrom.MSDK_PAYMENT,
                ),
            )
        val token =
            tokenResult.token
                ?: throw SdkCryptogramException("Tokenization failed: ${tokenResult.errors}")

        _state.value = PaymentState.Processing(ProcessingStep.SUBMITTING_PAYMENT)
        paymentSent = true
        if (transfer != null) {
            resolveProcessFormResponse(api.p2pPerformByBinding(transferFromSavedCard(P2PFromCard.SeToken(token), intent)))
            return
        }
        val response =
            api.processBindingForm(
                ProcessFormRequest(
                    paymentToken = token,
                    mdOrder = mdOrder,
                    holder = "CARDHOLDER",
                    saveCard = false,
                    payerData = intent.payerData,
                ),
            )

        resolveProcessFormResponse(response)
    }

    /** A transfer from a saved card: the card is already saved, so the gateway saves nothing. */
    private fun transferFromSavedCard(
        fromCard: P2PFromCard,
        intent: PaymentIntent.SubmitSavedCard,
    ) = P2PPerformRequest(
        mdOrder = mdOrder,
        fromCard = fromCard,
        bindingNotNeeded = true,
        payerData = intent.payerData,
        language = language,
    )

    /**
     * A transfer from a wallet: the gateway decrypts the card from the token. Nothing is saved — a wallet card is not
     * kept for later transfers — and no payer fields go: the wallet is offered only when the order asks for none.
     */
    private fun transferFromWallet(fromCard: P2PFromCard.Wallet) =
        P2PPerformRequest(
            mdOrder = mdOrder,
            fromCard = fromCard,
            bindingNotNeeded = true,
            language = language,
        )

    private suspend fun handleUnbindSavedCard(intent: PaymentIntent.UnbindSavedCard) {
        api.unbindCardAnonymous(bindingId = intent.bindingId, mdOrder = mdOrder)
        val snapshot = cardFormSnapshot ?: return
        val updated = snapshot.copy(savedCards = snapshot.savedCards.filterNot { it.bindingId == intent.bindingId })
        cardFormSnapshot = updated
        if (_state.value is PaymentState.CardFormShown) _state.value = updated
    }

    private fun handleBackToPaymentMethods() {
        val current = _state.value as? PaymentState.Completed ?: return
        if (current.result !is CheckoutResult.Failed || !canReturnToForm) return
        cardFormSnapshot?.let { _state.value = it }
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Suppress("ThrowsCount") // precondition + token-result error branches are each a distinct contract
    private suspend fun handleTapGooglePay() {
        require(_state.value is PaymentState.CardFormShown) {
            "TapGooglePay requires state to be CardFormShown, got ${_state.value}"
        }
        val session = latestSession ?: throw SdkNotConfiguredException("Google Pay tapped before session was loaded")
        val config =
            cardFormSnapshot?.googlePay ?: run {
                // A button the order does not offer (e.g. a merchant's own form): nothing to open, the form stays.
                logger.warning(TAG, "Google Pay tapped but not offered for this order")
                cardFormSnapshot?.let { _state.value = it }
                return
            }
        val amountMinor = MoneyFormatter.minorUnits(session.amount, session.currencyAlphaCode) ?: 0L

        _state.value = PaymentState.Processing(ProcessingStep.SUBMITTING_PAYMENT)
        val tokenResult =
            googlePayTokenProvider.requestPaymentToken(
                config = config,
                amountMinorUnits = amountMinor,
                // Both known: the resolver offers Google Pay only with the order's currency and a country.
                currencyCode = session.currencyAlphaCode.orEmpty(),
                countryCode = config.countryCode.orEmpty(),
            )
        when (tokenResult) {
            is GooglePayTokenResult.Success -> {
                paymentSent = true
                // The gateway reads Google's token as base64, as its own payment page sends it.
                val token = Base64.encode(tokenResult.paymentToken.encodeToByteArray())
                if (transfer != null) {
                    resolveProcessFormResponse(api.p2pPerform(transferFromWallet(P2PFromCard.Wallet(WalletVendor.GOOGLE, token))))
                    return
                }
                val response = api.googlePayProcessForm(GooglePayProcessFormRequest(paymentToken = token, mdOrder = mdOrder))
                // Flatten into a ProcessFormResponse-shape for shared 3DS and refusal handling.
                resolveProcessFormResponse(walletAnswer(response.success, response.data, response.error))
            }
            GooglePayTokenResult.Cancelled -> {
                // User dismissed the GPay sheet — return to card form, don't terminate checkout.
                cardFormSnapshot?.let { _state.value = it }
            }
            GooglePayTokenResult.NotAvailable ->
                throw SdkGooglePayException("Google Pay is not available on this device")
            is GooglePayTokenResult.Failure ->
                throw SdkGooglePayException(tokenResult.reason, tokenResult.cause)
        }
    }

    @Suppress("ThrowsCount") // precondition + token-result error branches are each a distinct contract
    private suspend fun handleTapApplePay() {
        require(_state.value is PaymentState.CardFormShown) {
            "TapApplePay requires state to be CardFormShown, got ${_state.value}"
        }
        val session = latestSession ?: throw SdkNotConfiguredException("Apple Pay tapped before session was loaded")
        val config =
            cardFormSnapshot?.applePay ?: run {
                // A button the order does not offer (e.g. a merchant's own form): nothing to open, the form stays.
                logger.warning(TAG, "Apple Pay tapped but not offered for this order")
                cardFormSnapshot?.let { _state.value = it }
                return
            }
        val amountMinor = MoneyFormatter.minorUnits(session.amount, session.currencyAlphaCode) ?: 0L
        val merchantName = session.merchantInfo.merchantFullName.orEmpty()

        _state.value = PaymentState.Processing(ProcessingStep.SUBMITTING_PAYMENT)
        val tokenResult =
            applePayTokenProvider.requestPaymentToken(
                config = config,
                amountMinorUnits = amountMinor,
                // Both known: the resolver offers Apple Pay only with the order's currency and a country.
                currencyCode = session.currencyAlphaCode.orEmpty(),
                countryCode = config.countryCode.orEmpty(),
                merchantDisplayName = merchantName,
            )
        when (tokenResult) {
            is ApplePayTokenResult.Success -> {
                paymentSent = true
                if (transfer != null) {
                    resolveProcessFormResponse(
                        api.p2pPerform(transferFromWallet(P2PFromCard.Wallet(WalletVendor.APPLE, tokenResult.paymentToken))),
                    )
                    return
                }
                val response =
                    api.applePayProcessForm(
                        ApplePayProcessFormRequest(
                            paymentToken = tokenResult.paymentToken,
                            mdOrder = mdOrder,
                        ),
                    )
                resolveProcessFormResponse(walletAnswer(response.success, response.data, response.error))
            }
            ApplePayTokenResult.Cancelled -> {
                // User dismissed the Apple Pay sheet — return to card form, don't terminate checkout.
                cardFormSnapshot?.let { _state.value = it }
            }
            ApplePayTokenResult.NotAvailable ->
                throw SdkApplePayException("Apple Pay is not available on this device")
            is ApplePayTokenResult.Failure ->
                throw SdkApplePayException(tokenResult.reason, tokenResult.cause)
        }
    }

    /**
     * A wallet payment answer in the card form's shape: a challenge, or — `"success": false` — a refusal with the
     * gateway's reason (the buyer's wording first), so it ends the checkout as declined instead of being looked up.
     */
    private fun walletAnswer(
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

    private suspend fun resolveProcessFormResponse(response: ProcessFormResponse) {
        when {
            response.threeDSMethodURL != null ->
                throw SdkNotConfiguredException("Merchant requires 3DS2SDK which this SDK does not support")

            response.acsUrl != null && response.paReq != null && response.termUrl != null -> {
                _state.value = PaymentState.ThreeDsChallenge
                val challenge =
                    ThreeDsChallenge(
                        mdOrder = mdOrder,
                        acsUrl = response.acsUrl,
                        paReq = response.paReq,
                        md = mdOrder,
                        termUrl = response.termUrl,
                        // An API V2 session's buyer comes back to its successUrl/failureUrl naming the session; an
                        // order registered through API V1 has a session id too, the gateway just never sends it.
                        sessionId = SessionIdConverter.mdOrderToSessionId(mdOrder),
                    )
                _effects.tryEmit(PaymentEffect.ShowThreeDs(challenge))
                beforeChallenge()
                // Cancelled while the checkout made room: the bank page must not open over a finished checkout.
                if (_state.value != PaymentState.ThreeDsChallenge) return
                when (val result = webChallengeHost.performChallenge(challenge)) {
                    is ThreeDsResult.Succeeded -> {
                        challengeSucceeded = true
                        finalize()
                    }
                    is ThreeDsResult.Failed -> completeWithError(SdkThreeDsChallengeFailedException(result.reason))
                    ThreeDsResult.TimedOut -> completeAfterLostChallenge(SdkThreeDsTimeoutException())
                    is ThreeDsResult.Interrupted -> completeAfterLostChallenge(SdkThreeDsChallengeFailedException(result.reason))
                    ThreeDsResult.Cancelled -> complete(CheckoutResult.Canceled(orderId()))
                }
            }

            // The gateway writes an empty `displayErrorMessage` when it has no wording for the buyer: the reason is then `error`.
            response.acsUrl == null && refusalReason(response) != null -> {
                val reason = refusalReason(response)
                completeWithError(SdkDeclinedException(declineCode = response.errorTypeName, message = reason), reason)
            }

            else -> finalize()
        }
    }

    private fun refusalReason(response: ProcessFormResponse): String? =
        response.displayErrorMessage?.takeIf { it.isNotBlank() } ?: response.error?.takeIf { it.isNotBlank() }

    private suspend fun finalize() {
        _state.value = PaymentState.Processing(ProcessingStep.FINALIZING)
        val finishedInfo = api.getFinishedPaymentInfo(mdOrder)
        if (settledResult(finishedInfo) != null) {
            completeFrom(finishedInfo)
            return
        }
        // Neither paid nor declined yet (still CREATED, or no status): ask again before giving up.
        val recheck = recheck()
        if (recheck.info != null) completeFrom(recheck.info) else complete(CheckoutResult.Unknown(orderId(), recheck.lastError))
    }

    /** Ends with the outcome [info] settles, and its receipt. */
    private fun completeFrom(info: FinishedPaymentInfoResponse) {
        complete(
            result = checkNotNull(settledResult(info)),
            receipt =
                PaymentReceipt(
                    // The gateway's "3.90 EUR", written like the order amount before it; it may leave the code out ("3.90").
                    formattedAmount =
                        if (savesCardOnly(info)) {
                            null
                        } else {
                            MoneyFormatter.format(info.formattedAmount, latestSession?.currencyAlphaCode, language)
                                ?: _order.value?.formattedAmount
                        },
                    paymentDate = info.paymentDate,
                    panMasked = info.panMasked,
                    approvalCode = info.approvalCode.orNullIfBlank(),
                    refNum = info.refNum.orNullIfBlank(),
                    actionCodeDescription = declineReason(info) ?: info.actionCodeDescription,
                ),
        )
    }

    /** The payment was sent but its answer was lost: the order status decides, if it can be learned. */
    private suspend fun completeAfterLostAnswer(error: SdkException) {
        val recheck = recheck(settlesOrAwaitsAttempt)
        val info = recheck.info
        when {
            info == null -> complete(CheckoutResult.Unknown(orderId(), recheck.lastError ?: error))
            // The buyer confirmed on the bank page, but no attempt is recorded yet: the gateway has not settled it.
            awaitsAttempt(info) && challengeSucceeded -> complete(CheckoutResult.Unknown(orderId(), error))
            // The payment never reached the gateway: nothing was charged, the buyer can pay again.
            awaitsAttempt(info) -> completeWithError(error)
            else -> completeFrom(info)
        }
    }

    /** Once our request or the bank page is lost, "no attempt recorded" is an answer too: the attempt did not happen. */
    private val settlesOrAwaitsAttempt: (FinishedPaymentInfoResponse) -> Boolean = { settledResult(it) != null || awaitsAttempt(it) }

    /**
     * The bank page ended without telling how (it timed out or lost its connection), but the buyer may have confirmed
     * before it did: the order status decides. [error] is what the buyer is told if the order is not paid.
     */
    private suspend fun completeAfterLostChallenge(error: SdkException) {
        val info = recheck(settlesOrAwaitsAttempt).info
        when {
            info == null -> complete(CheckoutResult.Unknown(orderId(), error))
            // Paid, declined with the gateway's own reason, or expired: that is the outcome, with its receipt.
            isPaid(info) || declineReason(info) != null || info.sessionExpired -> completeFrom(info)
            else -> completeWithError(error)
        }
    }

    /**
     * The outcome [info] settles, or null while the gateway has not settled the order: still CREATED with no code,
     * with -100 (no payment attempt recorded yet and the session is alive — not a decline), or with a code saying the
     * gateway itself does not know the outcome yet ([ACTION_CODES_OUTCOME_UNKNOWN]).
     */
    private fun settledResult(info: FinishedPaymentInfoResponse): CheckoutResult? {
        val status = info.status
        val declineReason = declineReason(info)
        return when {
            // The gateway sends nothing but this flag for an order its session expired on (declined, never paid).
            info.sessionExpired -> CheckoutResult.Failed(orderId(), SdkSessionExpiredException(mdOrder))
            isPaid(info) || cardSaved(info) -> CheckoutResult.Completed(orderId())
            awaitsAttempt(info) || info.actionCode in ACTION_CODES_OUTCOME_UNKNOWN -> null
            declineReason != null ->
                CheckoutResult.Failed(
                    orderId(),
                    SdkDeclinedException(declineCode = info.errorTypeName ?: info.actionCode.toString(), message = declineReason),
                )
            status != null && status.containsAnyOfKeywordIgnoreCase(ORDER_STATUS_FAILED) ->
                CheckoutResult.Failed(orderId(), SdkDeclinedException(message = "Order is declined"))
            status != null && status.containsAnyOfKeywordIgnoreCase(ORDER_STATUS_UNDONE) ->
                CheckoutResult.Failed(orderId(), SdkTransactionException("Payment finished with status $status"))
            else -> null
        }
    }

    private fun isPaid(info: FinishedPaymentInfoResponse): Boolean = info.status?.containsAnyOfKeywordIgnoreCase(ORDER_STATUS_PAID) == true

    /**
     * An order that only saves a card ended with the card saved: the gateway makes a zero payment and takes it back, so
     * `REVERSED` with action code 0 is its success, as on the gateway's own page.
     */
    private fun cardSaved(info: FinishedPaymentInfoResponse): Boolean =
        savesCardOnly(info) && info.actionCode == 0 && info.status?.containsAnyOfKeywordIgnoreCase(listOf(OrderStatus.REVERSED)) == true

    /** The order only saves a card (`VERIFY`): as the order status or the payment session tells. */
    private fun savesCardOnly(info: FinishedPaymentInfoResponse): Boolean =
        FEATURE_VERIFY in info.orderFeatures || latestSession?.orderFeatures?.contains(FEATURE_VERIFY) == true

    /** No payment attempt is recorded and the session is alive (-100): nothing was charged, the order can still be paid. */
    private fun awaitsAttempt(info: FinishedPaymentInfoResponse): Boolean = !isPaid(info) && info.actionCode == ACTION_CODE_NO_ATTEMPT_YET

    /**
     * The gateway's explanation of a decline for the payer; set only when an action code reports one (not -100 or a
     * code for an outcome the gateway does not know yet, whose texts read like "pending" but decline nothing).
     */
    private fun declineReason(info: FinishedPaymentInfoResponse): String? {
        val code = info.actionCode ?: 0
        val declines = !isPaid(info) && code != 0 && code != ACTION_CODE_NO_ATTEMPT_YET && code !in ACTION_CODES_OUTCOME_UNKNOWN
        return (info.displayErrorMessage.orNullIfBlank() ?: info.actionCodeDescription.orNullIfBlank()).takeIf { declines }
    }

    /** What [recheck] learned: the first settled order status, or the last error seen while asking. */
    private class Recheck(
        val info: FinishedPaymentInfoResponse?,
        val lastError: SdkException?,
    )

    /**
     * Asks the gateway how the order ended, waiting [RECHECK_DELAYS_MS] before each attempt; the first settled answer
     * wins. Used once a payment was sent but its outcome did not arrive, so the buyer is not told "failed" for a
     * payment that went through.
     */
    private suspend fun recheck(settles: (FinishedPaymentInfoResponse) -> Boolean = { settledResult(it) != null }): Recheck {
        _state.value = PaymentState.Processing(ProcessingStep.FINALIZING)
        resolving = true
        try {
            var lastError: SdkException? = null
            for (wait in RECHECK_DELAYS_MS) {
                delay(wait)
                try {
                    val info = api.getFinishedPaymentInfo(mdOrder)
                    if (settles(info)) return Recheck(info, lastError)
                } catch (e: SdkException) {
                    lastError = e
                }
            }
            return Recheck(null, lastError)
        } finally {
            resolving = false
        }
    }

    /** The gateway writes absent values as blank or as the string `null`. */
    private fun String?.orNullIfBlank(): String? = this?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }

    /**
     * The outcome for a session without `remainingSecs`, from the order's final status: an order paid earlier is
     * [CheckoutResult.Completed], anything else cannot be paid any more.
     */
    private suspend fun closedSessionResult(checkout: CheckoutConfig): CheckoutResult {
        val info = api.getFinishedPaymentInfo(mdOrder)
        val status = info.status
        val error =
            when {
                info.sessionExpired -> SdkSessionExpiredException(mdOrder)
                status == null ->
                    when (checkout) {
                        is CheckoutConfig.SessionId -> SdkSessionNotFoundException(checkout.value)
                        is CheckoutConfig.MdOrder -> SdkOrderNotFoundException(mdOrder)
                    }
                status.containsAnyOfKeywordIgnoreCase(ORDER_STATUS_PAID) || cardSaved(info) -> return CheckoutResult.Completed(orderId())
                // The gateway's code and wording when it has them (e.g. DECLINED_BY_MERCHANT: an API V2 session expired
                // by the merchant).
                status.containsAnyOfKeywordIgnoreCase(ORDER_STATUS_FAILED) ->
                    SdkDeclinedException(
                        declineCode = info.errorTypeName.orNullIfBlank(),
                        message = declineReason(info) ?: "Order is declined",
                    )
                else -> SdkTransactionException("Order cannot be paid, status $status")
            }
        return CheckoutResult.Failed(orderId(), error)
    }

    /** @param reason the gateway's explanation for the payer, shown on the result screen */
    private fun completeWithError(
        error: SdkException,
        reason: String? = null,
    ) {
        complete(
            result = CheckoutResult.Failed(orderId(), error),
            receipt = reason?.let { PaymentReceipt(formattedAmount = _order.value?.formattedAmount, actionCodeDescription = it) },
        )
    }

    private fun complete(
        result: CheckoutResult,
        receipt: PaymentReceipt? = null,
    ) {
        // The first outcome is the one the merchant gets; a late one (the bank page answering after a cancel) is dropped.
        if (_state.value is PaymentState.Completed) return
        _state.value = PaymentState.Completed(result = result, receipt = receipt)
        _effects.tryEmit(PaymentEffect.Dismiss)
    }

    /** The order the checkout pays; before the session id is decoded (or if it cannot be), the id the merchant passed. */
    private fun orderId(): String = mdOrder.ifBlank { originalCheckoutId }

    private fun asSdkException(e: Throwable): SdkException = if (e is SdkException) e else SdkException(e.message, e)

    private fun epochMs(): Long = Clock.System.now().toEpochMilliseconds()
}
