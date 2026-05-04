package br.com.coradini.kotlin.ch.login.api.domain.service

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.ValidationResult
import br.com.coradini.kotlin.ch.login.api.domain.rule.PasswordRule
import org.springframework.stereotype.Component

@Component
class PasswordValidator(private val rules: List<PasswordRule>) {
    fun validate(password: Password): ValidationResult {
        val violations = rules
            .filterNot { it.isSatisfiedBy(password) }
            .map { it.violation }
        return ValidationResult(violations)
    }
}
