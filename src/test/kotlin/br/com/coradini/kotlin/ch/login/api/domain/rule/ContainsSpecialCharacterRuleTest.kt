package br.com.coradini.kotlin.ch.login.api.domain.rule

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ContainsSpecialCharacterRuleTest {

    private val rule = ContainsSpecialCharacterRule()

    @Nested
    inner class IsSatisfiedByMethod {

        @ParameterizedTest
        @ValueSource(strings = ["AbTp9!fok", "AbTp9@fok", "AbTp9#fok", "AbTp9\$fok", "AbTp9%fok", "AbTp9^fok", "AbTp9&fok", "AbTp9*fok", "AbTp9(fok", "AbTp9)fok", "AbTp9-fok", "AbTp9+fok"])
        fun `isSatisfiedBy - should return true when password contains an allowed special character`(rawPassword: String) {
            val result = rule.isSatisfiedBy(Password(rawPassword))

            result `should be equal to` true
        }

        @Test
        fun `isSatisfiedBy - should return false when password has no special character`() {
            val result = rule.isSatisfiedBy(Password("AbTp9xfok"))

            result `should be equal to` false
        }

        @Test
        fun `isSatisfiedBy - should return false when password contains a non-allowed special character`() {
            val result = rule.isSatisfiedBy(Password("AbTp9?fok"))

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
        fun `violation - should expose NO_SPECIAL_CHARACTER code`() {
            rule.violation `should be equal to` RuleViolation.NO_SPECIAL_CHARACTER
        }
    }
}
