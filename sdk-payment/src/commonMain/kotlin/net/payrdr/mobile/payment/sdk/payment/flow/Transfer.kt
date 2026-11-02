package net.payrdr.mobile.payment.sdk.payment.flow

import net.payrdr.mobile.payment.sdk.payment.api.dto.response.BindingItem
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.SessionStatusResponse
import net.payrdr.mobile.payment.sdk.payment.result.PayerDataRequest
import net.payrdr.mobile.payment.sdk.payment.result.PayerField

/**
 * A one-leg AFT transfer: a P2P order (`registerP2P.do`) with `WITHOUT_TO_CARD` — the money goes from the buyer's card
 * to the recipient the merchant registered, through `p2p/perform.do` instead of `processform.do`. Read the way the
 * gateway's own payment page reads it, from `getSessionStatus.do`.
 *
 * @property saveCard whether, and how, the buyer may save the card for later transfers
 * @property cvcBindingNotRequired a saved card goes without its CVC (by binding id)
 * @property saveCardOnly the order moves no money, it only saves a new card (`VERIFY`, a zero amount)
 */
internal class Transfer(
    val saveCard: SaveCard,
    val cvcBindingNotRequired: Boolean,
    val saveCardOnly: Boolean = false,
) {
    /** How the "Save card" box of a transfer is shown, and whether a new card is saved. */
    enum class SaveCard {
        /** Not shown, nothing saved: the merchant may not keep debit cards, or the order forbids saving. */
        HIDDEN,

        /** Shown unticked: the buyer decides. */
        OFFERED,

        /** Shown ticked and locked: the order demands a saved card (`FORCE_CREATE_BINDING`). */
        LOCKED,

        /**
         * Not shown, yet the card is saved: the order demands it (`FORCE_CREATE_BINDING`, `VERIFY`) and there is no box
         * to show — the order only saves a card, or the merchant has no option to offer one.
         */
        IMPLIED,
        ;

        /** The box is on the form. */
        val shown: Boolean get() = this == OFFERED || this == LOCKED
    }

    /**
     * `bindingNotNeeded` of a new card: the box when the buyer decides, fixed otherwise. Always false when the order
     * demands a saved card: true would cancel what the merchant asked for (and a `VERIFY` order is refused with it).
     */
    fun bindingNotNeeded(saveCardTicked: Boolean): Boolean =
        when (saveCard) {
            SaveCard.HIDDEN -> true
            SaveCard.LOCKED, SaveCard.IMPLIED -> false
            SaveCard.OFFERED -> !saveCardTicked
        }

    /**
     * Apple Pay and Google Pay may pay this transfer. A wallet card is never saved, so not when the order demands a
     * saved card; and a wallet has no form, so not when the order asks the buyer for a mandatory detail.
     */
    fun paysByWallet(payerData: PayerDataRequest): Boolean =
        (saveCard == SaveCard.HIDDEN || saveCard == SaveCard.OFFERED) &&
            payerData.fieldsByPaymentSystem.values.none { fields -> fields.any { it.mandatory } }

    companion object {
        private const val PAYMENT_WAY_P2P = "P2P"
        private const val FEATURE_WITHOUT_TO_CARD = "WITHOUT_TO_CARD"
        private const val FEATURE_VERIFY = "VERIFY"
        private const val FEATURE_FORCE_CREATE_BINDING = "FORCE_CREATE_BINDING"
        private const val FEATURE_BINDING_NOT_NEEDED = "BINDING_NOT_NEEDED"
        private const val OPTION_DEBIT_BINDING_ALLOWED = "DEBIT_BINDING_ALLOWED"

        /** Category of a saved card a transfer may debit. */
        private const val CATEGORY_DEBIT = "D"

        /** A P2P order this SDK cannot pay: a transfer to a card (OCT) or between two cards (two-leg). */
        fun isUnsupported(session: SessionStatusResponse): Boolean = session.isP2P && FEATURE_WITHOUT_TO_CARD !in session.orderFeatures

        /** The transfer [session] is, or null for an e-commerce order. */
        fun of(session: SessionStatusResponse): Transfer? {
            if (!session.isP2P || FEATURE_WITHOUT_TO_CARD !in session.orderFeatures) return null
            val features = session.orderFeatures
            val saveCardOnly = FEATURE_VERIFY in features
            val forced = saveCardOnly || FEATURE_FORCE_CREATE_BINDING in features
            val offered = session.savingAllowed && OPTION_DEBIT_BINDING_ALLOWED in session.merchantOptions && !saveCardOnly
            val saveCard =
                when {
                    forced && offered -> SaveCard.LOCKED
                    forced -> SaveCard.IMPLIED
                    offered -> SaveCard.OFFERED
                    else -> SaveCard.HIDDEN
                }
            return Transfer(saveCard, session.cvcBindingNotRequired, saveCardOnly)
        }

        /**
         * Saved cards a transfer may debit: debit bindings of a buyer the merchant keeps cards for. `BINDING_NOT_NEEDED`
         * forbids saving a new card only; an order that only saves a card offers none.
         */
        fun debitCards(session: SessionStatusResponse): List<BindingItem> =
            if (session.bindingEnabled && FEATURE_VERIFY !in session.orderFeatures) {
                session.bindingItems.filter { it.bindingCategory == CATEGORY_DEBIT }
            } else {
                emptyList()
            }

        private val SessionStatusResponse.isP2P: Boolean get() = orderPaymentWay.equals(PAYMENT_WAY_P2P, ignoreCase = true)

        /** A new card may be saved: the merchant keeps this buyer's cards and the order does not forbid it. */
        private val SessionStatusResponse.savingAllowed: Boolean
            get() = bindingEnabled && FEATURE_BINDING_NOT_NEEDED !in orderFeatures
    }
}

/**
 * The cardholder sealed into a transfer's seToken: the P2P gateway refuses an empty one (`[fromCard.cardholderName]
 * empty`), unlike `processform.do`, which takes it beside the token. The name typed on the card, else the sender's name
 * from the transfer fields, else `CARDHOLDER`, as the payment does.
 */
internal fun transferCardholder(
    typed: String?,
    payerData: Map<PayerField, String>,
): String =
    typed?.trim()?.takeIf { it.isNotEmpty() }
        ?: listOfNotNull(payerData[PayerField.PAYER_FIRST_NAME], payerData[PayerField.PAYER_LAST_NAME])
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .takeIf { it.isNotEmpty() }
        ?: "CARDHOLDER"
