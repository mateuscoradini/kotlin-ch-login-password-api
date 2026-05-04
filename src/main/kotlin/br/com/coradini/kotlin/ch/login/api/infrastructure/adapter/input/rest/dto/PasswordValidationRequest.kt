package br.com.coradini.kotlin.ch.login.api.infrastructure.adapter.input.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class PasswordValidationRequest(
    @field:NotNull
    @field:Schema(description = "Password to be validated", example = "AbTp9!fok", required = true)
    val password: String?
)
