package net.payrdr.mobile.payment.sdk.core.model

/**
 * Card identifier — either by card number (PAN) or by stored binding id.
 */
internal sealed class CardIdentifier {
    abstract val value: String
}

/**
 * Identifier by card number.
 */
internal data class CardPanIdentifier(
    override val value: String,
) : CardIdentifier()

/**
 * Identifier by binding id.
 */
internal data class CardBindingIdIdentifier(
    override val value: String,
) : CardIdentifier()
