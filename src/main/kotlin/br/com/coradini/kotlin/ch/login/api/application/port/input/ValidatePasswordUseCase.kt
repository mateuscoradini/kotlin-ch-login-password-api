package br.com.coradini.kotlin.ch.login.api.application.port.input

import br.com.coradini.kotlin.ch.login.api.domain.model.ValidationResult

interface ValidatePasswordUseCase {
    fun perform(rawPassword: String): ValidationResult
}
