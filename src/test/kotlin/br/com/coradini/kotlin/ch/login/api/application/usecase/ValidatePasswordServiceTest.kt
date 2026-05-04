package br.com.coradini.kotlin.ch.login.api.application.usecase

import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import br.com.coradini.kotlin.ch.login.api.domain.model.ValidationResult
import br.com.coradini.kotlin.ch.login.api.domain.service.PasswordValidator
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.amshove.kluent.`should be empty`
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ValidatePasswordServiceTest {

    private val passwordValidator: PasswordValidator = mock()
    private lateinit var meterRegistry: MeterRegistry
    private lateinit var service: ValidatePasswordService

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
        service = ValidatePasswordService(passwordValidator, meterRegistry)
    }

    @Nested
    inner class PerformMethod {

        @Test
        fun `perform - should wrap the raw password into the Password value object before validating`() {
            val rawPassword = "AbTp9!fok"
            whenever(passwordValidator.validate(eq(Password(rawPassword)))).thenReturn(ValidationResult.VALID)

            service.perform(rawPassword)

            verify(passwordValidator).validate(eq(Password(rawPassword)))
        }

        @Test
        fun `perform - should return the validation result produced by the validator`() {
            val rawPassword = "AbTp9!foo"
            val expected = ValidationResult(listOf(RuleViolation.REPEATED_CHARACTERS))
            whenever(passwordValidator.validate(eq(Password(rawPassword)))).thenReturn(expected)

            val result = service.perform(rawPassword)

            result `should be equal to` expected
        }

        @Test
        fun `perform - should return a valid result when the validator reports no violations`() {
            val rawPassword = "AbTp9!fok"
            whenever(passwordValidator.validate(eq(Password(rawPassword)))).thenReturn(ValidationResult.VALID)

            val result = service.perform(rawPassword)

            result.valid `should be equal to` true
            result.violations.`should be empty`()
        }
    }

    @Nested
    inner class Metrics {

        @Test
        fun `perform - should increment validations counter with valid result tag for valid passwords`() {
            whenever(passwordValidator.validate(eq(Password("ok")))).thenReturn(ValidationResult.VALID)

            service.perform("ok")

            validationsCounter(ValidatePasswordService.RESULT_VALID).count() `should be equal to` 1.0
            validationsCounter(ValidatePasswordService.RESULT_INVALID).count() `should be equal to` 0.0
        }

        @Test
        fun `perform - should increment validations counter with invalid result tag and per-rule violations counter`() {
            val violations = listOf(RuleViolation.MIN_LENGTH, RuleViolation.NO_DIGIT)
            whenever(passwordValidator.validate(eq(Password("bad")))).thenReturn(ValidationResult(violations))

            service.perform("bad")

            validationsCounter(ValidatePasswordService.RESULT_INVALID).count() `should be equal to` 1.0
            validationsCounter(ValidatePasswordService.RESULT_VALID).count() `should be equal to` 0.0
            violationsCounter(RuleViolation.MIN_LENGTH).count() `should be equal to` 1.0
            violationsCounter(RuleViolation.NO_DIGIT).count() `should be equal to` 1.0
            violationsCounter(RuleViolation.NO_LOWERCASE).count() `should be equal to` 0.0
        }


        private fun validationsCounter(resultTag: String) = meterRegistry
            .counter(ValidatePasswordService.METRIC_VALIDATIONS, ValidatePasswordService.TAG_RESULT, resultTag)

        private fun violationsCounter(rule: RuleViolation) = meterRegistry
            .counter(ValidatePasswordService.METRIC_VIOLATIONS, ValidatePasswordService.TAG_RULE, rule.name)
    }
}
