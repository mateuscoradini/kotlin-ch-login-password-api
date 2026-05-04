package br.com.coradini.kotlin.ch.login.api.domain.rule

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoWhitespaceRuleTest {

    private val rule = NoWhitespaceRule()

    @Nested
    inner class IsSatisfiedByMethod {

        @Test
        fun `isSatisfiedBy - should return true when password has no whitespace`() {
            val result = rule.isSatisfiedBy(Password("AbTp9!fok"))

            result `should be equal to` true
        }

        @Test
        fun `isSatisfiedBy - should return false when password contains a space`() {
            val result = rule.isSatisfiedBy(Password("AbTp9 fok"))

            result `should be equal to` false
        }

        @Test
        fun `isSatisfiedBy - should return false when password contains a tab character`() {
            val result = rule.isSatisfiedBy(Password("AbTp9\tfok"))

            result `should be equal to` false
        }

        @Test
        fun `isSatisfiedBy - should return true when password is empty`() {
            val result = rule.isSatisfiedBy(Password(""))

            result `should be equal to` true
        }
    }

    @Nested
    inner class ViolationProperty {

        @Test
        fun `violation - should expose CONTAINS_WHITESPACE code`() {
            rule.violation `should be equal to` RuleViolation.CONTAINS_WHITESPACE
        }
    }
}
