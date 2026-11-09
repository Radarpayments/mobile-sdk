package net.payrdr.mobile.payment.sdk.payment.gpay

/**
 * Authorization methods supported by Google Pay. Values match the strings serialized
 * into `allowedAuthMethods` in a Google Pay PaymentDataRequest.
 *
 * - [PAN_ONLY] — PAN is tokenized server-side; the issuer receives a virtual account number.
 * - [CRYPTOGRAM_3DS] — device-bound cryptogram; issuer receives the DPAN + cryptogram.
 */
public enum class GooglePayAuthMethod(
    public val value: String,
) {
    PAN_ONLY("PAN_ONLY"),
    CRYPTOGRAM_3DS("CRYPTOGRAM_3DS"),
}
