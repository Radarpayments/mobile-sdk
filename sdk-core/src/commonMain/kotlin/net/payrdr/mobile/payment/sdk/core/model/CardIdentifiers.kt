package net.payrdr.mobile.payment.sdk.core.model

/**
 * Card identifier — either by card number (PAN) or by stored binding id.
 */
public sealed class CardIdentifier {
    public abstract val value: String
}

/**
 * Identifier by card number.
 */
public data class CardPanIdentifier(
    override val value: String,
) : CardIdentifier()

/**
 * Identifier by binding id.
 */
public data class CardBindingIdIdentifier(
    override val value: String,
) : CardIdentifier()
