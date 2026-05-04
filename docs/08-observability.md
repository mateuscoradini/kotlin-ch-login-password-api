# 08 — Observabilidade (métricas e logs)

Este documento cobre o que a aplicação emite para observabilidade externa.
Em sistemas de autenticação, observabilidade é especialmente sensível —
**a senha nunca pode aparecer em log, métrica, traço ou erro**, em nenhuma
hipótese. Isso é uma invariante hard, validada caso a caso abaixo.

## Princípio inviolável: senha nunca sai do request

| O que **PODE** virar telemetria | O que **NÃO PODE** virar telemetria |
|---|---|
| Resultado (`valid: true/false`) | A senha em texto plano |
| Quantidade de violações | Hash da senha (até hash facilita correlação indevida) |
| Códigos das regras violadas (enum) | Substring da senha (length é OK; conteúdo não) |
| Status HTTP, código de erro | IDs de usuário associados (não há nesse contexto, mas ficaria fora) |
| | Stack traces que carreguem a senha em variáveis locais |

Toda alteração nesta área deve passar por revisão com este checklist.

## Métricas

### Stack

- **Micrometer** (já vem com Spring Boot Starter Actuator).
- **`micrometer-registry-prometheus`** — formato OpenMetrics exposto em
  `/actuator/prometheus`.
- Tag global `application=kotlin-ch-login-password-api` injetada via
  `management.metrics.tags.application` no `application.yml`.

### O que é exposto

| Métrica | Tipo | Tags | Significado |
|---|---|---|---|
| `password_validations_total` | Counter | `result={valid,invalid}` | Quantas validações terminaram com sucesso/falha. |
| `password_violations_total` | Counter | `rule={MIN_LENGTH,NO_DIGIT,...}` | Quantas vezes cada regra foi violada (uma req inválida pode incrementar várias). |

> **Nota:** o Timer de duração (`password_validation_duration_seconds`) foi removido intencionalmente.
> A validação é CPU-bound pura e sub-milissegundo — adicionar um Timer introduz overhead de
> instrumentação maior que o próprio trabalho. Quando regras com I/O entrarem (HIBP, blacklist em DB),
> o Timer deve ser reintroduzido pontualmente.

Todas são incrementadas em [`ValidatePasswordService`](../src/main/kotlin/br/com/coradini/kotlin/ch/login/api/application/usecase/ValidatePasswordService.kt) — o boundary
da camada `application/`. Métricas vivem **fora** do `domain/` para manter o
núcleo livre de framework de observabilidade.

### Por que dessa forma

- Counter `validations_total` separado por `result` permite alertas em
  *taxa de inválidas* (`rate(password_validations_total{result="invalid"}[5m])`).
- Counter `violations_total` por `rule` indica **qual regra está mais
  bloqueando usuários** — sinal direto de UX/produto. Se uma regra responde
  por 80% das falhas, talvez o erro esteja em comunicação, não na regra.

### Onde está exposto

| Profile | `/actuator/prometheus` |
|---|---|
| `dev` | ✅ |
| `test` | ✅ |
| `staging` | ✅ |
| `prod` | ❌ (intencional) |

**Em prod o endpoint Prometheus está fechado por padrão.** Em ambientes reais,
métricas costumam ser scrape-adas pelo Prometheus servidor a partir da rede
interna do cluster, com auth (basic, mTLS) ou via sidecar. Habilitar
publicamente no perímetro é vazamento de informação operacional.

Para habilitar em prod com auth: adicionar Spring Security restringindo
`/actuator/**` a um role interno + ajustar `application-prod.yml` com
`include: health,info,prometheus`. Documentar no ADR antes de fazer.

### Smoke local

```bash
./gradlew bootRun

# 3 validações
curl -s -X POST localhost:8080/api/v1/passwords/validate \
  -H 'Content-Type: application/json' -d '{"password":"AbTp9!fok"}' > /dev/null
curl -s -X POST localhost:8080/api/v1/passwords/validate \
  -H 'Content-Type: application/json' -d '{"password":"AbTp9!foo"}' > /dev/null
curl -s -X POST localhost:8080/api/v1/passwords/validate \
  -H 'Content-Type: application/json' -d '{"password":"abc"}' > /dev/null

# Métricas brutas
curl -s localhost:8080/actuator/prometheus | grep ^password_
```

Saída esperada:
```
password_validations_total{application="kotlin-ch-login-password-api",result="invalid"} 2.0
password_validations_total{application="kotlin-ch-login-password-api",result="valid"} 1.0
password_violations_total{application="kotlin-ch-login-password-api",rule="MIN_LENGTH"} 1.0
password_violations_total{application="kotlin-ch-login-password-api",rule="REPEATED_CHARACTERS"} 1.0
...
```

## Logs

### Stack

- **SLF4J** + **Logback** (default do Spring Boot, sem deps extras).
- Padrão de saída: humano-legível em dev, JSON-structured em prod via
  `logback-spring.xml` (a adicionar quando produção for real — atualmente
  usamos os defaults).
- Loggers declarados como `private val` em `companion object` da classe que
  loga — uma instância por classe, não por instância da classe.

### O que é logado

| Localização | Nível | Mensagem | Por quê |
|---|---|---|---|
| `ValidatePasswordService.recordOutcome` (senha válida) | `INFO` | `Password validation completed: valid=true` | Toda validação deixa rastro do resultado positivo. |
| `ValidatePasswordService.recordOutcome` (senha inválida) | `INFO` | `Password validation completed: valid=false, violationCount={n}, violatedRules=[RULE_A, RULE_B]` | Resultado negativo com as **regras exatas** que falharam — rastreabilidade por regra sem expor a senha. |
| `GlobalExceptionHandler.handleUnreadableBody` | `DEBUG` | `Rejected malformed request body: {message}` | Útil para debugar integrações com clientes errantes; não é um erro real. |
| `GlobalExceptionHandler.handleUnexpected` | `ERROR` | `Unexpected exception while handling request` + stack trace | Catch-all de bugs. Stack trace incluído pois ninguém quer 500s sem causa visível. |

### Por que dessa forma

- **Granularidade no boundary, não em todo lugar.** Logar dentro de cada
  regra geraria ruído (60+ linhas por request) sem agregar info. O
  `recordOutcome` no service compõe um *único* log line por validação com
  toda a informação útil.
- **Regras violadas no log, não apenas o contador.** O campo `violatedRules`
  permite correlacionar falhas sem depender exclusivamente de consultas no
  Prometheus — útil em debug local e em análise de logs estruturados.
- **`INFO` para sucesso e falha de validação, `ERROR` só para bugs.** Validação
  que falha (senha inválida) é o caminho normal — vai como `INFO`, não `ERROR`.
  `ERROR` fica reservado para o handler catch-all (algo realmente quebrou).
- **`logger` em `companion object`.** Idiomático Kotlin; evita instanciar um
  logger por instância da classe. Equivalente ao `private static final
  Logger LOG = ...` do Java.

### O que **não** é logado

- A senha (`request.password`) — em nenhum nível.
- O hash da senha — habilita correlação que o sistema não precisa fazer.
- Cabeçalhos do request — Spring Boot loga só o método/URI por default,
  o que é seguro.

### Próximos passos para produção

Quando sair do desafio para produção real:

1. **`logback-spring.xml`** com encoder JSON
   (`net.logstash.logback:logstash-logback-encoder`) sob profile `!dev`,
   para shipar a CloudWatch / ELK / Datadog.
2. **MDC (`Mapped Diagnostic Context`)** com `traceId`/`spanId` injetados
   por OpenTelemetry, permitindo correlação log↔trace.
3. **Sampling de logs** se volume virar problema (não é o caso para um
   validador, mas vale citar).
4. **Mascaramento defensivo:** filtro Logback que faz scan dos
   campos serializados para garantir que se uma senha vazar por engano em
   `toString()` de outra DTO, ela seja mascarada.

## Próximos passos para métricas

1. **SLOs definidos**: error rate < 0.1%.
2. **Painel Grafana** com `validations_total`, `violations_total` quebrado por
   regra (top-N).
3. **Alertas** em:
   - taxa de `result=invalid` subindo abruptamente (possível ataque ou
     mudança de cliente);
   - erro 500 em `handleUnexpected` (qualquer ocorrência).
4. **Timer de duração** reintroduzido quando regras com I/O entrarem
   (HIBP — ver [`07-next-steps.md`](07-next-steps.md)).
5. **Distributed tracing** quando integrações externas entrarem.

## Tabela-resumo: o que está pronto vs. o que é roadmap

| Item | Status |
|---|---|
| Counter validations / result | ✅ |
| Counter violations / rule | ✅ |
| Timer de duração | ❌ removido (sem I/O, overhead desnecessário) |
| `/actuator/prometheus` em dev/test/staging | ✅ |
| `/actuator/prometheus` desabilitado em prod | ✅ (intencional) |
| Log INFO no resultado da validação (válida e inválida) | ✅ |
| Log com regras violadas detalhadas (`violatedRules=[...]`) | ✅ |
| Log ERROR em exception inesperada | ✅ |
| Log DEBUG em body malformado | ✅ |
| Logger em companion object | ✅ |
| Senha excluída de toda telemetria | ✅ |
| `logback-spring.xml` com JSON encoder | ⏳ (roadmap) |
| MDC com traceId | ⏳ (roadmap) |
| SLOs + alertas | ⏳ (roadmap) |
| Timer reintroduzido para regras com I/O | ⏳ (roadmap) |
