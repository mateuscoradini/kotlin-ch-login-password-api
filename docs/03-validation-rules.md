# 03 — Regras de validação

As 7 regras seguem fielmente o `README-INSTRUCTIONS.md`. Cada uma é uma classe
em `domain/rule/` que implementa `PasswordRule` e expõe um código no enum
`RuleViolation`.

## Resumo em uma tabela

| # | Regra | Classe | Código (enum `RuleViolation`) |
|---|---|---|---|
| 1 | Comprimento ≥ 9 caracteres | `MinimumLengthRule` | `MIN_LENGTH` |
| 2 | Pelo menos 1 dígito | `ContainsDigitRule` | `NO_DIGIT` |
| 3 | Pelo menos 1 minúscula | `ContainsLowercaseRule` | `NO_LOWERCASE` |
| 4 | Pelo menos 1 maiúscula | `ContainsUppercaseRule` | `NO_UPPERCASE` |
| 5 | Pelo menos 1 caractere especial (`!@#$%^&*()-+`) | `ContainsSpecialCharacterRule` | `NO_SPECIAL_CHARACTER` |
| 6 | Sem caracteres repetidos | `NoRepeatedCharactersRule` | `REPEATED_CHARACTERS` |
| 7 | Sem espaços em branco | `NoWhitespaceRule` | `CONTAINS_WHITESPACE` |

## Detalhe de cada regra

### 1. `MinimumLengthRule`

```kotlin
override fun isSatisfiedBy(password: Password): Boolean =
    password.length >= MINIMUM_LENGTH

companion object { const val MINIMUM_LENGTH = 9 }
```

Constante extraída para `companion object` para deixar explícito o limiar e
facilitar reuso/teste.

### 2. `ContainsDigitRule`

```kotlin
override fun isSatisfiedBy(password: Password): Boolean =
    password.characters().any { it.isDigit() }
```

Usa `Char.isDigit()` da stdlib (cobre `0-9` ASCII e dígitos Unicode equivalentes).

### 3. `ContainsLowercaseRule` / 4. `ContainsUppercaseRule`

Idênticas em forma à regra de dígito, com `isLowerCase()` e `isUpperCase()`.
Cobertura Unicode-aware da stdlib.

### 5. `ContainsSpecialCharacterRule`

```kotlin
override fun isSatisfiedBy(password: Password): Boolean =
    password.characters().any { it in SPECIAL_CHARACTERS }

companion object { val SPECIAL_CHARACTERS: Set<Char> = "!@#\$%^&*()-+".toSet() }
```

O conjunto de especiais é **fechado e exato** (12 caracteres listados no
enunciado). `Set<Char>` garante busca O(1). O `\$` no literal é apenas escape de
template Kotlin — o caractere armazenado é `$`.

### 6. `NoRepeatedCharactersRule`

```kotlin
override fun isSatisfiedBy(password: Password): Boolean =
    password.value.toSet().size == password.length
```

Implementação canônica: nenhuma repetição ⇔ `|set(chars)| == |chars|`.
Diferença de caso (`'A'` vs `'a'`) é tratada como caracteres distintos — o
enunciado não menciona case-insensitivity. Coberto por
`isSatisfiedBy - should treat lowercase and uppercase as distinct characters`.

### 7. `NoWhitespaceRule`

```kotlin
override fun isSatisfiedBy(password: Password): Boolean =
    password.characters().none { it.isWhitespace() }
```

`Char.isWhitespace()` cobre espaço, tab, line breaks etc. Esta é a tradução
direta da nota do enunciado:
> *"Espaços em branco não devem ser considerados como caracteres válidos."*

## Exemplos do enunciado, mapeados

| Senha | Resultado | Por quê (códigos retornados) |
|---|---|---|
| `""` | inválida | `MIN_LENGTH`, `NO_DIGIT`, `NO_LOWERCASE`, `NO_UPPERCASE`, `NO_SPECIAL_CHARACTER` |
| `"aa"` | inválida | `MIN_LENGTH`, `NO_DIGIT`, `NO_UPPERCASE`, `NO_SPECIAL_CHARACTER`, `REPEATED_CHARACTERS` |
| `"ab"` | inválida | `MIN_LENGTH`, `NO_DIGIT`, `NO_UPPERCASE`, `NO_SPECIAL_CHARACTER` |
| `"AAAbbbCc"` | inválida | `MIN_LENGTH`, `NO_DIGIT`, `NO_SPECIAL_CHARACTER`, `REPEATED_CHARACTERS` |
| `"AbTp9!foo"` | inválida | `REPEATED_CHARACTERS` |
| `"AbTp9!foA"` | inválida | `REPEATED_CHARACTERS` |
| `"AbTp9 fok"` | inválida | `NO_SPECIAL_CHARACTER`, `CONTAINS_WHITESPACE` |
| `"AbTp9!fok"` | **válida** | (nenhum) |

Todos os 8 exemplos estão cobertos pelo `PasswordValidationControllerIntegrationTest`.

## Como adicionar uma 8ª regra (Open/Closed na prática)

1. Criar `domain/rule/SuaRegra.kt` implementando `PasswordRule`, anotada
   `@Component`.
2. Adicionar uma entrada em `RuleViolation` (ex.: `SUA_VIOLATION`).
3. Escrever `SuaRegraTest` espelhando `MinimumLengthRuleTest`.
4. Atualizar a tabela acima.

Nada mais precisa mudar. O Spring injeta automaticamente a nova regra na
`List<PasswordRule>` do `PasswordValidator`.
