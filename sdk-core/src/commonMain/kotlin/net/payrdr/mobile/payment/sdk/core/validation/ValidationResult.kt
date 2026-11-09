package net.payrdr.mobile.payment.sdk.core.validation

/**
 * Outcome of a single validation rule evaluation.
 *
 * @property isValid true if the data satisfies the rule
 * @property errorCode stable error code (see [ValidationCodes]) or null if valid
 * @property errorMessage human-readable debug message (English) or null if valid
 */
internal data class ValidationResult(
    val isValid: Boolean,
    val errorCode: String?,
    val errorMessage: String?,
) {
    companion object {
        val VALID: ValidationResult = ValidationResult(true, null, null)

        fun error(
            errorCode: String,
            errorMessage: String,
        ): ValidationResult = ValidationResult(false, errorCode, errorMessage)
    }
}
