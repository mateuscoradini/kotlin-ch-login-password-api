# 05 — Estratégia de testes

## Pirâmide adotada

```
               ┌─────────────────────────┐
               │  Integration (Spring)   │   ← 10 testes
               │  PasswordValidation…    │      MockMvc + @SpringBootTest
               │  ControllerIntegration  │
               └─────────────────────────┘
            ┌────────────────────────────────────┐
            │  Adapter unit (mocked)             │   ← 7 testes
            │  PasswordValidationControllerTest  │      controller isolado
            │  GlobalExceptionHandlerTest        │      branches de erro
            └────────────────────────────────────┘
            ┌────────────────────────────────┐
            │  Application unit (mocked)     │   ← 3 testes
            │  ValidatePasswordServiceTest   │      Mockito Kotlin no PasswordValidator
            └────────────────────────────────┘
       ┌──────────────────────────────────────────┐
       │  Domain orchestrator unit (mocked)       │   ← 3 testes
       │  PasswordValidatorTest                   │      Mockito Kotlin nas PasswordRule
       └──────────────────────────────────────────┘
  ┌────────────────────────────────────────────────────────┐
  │  Domain rule unit (zero mocks)                         │   ← 44 testes
  │  *RuleTest                                             │      uma classe por regra
  └────────────────────────────────────────────────────────┘
```

**Total:** 67 testes. Cobertura: **99% linhas / 100% branches** (Jacoco).

A pirâmide é intencional:
- A **base larga** com testes de regra é barata (instanciação direta, sem
  framework, sem mock) e cobre a lógica de negócio.
- O **meio** valida a orquestração e a integração entre camadas com mocks
  pequenos e focados.
- O **topo estreito** sobe o `ApplicationContext` apenas o suficiente para
  validar wiring e contrato HTTP — uma única classe.

## Convenções vindas do `co-dev.md`

Todas implementadas:

| Diretriz | Aplicação |
|---|---|
| Kluent infix (` `should be equal to` ` em vez de `shouldBeEqualTo`) | Todas as asserções dos unit tests. |
| Nome no formato `` `metodo - should comportamento` `` | Backticks em todos os testes. |
| `@Nested inner class` para unit tests | `IsSatisfiedByMethod`, `ViolationProperty`, `ValidateMethod`, `PerformMethod`. |
| **Sem** `@Nested` em integration tests | `PasswordValidationControllerIntegrationTest` é uma classe flat. |
| `should be empty()` para listas vazias | `result.violations.\`should be empty\`()` no `PasswordValidatorTest` e no `ValidatePasswordServiceTest`. |
| Sem `@DisplayName` | Nomes em backticks já são descritivos o suficiente. |
| Sem logs em testes | Nenhum `LoggerFactory` em `src/test/`. |

## Bibliotecas

| Lib | Por quê |
|---|---|
| JUnit 5 | Padrão de mercado em Kotlin/Spring; suporta `@Nested` e `@ParameterizedTest`. |
| Kluent 1.73 | Recomendado pelo `co-dev.md`. Sintaxe infix lê próximo de inglês. |
| Mockito Kotlin 5.4 | Recomendado pelo `co-dev.md`. Wrapper Kotlin-friendly do Mockito. |
| Spring Boot Starter Test | `MockMvc`, `@SpringBootTest`, Hamcrest matchers. |

> O `spring-boot-starter-test` traz Mockito core como dependência transitiva;
> excluímos `org.mockito` ali e re-incluímos `mockito-core` 5.14.2 + `mockito-kotlin`
> para evitar conflito de versão.

## Onde cada coisa é testada

### Regras (`*RuleTest`)

Uma classe por regra. Estrutura padrão (vide `MinimumLengthRuleTest`):

- `@Nested inner class IsSatisfiedByMethod { … }` — happy path + edge cases
  (vazio, exatamente no limite, fora do limite, casos especiais como
  case-sensitive em `NoRepeatedCharactersRule`).
- `@Nested inner class ViolationProperty { … }` — confirma que o código de
  violação está estável (proteção contra renomeação acidental do enum).

Para `ContainsSpecialCharacterRule` há um `@ParameterizedTest` cobrindo todos
os 12 caracteres especiais aceitos — pega regressões caso alguém remova um do
set.

### `PasswordValidatorTest`

Usa stubs de `PasswordRule` via `mock { on { isSatisfiedBy(...) } doReturn ... }`.
3 cenários:
1. Todas regras satisfeitas → `valid = true`, `violations` vazio.
2. Algumas regras falham → coleta apenas as violações das que falharam.
3. Lista de regras vazia → `valid = true` (degenerate case bem definido).

### `ValidatePasswordServiceTest`

Mock do `PasswordValidator`. 3 cenários:
1. Verifica que o use case embrulha o `String` cru em `Password` (value object)
   antes de delegar.
2. Devolve fielmente o `ValidationResult` produzido pelo validator.
3. Caso de sucesso explicito (`valid = true`, `violations.\`should be empty\`()`).

### `PasswordValidationControllerTest` e `GlobalExceptionHandlerTest`

Unit tests dos adapters de entrada para fechar branches que o teste de
integração não consegue exercitar diretamente:

- `PasswordValidationControllerTest`: instancia o controller com mock do use
  case e verifica o branch `request.password ?: ""` (impossível de testar via
  HTTP porque `@Valid @NotNull` bloqueia `null` antes do método rodar).
- `GlobalExceptionHandlerTest`: cobre o fallback `ifBlank { "Invalid request
  payload" }` (cenário `MethodArgumentNotValidException` sem field errors) e
  o handler `handleUnexpected` (500 INTERNAL_ERROR).

São tests de unidade pura — sem Spring, sem MockMvc — exatamente porque
queremos exercitar os branches do código sem o overhead/limitação do
container.

### `PasswordValidationControllerIntegrationTest`

`@SpringBootTest` + `@AutoConfigureMockMvc`. Sem `@Nested` (per `co-dev.md`).
Cobre:
- Os **8 exemplos canônicos** do `README-INSTRUCTIONS.md`, cada um numa função
  com nome explícito.
- 2 cenários de erro: body ausente (400 + `MALFORMED_BODY`) e body sem campo
  (400 + `VALIDATION_ERROR`).

## Cobertura (Jacoco)

```
./gradlew test
# Relatório: build/reports/jacoco/test/html/index.html
```

| Métrica | Valor |
|---|---|
| Linhas | **99 %** (65/66) |
| Branches | **100 %** (8/8) |
| Métodos | **96 %** (47/49) |
| Instructions | **99 %** (456/460) |

`build.gradle.kts` exclui da cobertura:
- `PasswordValidatorApplication*` — entry point, sem lógica.
- `**/dto/**` — data classes de transporte (sem comportamento).
- `**/config/**` — reservado para futuras configurations.

## Static analysis (Detekt)

```bash
./gradlew detekt
```

`config/detekt/detekt.yml` controla as regras. `autoCorrect = true` no
`build.gradle.kts` aplica fixes triviais (formatting) automaticamente.

## Smoke E2E externo

Coleção Postman em `postman/` cobre o caminho HTTP completo contra uma app
real. Documentado em [`04-api-contract.md`](04-api-contract.md#smoke-tests-via-postman--newman).
