package br.com.coradini.kotlin.ch.login.api.domain.model

data class Password(val value: String) {
    val length: Int
        get() = value.length

    fun characters(): Sequence<Char> = value.asSequence()
}
