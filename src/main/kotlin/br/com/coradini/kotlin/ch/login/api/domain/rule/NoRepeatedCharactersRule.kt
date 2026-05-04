package br.com.coradini.kotlin.ch.login.api.domain.rule

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import org.springframework.stereotype.Component

@Component
class NoRepeatedCharactersRule : PasswordRule {
    override val violation: RuleViolation = RuleViolation.REPEATED_CHARACTERS

    override fun isSatisfiedBy(password: Password): Boolean =
        password.value.toSet().size == password.length
}
