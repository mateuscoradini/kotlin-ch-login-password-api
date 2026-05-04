package br.com.coradini.kotlin.ch.login.api.domain.rule

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import org.springframework.stereotype.Component

@Component
class NoWhitespaceRule : PasswordRule {
    override val violation: RuleViolation = RuleViolation.CONTAINS_WHITESPACE

    override fun isSatisfiedBy(password: Password): Boolean =
        password.characters().none { it.isWhitespace() }
}
