package br.com.coradini.kotlin.ch.login.api.domain.rule

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoRepeatedCharactersRuleTest {

    private val rule = NoRepeatedCharactersRule()

    @Nested
    inner class IsSatisfiedByMethod {

        @Test
        fun `isSatisfiedBy - should return true when password has no repeated characters`() {
            val result = rule.isSatisfiedBy(Password("AbTp9!fok"))

            result `should be equal to` true
        }

        @Test
        fun `isSatisfiedBy - should return false when password has lowercase repetition`() {
            val result = rule.isSatisfiedBy(Password("AbTp9!foo"))

            result `should be equal to` false
        }

        @Test
        fun `isSatisfiedBy - should return false when password has uppercase repetition`() {
            val result = rule.isSatisfiedBy(Password("AbTp9!foA"))

            result `should be equal to` false
        }

        @Test
        fun `isSatisfiedBy - should return true when password is empty`() {
            val result = rule.isSatisfiedBy(Password(""))

            result `should be equal to` true
        }

        @Test
        fun `isSatisfiedBy - should treat lowercase and uppercase as distinct characters`() {
            val result = rule.isSatisfiedBy(Password("Aa"))

            result `should be equal to` true
        }
    }

    @Nested
    inner class ViolationProperty {

        @Test
        fun `violation - should expose REPEATED_CHARACTERS code`() {
            rule.violation `should be equal to` RuleViolation.REPEATED_CHARACTERS
        }
    }
}
