package br.com.coradini.kotlin.ch.login.api.domain.rule

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MinimumLengthRuleTest {

    private val rule = MinimumLengthRule()

    @Nested
    inner class IsSatisfiedByMethod {

        @Test
        fun `isSatisfiedBy - should return true when password has exactly 9 characters`() {
            val result = rule.isSatisfiedBy(Password("AbTp9!fok"))

            result `should be equal to` true
        }

        @Test
        fun `isSatisfiedBy - should return true when password has more than 9 characters`() {
            val result = rule.isSatisfiedBy(Password("AbTp9!fokX"))

            result `should be equal to` true
        }

        @Test
        fun `isSatisfiedBy - should return false when password has less than 9 characters`() {
            val result = rule.isSatisfiedBy(Password("AbTp9!fo"))

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
        fun `violation - should expose MIN_LENGTH code`() {
            rule.violation `should be equal to` RuleViolation.MIN_LENGTH
        }
    }
}
