package br.com.coradini.kotlin.ch.login.api.domain.rule

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import org.springframework.stereotype.Component

@Component
class ContainsLowercaseRule : PasswordRule {
    override val violation: RuleViolation = RuleViolation.NO_LOWERCASE

    override fun isSatisfiedBy(password: Password): Boolean =
        password.characters().any { it.isLowerCase() }
}
