package br.com.coradini.kotlin.ch.login.api.infrastructure.adapter.input.rest

import br.com.coradini.kotlin.ch.login.api.application.port.input.ValidatePasswordUseCase
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import br.com.coradini.kotlin.ch.login.api.domain.model.ValidationResult
import br.com.coradini.kotlin.ch.login.api.infrastructure.adapter.input.rest.dto.PasswordValidationRequest
import org.amshove.kluent.`should be empty`
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PasswordValidationControllerTest {

    private val useCase: ValidatePasswordUseCase = mock()
    private val controller = PasswordValidationController(useCase)

    @Nested
    inner class ValidateMethod {

        @Test
        fun `validate - should pass the raw password to the use case when present`() {
            val rawPassword = "AbTp9!fok"
            whenever(useCase.perform(eq(rawPassword))).thenReturn(ValidationResult.VALID)

            val response = controller.validate(PasswordValidationRequest(password = rawPassword))

            verify(useCase).perform(eq(rawPassword))
            response.valid `should be equal to` true
            response.violations.`should be empty`()
        }

        @Test
        fun `validate - should pass empty string to the use case when request password is null`() {
            whenever(useCase.perform(any())).thenReturn(ValidationResult.VALID)

            controller.validate(PasswordValidationRequest(password = null))

            verify(useCase).perform(eq(""))
        }

        @Test
        fun `validate - should map every violation returned by the use case into the response`() {
            val rawPassword = "AbTp9!foo"
            val expected = ValidationResult(listOf(RuleViolation.REPEATED_CHARACTERS))
            whenever(useCase.perform(eq(rawPassword))).thenReturn(expected)

            val response = controller.validate(PasswordValidationRequest(password = rawPassword))

            response.valid `should be equal to` false
            response.violations `should be equal to` listOf(RuleViolation.REPEATED_CHARACTERS)
        }
    }
}
