package br.com.coradini.kotlin.ch.login.api.infrastructure.adapter.input.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class PasswordValidationControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `validate - should return valid true with empty violations for the canonical valid example`() {
        validatePassword("AbTp9!fok")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.violations").isEmpty)
    }

    @Test
    fun `validate - should return valid false when password is empty`() {
        validatePassword("")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.violations", org.hamcrest.Matchers.hasItem("MIN_LENGTH")))
    }

    @Test
    fun `validate - should return valid false when password has only two lowercase letters`() {
        validatePassword("aa")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.violations", org.hamcrest.Matchers.hasItem("MIN_LENGTH")))
            .andExpect(jsonPath("$.violations", org.hamcrest.Matchers.hasItem("REPEATED_CHARACTERS")))
    }

    @Test
    fun `validate - should return valid false when password has two distinct lowercase letters but lacks every other rule`() {
        validatePassword("ab")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.violations", org.hamcrest.Matchers.hasItem("MIN_LENGTH")))
            .andExpect(jsonPath("$.violations", org.hamcrest.Matchers.hasItem("NO_DIGIT")))
            .andExpect(jsonPath("$.violations", org.hamcrest.Matchers.hasItem("NO_UPPERCASE")))
            .andExpect(jsonPath("$.violations", org.hamcrest.Matchers.hasItem("NO_SPECIAL_CHARACTER")))
    }

    @Test
    fun `validate - should return valid false when password mixes letters and has repeated characters`() {
        validatePassword("AAAbbbCc")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.violations", org.hamcrest.Matchers.hasItem("MIN_LENGTH")))
            .andExpect(jsonPath("$.violations", org.hamcrest.Matchers.hasItem("NO_DIGIT")))
            .andExpect(jsonPath("$.violations", org.hamcrest.Matchers.hasItem("NO_SPECIAL_CHARACTER")))
            .andExpect(jsonPath("$.violations", org.hamcrest.Matchers.hasItem("REPEATED_CHARACTERS")))
    }

    @Test
    fun `validate - should return valid false when password has a repeated lowercase letter`() {
        validatePassword("AbTp9!foo")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.violations", org.hamcrest.Matchers.contains("REPEATED_CHARACTERS")))
    }

    @Test
    fun `validate - should return valid false when password has a repeated uppercase letter`() {
        validatePassword("AbTp9!foA")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.violations", org.hamcrest.Matchers.contains("REPEATED_CHARACTERS")))
    }

    @Test
    fun `validate - should return valid false when password contains a whitespace`() {
        validatePassword("AbTp9 fok")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.violations", org.hamcrest.Matchers.contains("CONTAINS_WHITESPACE", "NO_SPECIAL_CHARACTER")))
    }

    @Test
    fun `validate - should return 400 when request body is missing`() {
        mockMvc.perform(
            post("/api/v1/passwords/validate")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("MALFORMED_BODY"))
    }

    @Test
    fun `validate - should return 400 when password field is missing from body`() {
        mockMvc.perform(
            post("/api/v1/passwords/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
    }

    private fun validatePassword(password: String) =
        mockMvc.perform(
            post("/api/v1/passwords/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("password" to password)))
        )
}
