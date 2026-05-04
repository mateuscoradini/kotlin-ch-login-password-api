package br.com.coradini.kotlin.ch.login.api.infrastructure.adapter.input.rest

import br.com.coradini.kotlin.ch.login.api.application.port.input.ValidatePasswordUseCase
import br.com.coradini.kotlin.ch.login.api.infrastructure.adapter.input.rest.dto.PasswordValidationRequest
import br.com.coradini.kotlin.ch.login.api.infrastructure.adapter.input.rest.dto.PasswordValidationResponse
import br.com.coradini.kotlin.ch.login.api.infrastructure.adapter.input.rest.mapper.ValidationResultMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/passwords")
@Tag(name = "Password Validation", description = "Endpoints for validating passwords against the configured rule set")
class PasswordValidationController(
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    @PostMapping(
        "/validate",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Validate a password",
        description = "Returns whether the password is valid and, when invalid, the codes of the violated rules."
    )
    fun validate(@Valid @RequestBody request: PasswordValidationRequest): PasswordValidationResponse {
        val result = validatePasswordUseCase.perform(request.password.orEmpty())
        return ValidationResultMapper.toResponse(result)
    }
}
