# 01 — Arquitetura

## Visão geral

A aplicação adota **Arquitetura Hexagonal (Ports & Adapters)**. O núcleo
(`domain/`) contém as regras de negócio e não importa nada de Spring, Jackson
ou HTTP. A camada `application/` define o que o sistema faz (use cases) através
de uma interface (port) e fornece a implementação. A camada `infrastructure/`
é onde tecnologias concretas (Spring Web, Springdoc, Bean Validation) entram —
em formato de adapters que **dependem do domínio**, nunca o contrário.

```
┌────────────────────────────────────────────────────────────────────┐
│  infrastructure/adapter/input/rest/                                │
│   ┌──────────────────────────┐    ┌────────────────────────────┐   │
│   │ PasswordValidationContr. │ →  │ ValidationResultMapper     │   │
│   │ (Spring @RestController) │    │ (DTO ↔ domain)             │   │
│   └────────────┬─────────────┘    └────────────────────────────┘   │
│                │ depende da interface (port) ↓                     │
└────────────────┼───────────────────────────────────────────────────┘
                 ↓
┌────────────────────────────────────────────────────────────────────┐
│  application/                                                      │
│   ┌──────────────────────────────┐                                 │
│   │ ValidatePasswordUseCase      │  ← port (interface)             │
│   └──────────────┬───────────────┘                                 │
│                  │ implementado por                                │
│                  ↓                                                 │
│   ┌──────────────────────────────┐                                 │
│   │ ValidatePasswordService      │  @Service                       │
│   └──────────────┬───────────────┘                                 │
└──────────────────┼─────────────────────────────────────────────────┘
                   ↓
┌────────────────────────────────────────────────────────────────────┐
│  domain/                                                           │
│   ┌────────────────┐  ┌─────────────────────┐  ┌────────────────┐  │
│   │ Password (VO)  │  │ ValidationResult    │  │ RuleViolation  │  │
│   └────────────────┘  └─────────────────────┘  └────────────────┘  │
│                                                                    │
│   ┌──────────────────────────────────────────────────────────────┐ │
│   │ PasswordValidator                                            │ │
│   │   private val rules: List<PasswordRule>                      │ │
│   │   fun validate(password: Password): ValidationResult         │ │
│   └─────────────┬────────────────────────────────────────────────┘ │
│                 │ Specification Pattern                            │
│                 ↓                                                  │
│   ┌──────────────────────────────────────────────────────────────┐ │
│   │ PasswordRule (interface)                                     │ │
│   │ ├─ MinimumLengthRule                                         │ │
│   │ ├─ ContainsDigitRule                                         │ │
│   │ ├─ ContainsLowercaseRule                                     │ │
│   │ ├─ ContainsUppercaseRule                                     │ │
│   │ ├─ ContainsSpecialCharacterRule                              │ │
│   │ ├─ NoRepeatedCharactersRule                                  │ │
│   │ └─ NoWhitespaceRule                                          │ │
│   └──────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────┘
```

A direção das setas é a direção da dependência — sempre apontando para o domínio.
O domínio nunca importa nada de application/infrastructure.

## Fluxo de uma requisição

1. **HTTP** chega em `POST /api/v1/passwords/validate` com body
   `{"password": "..."}`.
2. **`PasswordValidationController`** desserializa em `PasswordValidationRequest`
   (`infrastructure/.../dto`), extrai a string e chama
   `validatePasswordUseCase.perform(rawPassword)`.
3. **`ValidatePasswordService`** (`application/usecase`) embrulha a string em
   `Password` (value object do domínio) e delega para `PasswordValidator`.
4. **`PasswordValidator`** (`domain/service`) itera sobre `List<PasswordRule>`
   injetada pelo Spring, coleta o `RuleViolation` de cada regra que falhou e
   devolve `ValidationResult(violations)`.
5. **`ValidationResultMapper`** converte para `PasswordValidationResponse` e o
   controller retorna 200 OK.

## Por que essa separação rende para o desafio

- **Open/Closed (SOLID).** Adicionar uma 8ª regra estrutural é criar uma classe
  que implementa `PasswordRule` e anotá-la `@Component`. O Spring injeta na
  `List<PasswordRule>` e o validator passa a usá-la — **sem alterar o validator,
  o use case ou o controller**.
- **Dependency Inversion.** O controller depende da interface
  `ValidatePasswordUseCase`, não da implementação. Trocar o motor de validação
  (por algo assíncrono, com cache, com observabilidade) é trocar o `@Service`
  que implementa essa interface.
- **Testabilidade.** Cada camada se testa em isolamento sem subir o Spring:
  - regras: instanciadas direto, sem mock;
  - validator: stubs de `PasswordRule` via Mockito Kotlin;
  - service: mock do `PasswordValidator`;
  - controller: `@SpringBootTest` + `MockMvc` cobre o slice HTTP completo.
- **Domínio puro.** Sem `@Component`/`@Service` em `domain/model/`, sem Jackson
  nas DTOs do domínio. As anotações de `@Component` aparecem nas regras —
  **decisão pragmática** explicada no [ADR 03 do `02-design-decisions.md`](02-design-decisions.md#adr-03--anotações-spring-em-classes-de-domínio).

## Onde cada coisa mora

| Pacote | Responsabilidade | Exemplos |
|---|---|---|
| `domain/model/` | Tipos de dados do domínio | `Password`, `ValidationResult`, `RuleViolation` |
| `domain/rule/` | Specifications individuais | `MinimumLengthRule`, `NoRepeatedCharactersRule`, … |
| `domain/service/` | Orquestração de regras | `PasswordValidator` |
| `application/port/input/` | Contratos de entrada (driving ports) | `ValidatePasswordUseCase` |
| `application/usecase/` | Implementação dos use cases | `ValidatePasswordService` |
| `infrastructure/adapter/input/rest/` | Adapters HTTP (driving adapters) | `PasswordValidationController`, DTOs, mapper, exception handler |

> Não há `adapter/output/` no projeto — o desafio é puramente computacional, sem
> integração externa. Quando ports de saída forem introduzidos (ex.: HIBP no
> roadmap), elas viverão em `application/port/output/` e os adapters em
> `infrastructure/adapter/output/`. Ver [`07-next-steps.md`](07-next-steps.md).
