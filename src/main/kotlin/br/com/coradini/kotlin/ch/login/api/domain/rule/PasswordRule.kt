package br.com.coradini.kotlin.ch.login.api.domain.rule

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation

interface PasswordRule {
    val violation: RuleViolation
    fun isSatisfiedBy(password: Password): Boolean
}
