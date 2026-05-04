package br.com.coradini.kotlin.ch.login.api.domain.rule

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import org.springframework.stereotype.Component

@Component
class ContainsDigitRule : PasswordRule {
    override val violation: RuleViolation = RuleViolation.NO_DIGIT

    override fun isSatisfiedBy(password: Password): Boolean =
        password.characters().any { it.isDigit() }
}
