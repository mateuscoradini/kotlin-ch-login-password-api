# 04 — Contrato da API

Documentação canônica do endpoint, payloads e códigos de erro.
A versão interativa e sempre atualizada está no Swagger UI:
`http://localhost:8080/swagger-ui.html` (com a aplicação rodando).

## Endpoint

`POST /api/v1/passwords/validate`

| Header | Valor |
|---|---|
| `Content-Type` | `application/json` |
| `Accept` | `application/json` |

### Request body

```json
{
  "password": "AbTp9!fok"
}
```

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `password` | string | Sim | A senha a ser validada. Pode ser vazia (resulta em `valid: false` com várias violações). |

### Response 200 OK

```json
{
  "valid": true,
  "violations": []
}
```

```json
{
  "valid": false,
  "violations": ["NO_DIGIT", "REPEATED_CHARACTERS"]
}
```

| Campo | Tipo | Observação |
|---|---|---|
| `valid` | boolean | `true` ⇔ `violations` vazio. |
| `violations` | string[] | Códigos do enum `RuleViolation` — ver `docs/03-validation-rules.md`. |

#### Códigos possíveis em `violations`

`MIN_LENGTH`, `NO_DIGIT`, `NO_LOWERCASE`, `NO_UPPERCASE`,
`NO_SPECIAL_CHARACTER`, `REPEATED_CHARACTERS`, `CONTAINS_WHITESPACE`.

### Response 400 Bad Request

Quando o body é malformado ou o campo `password` está ausente:

```json
{
  "code": "MALFORMED_BODY",
  "message": "Request body is missing or malformed"
}
```

```json
{
  "code": "VALIDATION_ERROR",
  "message": "password: must not be null"
}
```

| `code` | Quando |
|---|---|
| `MALFORMED_BODY` | Body ausente, JSON inválido, content-type errado. |
| `VALIDATION_ERROR` | Body presente mas falha em `@Valid` (ex.: `password: null`). |
| `INTERNAL_ERROR` | Erro inesperado (catch-all). HTTP 500. |

## Exemplos `curl`

```bash
# Senha válida
curl -X POST http://localhost:8080/api/v1/passwords/validate \
  -H 'Content-Type: application/json' \
  -d '{"password":"AbTp9!fok"}'
# → {"valid":true,"violations":[]}

# Senha curta sem dígito sem maiúscula sem especial
curl -X POST http://localhost:8080/api/v1/passwords/validate \
  -H 'Content-Type: application/json' \
  -d '{"password":"ab"}'
# → {"valid":false,"violations":["MIN_LENGTH","NO_DIGIT","NO_UPPERCASE","NO_SPECIAL_CHARACTER"]}

# Senha com espaço
curl -X POST http://localhost:8080/api/v1/passwords/validate \
  -H 'Content-Type: application/json' \
  -d '{"password":"AbTp9 fok"}'
# → {"valid":false,"violations":["NO_SPECIAL_CHARACTER","CONTAINS_WHITESPACE"]}

# Body vazio
curl -X POST http://localhost:8080/api/v1/passwords/validate \
  -H 'Content-Type: application/json'
# → 400 {"code":"MALFORMED_BODY","message":"Request body is missing or malformed"}
```

## Smoke tests via Postman / Newman

A pasta `postman/` contém uma coleção pronta para execução manual ou
automatizada via [Newman](https://www.npmjs.com/package/newman).

```
postman/
├── kotlin-ch-login-password-api.postman_collection.json   # 11 requests com test scripts
└── env/
    ├── local.postman_environment.json           # baseUrl=http://localhost:8080
    ├── test.postman_environment.json            # placeholder p/ ambiente de QA
    ├── staging.postman_environment.json         # placeholder p/ staging
    └── prod.postman_environment.json            # placeholder p/ prod
```

A coleção verifica os 8 exemplos do `README-INSTRUCTIONS.md`, dois cenários de
erro (body vazio + body sem campo) e o `/actuator/health`. Cada request tem
um `pm.test(...)` que valida status + payload.

Para rodar manualmente, importe a coleção no Postman e selecione um environment.

Para rodar via Newman (CI ou shell):

```bash
# Instalar uma vez
npm install -g newman

# Subir a app
./gradlew bootRun &

# Esperar a app subir e disparar a coleção contra o env local
newman run postman/kotlin-ch-login-password-api.postman_collection.json \
  -e postman/env/local.postman_environment.json
```

Os arquivos de env de `test/staging/prod` contêm `https://*.example.invalid` e
**precisam ser atualizados** com os hostnames reais antes de uso.

## Versionamento

- O endpoint vive sob `/api/v1/` — mudanças incompatíveis abrirão `/api/v2/` em
  paralelo, sem quebrar clientes existentes.
- O enum `RuleViolation` é parte do contrato: nomes nunca devem ser renomeados;
  códigos novos são adicionados no fim do enum.
