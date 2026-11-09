package net.payrdr.mobile.payment.sdk.core.model

/**
 * Sealed root for all card-payment parameter variants.
 *
 * @property pubKey PEM-encoded RSA public key used to seal the cryptogram
 */
public sealed class PaymentCardParams(
    public open val pubKey: String,
) {
    /**
     * Parameters for a previously saved (bound) card.
     */
    public sealed class StoredCardPaymentParams(
        public open val cvc: String?,
        override val pubKey: String,
    ) : PaymentCardParams(pubKey = pubKey)

    /**
     * Parameters for a newly entered (unsaved) card.
     */
    public sealed class NewCardPaymentParams(
        public open val pan: String,
        public open val cvc: String,
        public open val expiryMMYY: String,
        public open val cardHolder: String?,
        override val pubKey: String,
    ) : PaymentCardParams(pubKey = pubKey)
}
