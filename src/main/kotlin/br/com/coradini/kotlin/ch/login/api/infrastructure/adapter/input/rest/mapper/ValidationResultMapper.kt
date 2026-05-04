package br.com.coradini.kotlin.ch.login.api.infrastructure.adapter.input.rest.mapper

import br.com.coradini.kotlin.ch.login.api.domain.model.ValidationResult
import br.com.coradini.kotlin.ch.login.api.infrastructure.adapter.input.rest.dto.PasswordValidationResponse

object ValidationResultMapper {
    fun toResponse(result: ValidationResult): PasswordValidationResponse =
        PasswordValidationResponse(
            valid = result.valid,
            violations = result.violations
        )
}
