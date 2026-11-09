package net.payrdr.mobile.payment.sdk.core.validation

/**
 * Base class for per-field validators.
 *
 * Rules are evaluated in registration order; the first failing rule short-circuits.
 */
internal open class BaseValidator<DATA> {
    private val rules: MutableList<BaseValidationRule<DATA>> = mutableListOf()

    protected fun addRules(vararg checker: BaseValidationRule<DATA>) {
        rules.addAll(checker)
    }

    fun validate(data: DATA): ValidationResult {
        rules.forEach { rule ->
            val result = rule.validateForError(data)
            if (!result.isValid) return result
        }
        return ValidationResult.VALID
    }
}
