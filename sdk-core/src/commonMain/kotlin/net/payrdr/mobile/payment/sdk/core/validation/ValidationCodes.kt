package net.payrdr.mobile.payment.sdk.core.validation

/**
 * Stable string codes for validation errors.
 *
 * These codes are part of the public SDK API — merchants map them to their own localized strings.
 * Property names are intentionally lowercase to match the wire-format error code strings.
 */
@Suppress("PropertyName")
public object ValidationCodes {
    public const val required: String = "required"
    public const val invalid: String = "invalid"
    public const val invalidFormat: String = "invalid-format"
}
