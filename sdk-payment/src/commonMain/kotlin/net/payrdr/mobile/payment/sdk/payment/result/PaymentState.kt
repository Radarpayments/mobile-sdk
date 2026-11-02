package net.payrdr.mobile.payment.sdk.payment.result

import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig

/**
 * Observable state of a checkout run. Emitted by `PaymentFlowController.state` as a `StateFlow`.
 */
public sealed interface PaymentState {
    /** Initial state — session not yet loaded. */
    public data object Initializing : PaymentState

    /**
     * Card form is ready to be displayed; may or may not include saved cards.
     *
     * [googlePay] and [applePay] are the wallets this order can be paid with — the merchant's config completed
     * from the gateway and the order — or null when the merchant did not configure one or the gateway does not
     * offer it. Whether the device can pay with it is up to the platform.
     *
     * [transfer] — the order is a one-leg AFT transfer (a P2P order with `WITHOUT_TO_CARD`): the money goes to the
     * recipient the merchant registered, the button says "Transfer", and only cards the gateway may debit for
     * transfers are offered. [saveCardByDefault] is where the "Save card" box starts; [saveCardLocked] keeps it
     * ticked (the order demands a saved card). [saveCardOnly] — the transfer moves no money, it only saves a new card
     * (`VERIFY`, a zero amount): no saved cards, no "Save card" box, the button says "Save card".
     */
    public data class CardFormShown(
        val savedCards: List<SavedCardInfo>,
        val allowNewCard: Boolean,
        val cvcRequired: Boolean,
        val canSaveCard: Boolean = true,
        val payerData: PayerDataRequest = PayerDataRequest(),
        val googlePay: GooglePayConfig? = null,
        val applePay: ApplePayConfig? = null,
        val transfer: Boolean = false,
        val saveCardByDefault: Boolean = true,
        val saveCardLocked: Boolean = false,
        val saveCardOnly: Boolean = false,
    ) : PaymentState

    /** In-flight backend work — tokenization, processForm call, polling. */
    public data class Processing(
        val step: ProcessingStep,
    ) : PaymentState

    /** Awaiting 3DS v1 web challenge completion. */
    public data object ThreeDsChallenge : PaymentState

    /** Terminal — the result has been computed. [receipt] is present when the gateway reported a final order status. */
    public data class Completed(
        val result: CheckoutResult,
        val receipt: PaymentReceipt? = null,
    ) : PaymentState
}

/** Granular "what are we doing right now" annotation for [PaymentState.Processing]. */
public enum class ProcessingStep {
    LOADING_SESSION,
    LOADING_PUBLIC_KEY,
    TOKENIZING_CARD,
    SUBMITTING_PAYMENT,
    FINALIZING,
}

/**
 * Minimal info about a saved binding displayed in the card list.
 *
 * @property maskedPan masked PAN without the expiry, e.g. `411111**1111`
 * @property expiry card expiry as `MM/YY`, or null if the gateway did not send it
 * @property payerPrefill payer details remembered with the binding; override [PayerDataRequest.prefill]
 */
public data class SavedCardInfo(
    val bindingId: String,
    val maskedPan: String,
    val paymentSystem: String,
    val cardHolder: String,
    val canDelete: Boolean,
    val expiry: String? = null,
    val payerPrefill: Map<PayerField, String> = emptyMap(),
)

/**
 * What the checkout UI shows about the order: header of the payment sheet, Pay button, processing
 * and result screens. Every field is optional — the gateway may omit any of them.
 *
 * The merchant name is deliberately not shown: it is a gateway setting, not something the payer recognises.
 *
 * @property formattedAmount amount with currency, e.g. `€49.90`
 * @property orderNumber merchant-side order number
 */
public data class OrderSummary(
    val formattedAmount: String? = null,
    val orderNumber: String? = null,
)

/**
 * Final order details from `getFinishedPaymentInfo.do`, shown on the optional result screen.
 */
public data class PaymentReceipt(
    val formattedAmount: String? = null,
    val paymentDate: String? = null,
    val panMasked: String? = null,
    val approvalCode: String? = null,
    val refNum: String? = null,
    val actionCodeDescription: String? = null,
)
