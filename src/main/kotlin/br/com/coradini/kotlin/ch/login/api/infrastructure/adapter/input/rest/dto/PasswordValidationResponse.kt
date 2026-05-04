package br.com.coradini.kotlin.ch.login.api.infrastructure.adapter.input.rest.dto

import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import io.swagger.v3.oas.annotations.media.Schema

data class PasswordValidationResponse(
    @field:Schema(description = "Whether the password satisfies all validation rules", example = "true")
    val valid: Boolean,
    @field:Schema(
        description = "Codes of every rule the password violated. Empty when valid is true.",
        example = "[]"
    )
    val violations: List<RuleViolation>
)
