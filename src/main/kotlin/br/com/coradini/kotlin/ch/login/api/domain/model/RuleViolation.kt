package br.com.coradini.kotlin.ch.login.api.domain.model

enum class RuleViolation {
    MIN_LENGTH,
    NO_DIGIT,
    NO_LOWERCASE,
    NO_UPPERCASE,
    NO_SPECIAL_CHARACTER,
    REPEATED_CHARACTERS,
    CONTAINS_WHITESPACE
}
