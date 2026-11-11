package net.payrdr.mobile.payment.sdk.payment.presentation

import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.screens.savedCardPaymentSystems
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardFormatters
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardInputValidation
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetwork
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetworkResolver
import net.payrdr.mobile.payment.sdk.payment.ui.util.FieldError
import net.payrdr.mobile.payment.sdk.payment.ui.util.gatewayPaymentSystems
import kotlin.math.min

/**
 * What the card number typed so far says about the card.
 *
 * @property logo the network's logo, null until it is recognised.
 * @property cvvLength digits in this network's security code.
 * @property paymentSystems the gateway's names of the network, for the payer fields it asks for.
 * @property complete the number is long enough to be checked while still typing.
 * @property cvvHintKey the string that says where the security code is.
 */
@PayrdrUiApi
public class CardNumberSpec internal constructor(
    public val logo: NetworkLogoSpec?,
    public val cvvLength: Int,
    public val paymentSystems: List<String>,
    public val complete: Boolean,
    public val cvvHintKey: String,
)

/** The rules of the card fields for the SwiftUI form: the same formatters and checks the Compose form uses. */
@PayrdrUiApi
@Suppress("TooManyFunctions")
public object CardEntry {
    /** The digits of [text] a card number field keeps. */
    public fun panDigits(text: String): String {
        val digits = CardFormatters.digitsOnly(text)
        return digits.take(CardFormatters.maxPanLength(CardNetworkResolver.resolve(digits)))
    }

    /** [digits] of a card number grouped as its network prints them. */
    public fun formatPan(digits: String): String = CardFormatters.formatPan(digits)

    /** What the card number [pan] typed so far says; [dark] picks the logo's colours for a dark sheet. */
    public fun number(
        pan: String,
        dark: Boolean,
    ): CardNumberSpec =
        spec(
            CardNetworkResolver.resolve(pan),
            dark,
            complete =
                pan.length >= min(CardFormatters.maxPanLength(CardNetworkResolver.resolve(pan)), FULL_PAN),
        )

    /** The same for a saved [card], which is always complete. */
    public fun savedNumber(
        card: SavedCardInfo,
        dark: Boolean,
    ): CardNumberSpec {
        val network = CardNetworkResolver.resolveSaved(card.paymentSystem, card.maskedPan)
        return CardNumberSpec(
            logo = NetworkLogoSpec.of(network, dark),
            cvvLength = CardFormatters.cvvLength(network),
            paymentSystems = savedCardPaymentSystems(card),
            complete = true,
            cvvHintKey = hintKey(network),
        )
    }

    /** The digits of [text] an expiry field keeps: a month that can only be one digit gets its zero. */
    public fun expiryDigits(text: String): String {
        val next = CardFormatters.digitsOnly(text).take(EXPIRY_DIGITS)
        return if (next.length == 1 && next[0] > '1') "0$next" else next
    }

    /** Expiry [digits] with the slash after the month, as `MM/YY`. */
    public fun formatExpiry(digits: String): String = CardFormatters.formatExpiry(digits)

    /** The digits of [text] a security code field of [length] digits keeps. */
    public fun cvcDigits(
        text: String,
        length: Int,
    ): String = CardFormatters.digitsOnly(text).take(length)

    /** The string key of what is wrong with the card number, null when it is fine. */
    public fun panError(pan: String): String? = CardInputValidation.pan(pan)?.messageKey

    /** The string key of what is wrong with the expiry, null when it is fine. */
    public fun expiryError(expiry: String): String? = CardInputValidation.expiry(expiry)?.messageKey

    /** The string key of a security code shorter than the network's [length], null when it is complete. */
    public fun cvvError(
        cvc: String,
        length: Int,
    ): String? =
        when {
            cvc.length == length -> null
            length == AMEX_CVV -> FieldError.CVV_4.messageKey
            else -> FieldError.CVV_3.messageKey
        }

    /** A saved card's [maskedPan] shortened to its last four digits. */
    public fun shortMask(maskedPan: String): String = CardFormatters.shortMask(maskedPan)

    /** The payment with a typed card; [saveCard] asks the gateway to keep it. */
    public fun submitNewCard(
        pan: String,
        expiry: String,
        cvc: String,
        saveCard: Boolean,
        payerData: Map<PayerField, String>,
    ): PaymentIntent =
        PaymentIntent.SubmitNewCard(
            pan = pan,
            expiryMMYY = CardFormatters.formatExpiry(expiry),
            cvc = cvc,
            cardHolder = null,
            saveCard = saveCard,
            payerData = payerData,
        )

    /** The payment with a saved [card]; [cvc] is null when the gateway does not ask for it. */
    public fun submitSavedCard(
        card: SavedCardInfo,
        cvc: String?,
        payerData: Map<PayerField, String>,
    ): PaymentIntent = PaymentIntent.SubmitSavedCard(bindingId = card.bindingId, cvc = cvc, payerData = payerData)

    private fun spec(
        network: CardNetwork,
        dark: Boolean,
        complete: Boolean,
    ) = CardNumberSpec(
        logo = NetworkLogoSpec.of(network, dark),
        cvvLength = CardFormatters.cvvLength(network),
        paymentSystems = network.gatewayPaymentSystems,
        complete = complete,
        cvvHintKey = hintKey(network),
    )

    private fun hintKey(network: CardNetwork) = if (CardFormatters.cvvLength(network) == AMEX_CVV) "cvv_hint_front" else "cvv_hint_back"

    private const val FULL_PAN = 16
    private const val EXPIRY_DIGITS = 4
    private const val AMEX_CVV = 4
}

/** The string resource that words this error, as the Compose form picks it. */
internal val FieldError.messageKey: String
    get() =
        when (this) {
            FieldError.CVV_3 -> "error_cvv_3"
            FieldError.CVV_4 -> "error_cvv_4"
            else -> "error_${name.lowercase()}"
        }
