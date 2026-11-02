package net.payrdr.mobile.payment.sdk.payment.exception

/**
 * Base class for all SDK-thrown exceptions.
 */
public open class SdkException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Payment backend returned an API-level error.
 *
 * @property httpStatus HTTP status code, or null when the error was client-side before response
 * @property errorCode wire-protocol error code returned in the JSON body, or null
 */
public class SdkPaymentApiException(
    public val httpStatus: Int? = null,
    public val errorCode: String? = null,
    message: String? = null,
    cause: Throwable? = null,
) : SdkException(message, cause)

/**
 * Low-level networking failure (no response, timeout, SSL, DNS, etc.).
 */
public class SdkNetworkException(
    message: String? = null,
    cause: Throwable? = null,
) : SdkException(message, cause)

/**
 * Payment was declined by the issuer or the gateway.
 */
public class SdkDeclinedException(
    public val declineCode: String? = null,
    message: String? = null,
    cause: Throwable? = null,
) : SdkException(message, cause)

/**
 * Transaction-level failure (initialization error, malformed transaction state).
 */
public class SdkTransactionException(
    message: String? = null,
    cause: Throwable? = null,
) : SdkException(message, cause)

/**
 * Attempt to pay an already-paid order.
 */
public class SdkAlreadyPaidException(
    public val paymentId: String,
) : SdkException("Payment $paymentId already completed")

/**
 * Session lookup failed — session id does not exist on the server.
 */
public class SdkSessionNotFoundException(
    public val sessionId: String,
) : SdkException("Session $sessionId not found")

/**
 * Order lookup failed — order id does not exist on the server.
 */
public class SdkOrderNotFoundException(
    public val mdOrder: String,
) : SdkException("Order $mdOrder not found")

/**
 * The time to pay ran out (the order's session expired) before the payment reached the gateway: the gateway declined
 * the order and nothing was charged. It cannot be paid any more; register a new order to pay again.
 */
public class SdkSessionExpiredException(
    public val mdOrder: String,
) : SdkException("The session of order $mdOrder expired")

/**
 * Session id is malformed (not a valid `ps_` Base58 encoded string).
 */
public class SdkInvalidSessionIdException(
    public val sessionId: String,
) : SdkException("Session id malformed: $sessionId")

/**
 * Cryptogram generation failure.
 */
public class SdkCryptogramException(
    message: String? = null,
    cause: Throwable? = null,
) : SdkException(message, cause)

/**
 * Public key could not be loaded, parsed, or used.
 */
public class SdkInvalidPublicKeyException(
    message: String? = null,
    cause: Throwable? = null,
) : SdkException(message, cause)

/**
 * Merchant is missing required configuration — most notably the deprecated 3DS2 native SDK,
 * for which this SDK has no fallback. The caller should surface this as a hard failure.
 */
public class SdkNotConfiguredException(
    message: String,
) : SdkException(message)

/**
 * User cancelled the payment (backed out of the sheet, dismissed 3DS, etc.).
 */
public class SdkCancelledByUserException : SdkException("Payment cancelled by user")

/**
 * The 3DS v1 web-challenge flow failed — ACS returned error, redirect contained non-success
 * params, or SSL error occurred inside the WebView.
 */
public class SdkThreeDsChallengeFailedException(
    public val reason: String,
) : SdkException("3DS challenge failed: $reason")

/**
 * The 3DS v1 web-challenge took longer than the allowed window (default 5 min).
 */
public class SdkThreeDsTimeoutException : SdkException("3DS challenge timed out")

/** Google Pay sheet / tokenization failure. */
public class SdkGooglePayException(
    message: String? = null,
    cause: Throwable? = null,
) : SdkException(message, cause)

/** Apple Pay sheet / tokenization failure. */
public class SdkApplePayException(
    message: String? = null,
    cause: Throwable? = null,
) : SdkException(message, cause)
