# 06 — Premissas

Onde o `README-INSTRUCTIONS.md` é silencioso ou ambíguo, o que assumimos e por
quê. Cada premissa é endereçável: mudar a interpretação se traduz em uma única
mudança localizada.

## Premissa 1 — Response inclui a lista de violações

**Texto do enunciado:** *"Output: Um boolean indicando se a senha é válida."*

**Interpretação assumida:** O response é
`{ "valid": <bool>, "violations": [<RuleViolation>...] }`. O campo `valid`
respeita literalmente o enunciado; `violations` é informação adicional.

**Motivo:** OWASP Authentication Cheat Sheet recomenda explicitamente:
> *"the error message should describe **every** complexity rule that the new
> password does not comply with"*

Ver ADR 02 em [`02-design-decisions.md`](02-design-decisions.md#adr-02--resposta-com-valid--violations-em-vez-de-só-boolean).

**Como reverter:** Remover o campo `violations` em
`PasswordValidationResponse` e ajustar o `ValidationResultMapper`. Os testes
de integração que assertam o conteúdo de `violations` precisariam ser
relaxados.

---

## Premissa 2 — Caracteres especiais formam um conjunto fechado

**Texto do enunciado:** *"Considere como especial os seguintes caracteres:
`!@#$%^&*()-+`"*

**Interpretação assumida:** Os 12 caracteres listados são o conjunto
**exclusivo** de especiais aceitos. Qualquer outro símbolo (`?`, `_`, `~`,
etc.) **não** satisfaz `ContainsSpecialCharacterRule`.

**Motivo:** A formulação ("considere como especial **os seguintes**") sugere
enumeração taxativa, não exemplos.

**Consequência:** Uma senha como `AbTp9?fok` é inválida por
`NO_SPECIAL_CHARACTER` (mesmo que `?` seja "especial" no senso comum).
Documentado no Swagger e no `docs/03-validation-rules.md`.

**Como reverter:** Trocar o `Set<Char>` em `ContainsSpecialCharacterRule` por
uma definição mais ampla (por exemplo, `!Char.isLetterOrDigit() && !Char.isWhitespace()`).

---

## Premissa 3 — Caracteres fora dos conjuntos definidos não são proibidos

O enunciado define o que é dígito, minúscula, maiúscula, especial e
whitespace, mas **não declara que outras categorias** (símbolos não listados,
emoji, dígitos não-ASCII, letras acentuadas) são proibidas.

**Interpretação assumida:** Caracteres desses grupos *não* causam violação
direta, mas também *não* contam como satisfazendo `ContainsSpecialCharacterRule`.
Eles podem aparecer livremente desde que as 7 regras sejam satisfeitas.

**Exemplo:** `Aâbtp9!ok` é válida (mesmo com `â` que não é nem
"upper" nem "lower" puramente ASCII — embora `Char.isLowerCase()` da JVM
retorne `true` para acentuadas).

**Como reverter:** Adicionar uma 8ª regra `OnlyAllowedCharactersRule` checando
`char in (alphanum + SPECIAL_CHARACTERS)`.

---

## Premissa 4 — Repetição é case-sensitive

**Texto:** *"Não possuir caracteres repetidos dentro do conjunto"*

**Interpretação assumida:** `'A'` e `'a'` são caracteres **distintos**.
A senha `"Aa"` satisfaz `NoRepeatedCharactersRule`.

**Motivo:** Não há indicação de case-folding no enunciado. Consistente com
`Char.equals` da JVM (case-sensitive). Coberto por
`isSatisfiedBy - should treat lowercase and uppercase as distinct characters`.

**Como reverter:** Usar `password.value.lowercase().toSet().size ==
password.length` na regra.

---

## Premissa 5 — Body com `password: null` ou body inteiro ausente são erros 400

O enunciado não define o que fazer com requests inválidas (sem body, JSON
malformado, campo faltando).

**Interpretação assumida:** REST convencional — devolver `400 Bad Request` com
um payload de erro estruturado (`ApiError(code, message)`):
- Body ausente / JSON inválido → `MALFORMED_BODY`.
- Campo `password` ausente → `VALIDATION_ERROR` (Bean Validation com `@NotNull`).

**Motivo:** Validar limites do request **antes** de chegar ao domínio é
prática padrão; expor detalhes mínimos sobre o erro melhora DX.

**Casos validados:** dois testes em
`PasswordValidationControllerIntegrationTest`.

---

## Premissa 6 — String vazia é uma senha válida do ponto de vista do contrato

Diferente de "campo ausente". `{"password": ""}` é um body válido
sintaticamente; o conteúdo apenas falha em todas as regras de classe de
caractere e em `MIN_LENGTH`.

**Interpretação assumida:** Devolver `200 OK` com `valid: false` e a lista
de violações (consistente com a Premissa 1). **Não** retornar 400 — o request
está bem-formado; é a senha que é ruim.

**Coberto por:** `validate - should return valid false when password is empty`.

---

## Premissa 7 — Ordem das violações no array não é parte do contrato

A ordem em que as regras são executadas (e portanto a ordem dos códigos no
array `violations`) depende da ordem de descoberta de beans pelo Spring.

**Interpretação assumida:** O cliente deve tratar `violations` como um *set*
desordenado de códigos. Documentado no Swagger.

**Por quê:** Forçar uma ordem específica acopla o response a uma decisão
arquitetural mutável. Os testes de integração usam `hasItem(...)` (ordem
livre) na maioria dos casos; só os testes de violação única usam `contains(...)`.

**Como tornar determinístico (se necessário um dia):** anotar cada regra com
`@Order(N)` definindo a posição.
