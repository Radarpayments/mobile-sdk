package net.payrdr.mobile.payment.sdk.payment.gpay

/**
 * Card networks Google Pay can authorize. Values match the strings serialized into
 * `allowedCardNetworks` in a Google Pay PaymentDataRequest.
 */
public enum class GooglePayCardNetwork(
    public val value: String,
) {
    AMEX("AMEX"),
    DISCOVER("DISCOVER"),
    INTERAC("INTERAC"),
    JCB("JCB"),
    MASTERCARD("MASTERCARD"),
    VISA("VISA"),
}
