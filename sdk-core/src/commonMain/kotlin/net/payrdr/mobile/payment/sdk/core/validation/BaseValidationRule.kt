package net.payrdr.mobile.payment.sdk.core.validation

/**
 * A single validation rule applied to values of type [DATA].
 */
public interface BaseValidationRule<DATA> {
    public fun validateForError(data: DATA): ValidationResult
}
