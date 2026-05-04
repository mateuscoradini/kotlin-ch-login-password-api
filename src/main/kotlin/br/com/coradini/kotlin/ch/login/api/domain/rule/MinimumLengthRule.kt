package br.com.coradini.kotlin.ch.login.api.domain.rule

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import org.springframework.stereotype.Component

@Component
class MinimumLengthRule : PasswordRule {
    override val violation: RuleViolation = RuleViolation.MIN_LENGTH

    override fun isSatisfiedBy(password: Password): Boolean =
        password.length >= MINIMUM_LENGTH

    companion object {
        const val MINIMUM_LENGTH = 9
    }
}
