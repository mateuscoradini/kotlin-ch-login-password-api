package br.com.coradini.kotlin.ch.login.api.domain.service

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import br.com.coradini.kotlin.ch.login.api.domain.rule.PasswordRule
import org.amshove.kluent.`should be empty`
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class PasswordValidatorTest {

    private val anyPassword = Password("anything")

    @Nested
    inner class ValidateMethod {

        @Test
        fun `validate - should return a valid result when every rule is satisfied`() {
            val firstRule = stubRule(satisfied = true, violation = RuleViolation.MIN_LENGTH)
            val secondRule = stubRule(satisfied = true, violation = RuleViolation.NO_DIGIT)
            val validator = PasswordValidator(listOf(firstRule, secondRule))

            val result = validator.validate(anyPassword)

            result.valid `should be equal to` true
            result.violations.`should be empty`()
        }

        @Test
        fun `validate - should aggregate violations from every rule that is not satisfied in alphabetical order`() {
            val failingDigit = stubRule(satisfied = false, violation = RuleViolation.NO_DIGIT)
            val failingLength = stubRule(satisfied = false, violation = RuleViolation.MIN_LENGTH)
            val passingUppercase = stubRule(satisfied = true, violation = RuleViolation.NO_UPPERCASE)
            val validator = PasswordValidator(listOf(failingDigit, failingLength, passingUppercase))

            val result = validator.validate(anyPassword)

            result.valid `should be equal to` false
            result.violations `should be equal to` listOf(RuleViolation.MIN_LENGTH, RuleViolation.NO_DIGIT)
        }

        @Test
        fun `validate - should return a valid result when there are no rules configured`() {
            val validator = PasswordValidator(emptyList())

            val result = validator.validate(anyPassword)

            result.valid `should be equal to` true
            result.violations.`should be empty`()
        }
    }

    private fun stubRule(satisfied: Boolean, violation: RuleViolation): PasswordRule = mock {
        on { isSatisfiedBy(anyPassword) } doReturn satisfied
        on { this.violation } doReturn violation
    }
}
