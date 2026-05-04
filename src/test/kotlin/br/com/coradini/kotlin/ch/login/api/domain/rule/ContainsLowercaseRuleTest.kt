package br.com.coradini.kotlin.ch.login.api.domain.rule

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ContainsLowercaseRuleTest {

    private val rule = ContainsLowercaseRule()

    @Nested
    inner class IsSatisfiedByMethod {

        @Test
        fun `isSatisfiedBy - should return true when password contains at least one lowercase letter`() {
            val result = rule.isSatisfiedBy(Password("AbTp9!fok"))

            result `should be equal to` true
        }

        @Test
        fun `isSatisfiedBy - should return false when password has only uppercase letters and digits`() {
            val result = rule.isSatisfiedBy(Password("ABTP9!FOK"))

            result `should be equal to` false
        }

        @Test
        fun `isSatisfiedBy - should return false when password is empty`() {
            val result = rule.isSatisfiedBy(Password(""))

            result `should be equal to` false
        }
    }

    @Nested
    inner class ViolationProperty {

        @Test
        fun `violation - should expose NO_LOWERCASE code`() {
            rule.violation `should be equal to` RuleViolation.NO_LOWERCASE
        }
    }
}
