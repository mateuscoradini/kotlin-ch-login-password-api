# 02 — Decisões de design (ADRs)

Cada ADR segue o formato **Contexto → Decisão → Consequências (positivas/negativas)**.
A intenção é registrar o *porquê* de cada escolha — não o *quê* (o código já
mostra o quê).

---

## ADR 01 — Specification Pattern para as regras de validação

**Contexto.** Existem 7 regras de validação distintas e independentes entre si.
Implementar tudo num único método (`if/else`/regex gigante) seria mais curto
mas concentraria toda a lógica em um lugar e violaria SRP. O `co-dev.md` pede
SOLID e Object Calisthenics; o enunciado destaca "abstração, acoplamento,
extensibilidade e coesão" como critério de avaliação.

**Decisão.** Cada regra é uma classe que implementa a interface
`PasswordRule`:

```kotlin
interface PasswordRule {
    val violation: RuleViolation
    fun isSatisfiedBy(password: Password): Boolean
}
```

`PasswordValidator` recebe `List<PasswordRule>` por construtor e itera,
coletando o `violation` das regras que retornaram `false`.

**Consequências positivas.**
- **Open/Closed:** adicionar uma 8ª regra é criar uma classe + `@Component`. O
  validator e o use case não mudam.
- **Single Responsibility:** cada arquivo trata de uma única regra; testes ficam
  triviais (instanciar, chamar, asserir).
- **Composição declarativa:** se quisermos um validator "lite" (subset de
  regras) basta passar uma lista diferente — útil pra contextos como
  reset-de-senha vs. cadastro.

**Consequências negativas.**
- 7 arquivos para algo que caberia em 30 linhas inline. Em código de produção
  com dezenas de regras isso compensa; em 7 regras é um leve over-engineering
  que se justifica pelo critério de avaliação do desafio.
- A ordem nativa das violações dependeria da ordem de injeção do Spring (que
  segue a ordem dos pacotes/classes). Resolvido com `sortedBy { it.name }` no
  validator e contratualizado em [Premissa 7](06-assumptions.md#premissa-7--ordem-das-violações-é-alfabética-e-estável-parte-do-contrato).

---

## ADR 02 — Resposta com `valid` + `violations` em vez de só boolean

**Contexto.** O enunciado diz literalmente: *"Output: Um boolean indicando se a
senha é válida."* Tomar isso ao pé da letra produziria um response como
`{ "valid": true }` ou só `true`/`false`.

**Decisão.** O response inclui também `violations: List<RuleViolation>` — vazia
quando válida, populada com os códigos das regras violadas quando inválida.

**Justificativa.** Alinhamento com a OWASP Authentication Cheat Sheet:
> *"the error message should describe **every** complexity rule that the new
> password does not comply with, not just the 1st rule it doesn't comply with"*

Em produção, expor só o booleano frustra o usuário (que não sabe o que
corrigir) e força round-trips. Como a obrigação contratual de devolver o
veredicto continua respeitada (o campo `valid` é a verdade), entrego mais sem
quebrar o que foi pedido.

**Consequências positivas.**
- UX melhor (frontend pode listar as violações).
- Métrica útil em produção (qual regra mais bloqueia usuários?).
- Sem mudança de status code — sempre 200, response autodescritivo.

**Consequências negativas.**
- Acréscimo de superfície da API (campo extra). Documentado no Swagger e em
  [`docs/04-api-contract.md`](04-api-contract.md).
- Premissa explicitamente assumida — registrada em
  [`docs/06-assumptions.md`](06-assumptions.md).

---

## ADR 03 — Anotações Spring em classes de domínio

**Contexto.** Hexagonal "purista" pede que o domínio não conheça framework
algum. A alternativa é uma `@Configuration` que crie cada `PasswordRule` como
bean e construa o `PasswordValidator` manualmente.

**Decisão.** Aceitar anotações `@Component` nas regras e `@Component` no
`PasswordValidator`. As DTOs não têm anotações de domínio; e o domínio não
importa nada de `org.springframework.web.*` ou Jackson.

**Justificativa.** O custo da configuração manual (uma classe `@Configuration`
listando 7 beans) supera o ganho semântico. As anotações usadas
(`@Component`/`@Service`) são *do container DI*, não do framework de
transporte; Kotlin com value classes/data classes mantém o domínio totalmente
testável sem subir o Spring.

**Consequências positivas.**
- Setup mais simples; injeção da `List<PasswordRule>` é automática.
- Testes unitários do domínio continuam funcionando sem `@SpringBootTest`.

**Consequências negativas.**
- Domínio carrega uma micro-dependência conceitual de Spring DI. Para um
  produto que precisasse rodar fora de Spring (ex.: lib reutilizável), a
  decisão seria a oposta — explicitada como nota no roadmap.

---

## ADR 04 — `Password` como `data class` (e não `@JvmInline value class`)

**Contexto.** Object Calisthenics ("wrap primitives") sugere que tipos
primitivos (como uma `String` que representa uma senha) sejam embalados num
value object. A implementação Kotlin moderna preferida seria
`@JvmInline value class Password(val value: String)` — zero overhead em runtime.

**Decisão.** Usar `data class Password(val value: String)`.

**Justificativa.** O *name mangling* do compilador Kotlin para inline value
classes quebra o argument matching do Mockito (`eq(Password("abc"))` falha em
métodos virtuais mockados, porque o JVM vê a função tomando `String`, não
`Password`). Trocar o motor de mock para MockK resolveria, mas o `co-dev.md`
explicita Mockito Kotlin como padrão. O ganho de performance de `value class`
em uma operação O(n) sobre uma string curta é desprezível neste contexto.

**Consequências positivas.**
- Mockito Kotlin funciona naturalmente nos testes (cf.
  `ValidatePasswordServiceTest`).
- `data class` ainda satisfaz a intenção de Object Calisthenics: encapsulamento
  do primitivo + métodos de domínio (`length`, `characters()`).
- `equals/hashCode/copy` vêm de graça, úteis para testes e estruturas de dados.

**Consequências negativas.**
- Alocação de objeto a cada validação (vs. zero alocação com inline class).
  Aceitável: 1 objeto por requisição HTTP é ruído.
- ADR explicita que a escolha é *para acomodar a stack de testes*, não uma
  preferência absoluta.

---

## ADR 05 — Use case com `perform()` em vez de `validate()`

**Contexto.** O método principal do use case poderia se chamar `validate()`,
`execute()`, `invoke()`, `apply()`. Cada nome carrega convenções e
expectativas.

**Decisão.** O contrato do `ValidatePasswordUseCase` expõe `perform(rawPassword)`.
O `PasswordValidator` (no domínio) expõe `validate(password)`.

**Justificativa.** Distinguir verbo por camada elimina ambiguidade:
- `perform` → operação do use case (lê input cru, orquestra, devolve output);
- `validate` → operação semântica do domínio (predicado sobre uma `Password`).

Quem lê o controller vê `useCase.perform(...)` e sabe imediatamente que é o
boundary da aplicação. Quem lê o domínio vê `validator.validate(...)` e sabe
que é puro negócio.

**Consequências positivas.**
- Convenção replicável: novos use cases (`SignUpUseCase`, `ResetPasswordUseCase`)
  todos exporiam `perform()`. Operações de domínio mantêm verbos próprios.
- Evita o uso de `invoke()` (operator function), que esconde quem está sendo
  chamado quando lendo o código.

**Consequências negativas.**
- Pequena curva de aprendizado para quem espera `execute()` (padrão alternativo
  comum em projetos com Command Pattern).

---

## ADR 06 — Logging via SLF4J padrão (não classe Logger customizada)

**Contexto.** O `co-dev.md` recomenda priorizar uma classe `Logger` específica
do domínio do projeto de referência. O contexto deste desafio é diferente: é um
projeto público, e replicar nomes/utilitários internos seria inadequado.

**Decisão.** Usar `org.slf4j.LoggerFactory` quando logging for necessário.
A camada `application/` (`ValidatePasswordService`) loga **uma linha por
validação** — `INFO` com `valid=true/false` + `violatedRules=[...]` no caso
inválido — declarando o logger em `companion object`. O domínio
(`PasswordValidator` e regras) **não** loga, mantendo-se puro. Detalhes em
[`docs/08-observability.md`](08-observability.md#logs).

**Justificativa.** Adicionar uma `Logger` customizada exigiria cópia de uma
abstração existente em outro lugar; SLF4J já cobre 100% das necessidades
estruturadas via *MDC* + JSON encoder (Logback) sem código próprio. Logar
no boundary da `application/` (e não no domínio nem no controller) compõe
**uma única linha por request** com toda a informação útil — sem ruído.

**Consequências positivas.** Zero dívida técnica; alinhamento com o padrão
JVM. Granularidade no boundary evita logs duplicados ou conflitantes nas
camadas superior/inferior.

**Consequências negativas.** Quando logs em outras camadas forem necessários
(features futuras), surgirá a tentação de criar uma camada customizada.
Documentar isso em [`08-observability.md`](08-observability.md) *quando* a
necessidade surgir, não antes.
