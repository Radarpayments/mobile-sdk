package net.payrdr.mobile.payment.sdk.payment.keyprovider

import net.payrdr.mobile.payment.sdk.payment.exception.SdkException

/** Signals that the active RSA public key could not be fetched or parsed. */
internal class KeyProviderException(
    message: String,
    cause: Throwable? = null,
) : SdkException(message, cause)
