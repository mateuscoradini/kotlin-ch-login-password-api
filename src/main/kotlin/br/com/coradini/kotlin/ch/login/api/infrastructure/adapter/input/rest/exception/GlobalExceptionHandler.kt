package br.com.coradini.kotlin.ch.login.api.infrastructure.adapter.input.rest.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val message = exception.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
            .ifBlank { "Invalid request payload" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiError(VALIDATION_ERROR, message))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(exception: HttpMessageNotReadableException): ResponseEntity<ApiError> {
        logger.debug("Rejected malformed request body: {}", exception.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiError(MALFORMED_BODY, "Request body is missing or malformed"))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception): ResponseEntity<ApiError> {
        logger.error("Unexpected exception while handling request", exception)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError(INTERNAL_ERROR, "Unexpected error"))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

        const val VALIDATION_ERROR = "VALIDATION_ERROR"
        const val MALFORMED_BODY = "MALFORMED_BODY"
        const val INTERNAL_ERROR = "INTERNAL_ERROR"
    }
}
