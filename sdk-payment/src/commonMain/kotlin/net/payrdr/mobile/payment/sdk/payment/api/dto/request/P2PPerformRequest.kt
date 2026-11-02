package net.payrdr.mobile.payment.sdk.payment.api.dto.request

import net.payrdr.mobile.payment.sdk.payment.result.PayerField

/**
 * A one-leg AFT transfer of a registered order: `p2p/perform.do` (a new card) or `p2p/performByBinding.do` (a saved
 * one). Sent as JSON. The amount is not sent: the order's is transferred (an `amountInput` would change it).
 *
 * @property mdOrder order number
 * @property fromCard the card the money comes from
 * @property bindingNotNeeded the gateway does not save the card; always sent
 * @property payerData payer, sender and recipient fields collected for this card
 * @property language language of the gateway's messages, e.g. `en`
 */
public data class P2PPerformRequest(
    val mdOrder: String,
    val fromCard: P2PFromCard,
    val bindingNotNeeded: Boolean,
    val payerData: Map<PayerField, String> = emptyMap(),
    val language: String? = null,
) {
    // The seToken and payer data must not reach logs or crash reports.
    override fun toString(): String =
        "P2PPerformRequest(mdOrder=$mdOrder, fromCard=${fromCard::class.simpleName}, bindingNotNeeded=$bindingNotNeeded, " +
            "payerFields=${payerData.keys})"
}

/** The card a transfer debits. */
public sealed interface P2PFromCard {
    /** A card, or a saved card with its CVC, sealed by `SdkCore.generate(...)`. */
    public data class SeToken(
        val seToken: String,
    ) : P2PFromCard {
        override fun toString(): String = "SeToken(•••)"
    }

    /** A saved card the gateway debits without a CVC (`cvcBindingNotRequired`). */
    public data class Binding(
        val bindingId: String,
    ) : P2PFromCard

    /** A card in Apple Pay or Google Pay: the wallet's encrypted token, in base64, for the gateway to decrypt. */
    public data class Wallet(
        val vendor: WalletVendor,
        val paymentToken: String,
    ) : P2PFromCard {
        override fun toString(): String = "Wallet($vendor, •••)"
    }
}

/** The wallet a transfer's card comes from, as `fromCard.vendor` names it. */
public enum class WalletVendor { APPLE, GOOGLE }
