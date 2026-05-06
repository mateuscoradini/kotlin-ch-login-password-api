# Password Validator API

[![CI](https://github.com/coradini/kotlin-ch-login/actions/workflows/ci.yml/badge.svg)](https://github.com/coradini/kotlin-ch-login/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)

API web em Kotlin + Spring Boot que valida senhas contra um conjunto de 7 regras
(comprimento mínimo, classes de caracteres, repetições e espaços em branco) e
retorna tanto um **veredicto booleano** quanto a **lista exata de regras violadas**.

> **Desafio:** ver `README-INSTRUCTIONS.md` para o enunciado oficial.
> **Diretrizes de código aplicadas em co-pilot/claude:** ver `agents/co-dev.md`.

## Quickstart

```bash
./gradlew bootRun &
curl -X POST localhost:8080/api/v1/passwords/validate \
  -H 'Content-Type: application/json' \
  -d '{"password":"AbTp9!fok"}'
# → {"valid":true,"violations":[]}
```

Ou com Docker:

```bash
docker build -t password-api .
docker run --rm -p 8080:8080 password-api
```

## Sumário

- [Stack](#stack)
- [Como executar](#como-executar)
- [Endpoints](#endpoints)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Documentação](#documentação)

## Stack

| Camada | Escolha |
|---|---|
| Linguagem | Kotlin 2.0.21 |
| Runtime | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Build | Gradle 8.10.2 (via wrapper) |
| Testes | JUnit 5, Kluent (assertions infix), Mockito Kotlin |
| Cobertura | Jacoco |
| Documentação API | Springdoc OpenAPI (Swagger UI) |
| Observabilidade | Spring Boot Actuator |

## Como executar

Pré-requisitos: **JDK 21** instalado (não é preciso ter Gradle — usar o wrapper).

```bash
# Build + testes + relatório de cobertura
./gradlew clean build

# Apenas os testes
./gradlew test

# Subir a aplicação (porta 8080)
./gradlew bootRun
```

Saídas relevantes pós-build:

- Relatório de testes — `build/reports/tests/test/index.html`
- Relatório Jacoco — `build/reports/jacoco/test/html/index.html`
- Jar executável — `build/libs/kotlin-ch-login-password-api-1.0.0.jar`

## Endpoints

Com a aplicação rodando em `http://localhost:8080`:

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/api/v1/passwords/validate` | Valida uma senha. |
| `GET` | `/swagger-ui.html` | Documentação interativa (Swagger UI). |
| `GET` | `/v3/api-docs` | OpenAPI JSON. |
| `GET` | `/actuator/health` | Health check. |
| `GET` | `/actuator/prometheus` | Métricas em formato OpenMetrics (off em prod por padrão). |

### Exemplo de uso

```bash
curl -X POST http://localhost:8080/api/v1/passwords/validate \
  -H 'Content-Type: application/json' \
  -d '{"password":"AbTp9!fok"}'
# → {"valid":true,"violations":[]}

curl -X POST http://localhost:8080/api/v1/passwords/validate \
  -H 'Content-Type: application/json' \
  -d '{"password":"AbTp9!foo"}'
# → {"valid":false,"violations":["REPEATED_CHARACTERS"]}
```

Detalhes completos do contrato em [`docs/04-api-contract.md`](docs/04-api-contract.md).

## Estrutura do projeto

```
src/main/kotlin/br/com/coradini/kotlin/ch/login/api/
├── PasswordValidatorApplication.kt
├── domain/                              # núcleo (sem dependência de framework de transporte)
│   ├── model/                           # Password, ValidationResult, RuleViolation
│   ├── rule/                            # 7 implementações de PasswordRule
│   └── service/PasswordValidator.kt     # orquestrador (Specification Pattern)
├── application/
│   ├── port/input/                      # ValidatePasswordUseCase (interface)
│   └── usecase/ValidatePasswordService  # implementação do use case
└── infrastructure/adapter/input/rest/   # adapter de entrada HTTP
    ├── PasswordValidationController
    ├── dto/                             # request/response DTOs
    ├── mapper/                          # ValidationResult → DTO
    └── exception/                       # GlobalExceptionHandler + ApiError
```

A separação em `domain → application → infrastructure` segue
**Arquitetura Hexagonal (Ports & Adapters)**: o domínio não conhece HTTP, JSON
nem Spring Web. Detalhes e diagrama em [`docs/01-architecture.md`](docs/01-architecture.md).

## Documentação

A pasta `docs/` reúne, em arquivos curtos e focados, **as decisões por trás do
código** — exatamente o que o enunciado pede:

| Documento | Assunto |
|---|---|
| [`docs/01-architecture.md`](docs/01-architecture.md) | Arquitetura Hexagonal, fluxo, isolamento de camadas |
| [`docs/02-design-decisions.md`](docs/02-design-decisions.md) | ADRs: Specification Pattern, value object, response shape, framework footprint |
| [`docs/03-validation-rules.md`](docs/03-validation-rules.md) | As 7 regras: enunciado, classe e exemplos |
| [`docs/04-api-contract.md`](docs/04-api-contract.md) | Endpoint, payloads, status codes, erros |
| [`docs/05-testing-strategy.md`](docs/05-testing-strategy.md) | Pirâmide de testes, padrões do `co-dev.md`, cobertura |
| [`docs/06-assumptions.md`](docs/06-assumptions.md) | Premissas onde o enunciado é silencioso |
| [`docs/07-next-steps.md`](docs/07-next-steps.md) | Roadmap: 3 cenários de validação dupla (PII, blacklist, HIBP) |
| [`docs/08-observability.md`](docs/08-observability.md) | Métricas (Micrometer/Prometheus), logs (SLF4J), regra de não-vazamento de senha |
