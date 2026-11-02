package net.payrdr.mobile.payment.sdk.core.model

/**
 * Card data used for token generation.
 *
 * @property identifier card identifier (PAN or binding id)
 * @property expDate expiration date, null for saved-card flows without CVV flip
 * @property cvv card security code, null if not collected
 * @property cardHolder printed cardholder name, optional
 */
public data class CardInfo(
    val identifier: CardIdentifier,
    val expDate: ExpiryDate? = null,
    val cvv: String? = null,
    val cardHolder: String? = null,
)
