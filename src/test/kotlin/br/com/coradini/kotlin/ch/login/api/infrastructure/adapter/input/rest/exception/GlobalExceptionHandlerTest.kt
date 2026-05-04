package br.com.coradini.kotlin.ch.login.api.infrastructure.adapter.input.rest.exception

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Nested
    inner class HandleValidationMethod {

        @Test
        fun `handleValidation - should aggregate field errors into a comma separated message`() {
            val fieldErrors = listOf(
                FieldError("request", "password", "must not be null"),
                FieldError("request", "username", "must not be blank")
            )
            val exception = stubMethodArgumentNotValidException(fieldErrors)

            val response = handler.handleValidation(exception)

            response.statusCode.value() `should be equal to` HttpStatus.BAD_REQUEST.value()
            val body = response.body
            body.shouldNotBeNull()
            body.code `should be equal to` GlobalExceptionHandler.VALIDATION_ERROR
            body.message `should be equal to` "password: must not be null, username: must not be blank"
        }

        @Test
        fun `handleValidation - should fall back to a generic message when there are no field errors`() {
            val exception = stubMethodArgumentNotValidException(emptyList())

            val response = handler.handleValidation(exception)

            response.statusCode.value() `should be equal to` HttpStatus.BAD_REQUEST.value()
            val body = response.body
            body.shouldNotBeNull()
            body.code `should be equal to` GlobalExceptionHandler.VALIDATION_ERROR
            body.message `should be equal to` "Invalid request payload"
        }
    }

    @Nested
    inner class HandleUnreadableBodyMethod {

        @Test
        fun `handleUnreadableBody - should return 400 with MALFORMED_BODY code`() {
            val exception = HttpMessageNotReadableException("malformed", mock<HttpInputMessage>())

            val response = handler.handleUnreadableBody(exception)

            response.statusCode.value() `should be equal to` HttpStatus.BAD_REQUEST.value()
            val body = response.body
            body.shouldNotBeNull()
            body.code `should be equal to` GlobalExceptionHandler.MALFORMED_BODY
            body.message `should be equal to` "Request body is missing or malformed"
        }
    }

    @Nested
    inner class HandleUnexpectedMethod {

        @Test
        fun `handleUnexpected - should return 500 with INTERNAL_ERROR code for any uncaught exception`() {
            val response = handler.handleUnexpected(RuntimeException("boom"))

            response.statusCode.value() `should be equal to` HttpStatus.INTERNAL_SERVER_ERROR.value()
            val body = response.body
            body.shouldNotBeNull()
            body.code `should be equal to` GlobalExceptionHandler.INTERNAL_ERROR
            body.message `should be equal to` "Unexpected error"
        }
    }

    private fun stubMethodArgumentNotValidException(fieldErrors: List<FieldError>): MethodArgumentNotValidException {
        val bindingResult: BindingResult = mock {
            on { this.fieldErrors } doReturn fieldErrors
        }
        return mock {
            on { this.bindingResult } doReturn bindingResult
        }
    }
}
