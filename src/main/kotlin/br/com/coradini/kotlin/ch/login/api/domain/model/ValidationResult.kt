package br.com.coradini.kotlin.ch.login.api.domain.model

data class ValidationResult(val violations: List<RuleViolation>) {
    val valid: Boolean
        get() = violations.isEmpty()

    companion object {
        val VALID: ValidationResult = ValidationResult(emptyList())
    }
}
