package net.payrdr.mobile.payment.sdk.core.validation

/**
 * A single validation rule applied to values of type [DATA].
 */
internal interface BaseValidationRule<DATA> {
    fun validateForError(data: DATA): ValidationResult
}
