package br.com.coradini.kotlin.ch.login.api.domain.rule

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import org.springframework.stereotype.Component

@Component
class ContainsSpecialCharacterRule : PasswordRule {
    override val violation: RuleViolation = RuleViolation.NO_SPECIAL_CHARACTER

    override fun isSatisfiedBy(password: Password): Boolean =
        password.characters().any { it in SPECIAL_CHARACTERS }

    companion object {
        val SPECIAL_CHARACTERS: Set<Char> = "!@#\$%^&*()-+".toSet()
    }
}
