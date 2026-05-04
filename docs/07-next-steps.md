# 07 — Próximos passos (roadmap / material de apresentação)

Este documento existe para **mostrar como a aplicação evolui**. As 7 regras
estruturais do desafio são apenas um caso de validação; em produção, validar
senhas envolve outras dimensões — dados pessoais do usuário, listas de
senhas vazadas, bases de breaches conhecidos. Cada uma dessas dimensões pode
ser plugada na arquitetura atual com mudanças localizadas.

## 0. Por que o design já suporta evolução

O `PasswordValidator` recebe `List<PasswordRule>` por construtor. Adicionar
uma 8ª regra **estrutural** (algo que dependa só da `Password`) é trivial:
uma classe + `@Component`. O Spring injeta na lista, o validator passa a
considerá-la, e nenhum outro arquivo muda — Open/Closed na prática.

Os 3 cenários abaixo vão **além** disso: precisam de informação que a
`Password` sozinha não carrega (perfil do usuário, repositório de palavras
proibidas, serviço externo). Eles introduzem **novos ports** (interfaces na
camada `application/port/output/`) e novos adapters de saída.

## 1. Estratégia de composição (decidida)

Quando múltiplas validações coexistirem, **agregar todas as violações** em
um único response. Nunca fail-fast.

**Por quê:** OWASP Authentication Cheat Sheet:
> *"the error message should describe **every** complexity rule that the new
> password does not comply with, not just the 1st rule it doesn't comply with"*

Em código: o `PasswordValidator` já faz isso (itera todas as regras e coleta).
Para introduzir validações que precisem de I/O, manter a mesma forma —
executar todas, agregar resultados — mesmo quando uma das chamadas for cara
(ex: HIBP HTTP). Quando custo virar problema, otimização é cache local + TTL,
não fail-fast.

---

## 2. Cenário 1 — Senha + dados pessoais do usuário (PII)

### Motivação OWASP
> *"Is the user prevented from using his username or other account
> information (such as first or last name) in the password?"*
> — OWASP Testing for Weak Password Policy

### O problema a resolver
`MariaSilva88!` passa todas as 7 regras estruturais, mas se o usuário se chama
**Maria Silva** e nasceu em **1988**, a senha é trivialmente adivinhável.

### Design proposto

**Novo input port:**

```kotlin
interface ValidatePasswordForUserUseCase {
    fun perform(input: PasswordWithUserContext): ValidationResult
}

data class PasswordWithUserContext(
    val password: String,
    val username: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val birthDate: LocalDate? = null
)
```

**Nova categoria de regra (contextual):**

```kotlin
interface ContextualPasswordRule {
    val violation: RuleViolation
    fun isSatisfiedBy(password: Password, context: UserContext): Boolean
}

@Component
class PasswordDoesNotContainUsernameRule : ContextualPasswordRule {
    override val violation = RuleViolation.CONTAINS_USERNAME
    override fun isSatisfiedBy(password: Password, context: UserContext) =
        !password.value.contains(context.username, ignoreCase = true)
}
```

(Análogos para `ContainsFirstName`, `ContainsBirthYear`, etc.)

**Novo controller:** `POST /api/v1/users/passwords/validate` recebendo
`{username, password, firstName?, lastName?, birthDate?}`.

### Impacto em testes
- Unit test de cada regra contextual.
- Integration test do novo endpoint (request com dados PII).
- Os testes atuais **não mudam** — endpoint v1 segue intacto.

### Esforço estimado
**~1 dia** (3 regras + use case + controller + DTOs + testes).

---

## 3. Cenário 2 — Senha + blacklist de senhas comuns

### Motivação OWASP
> *"Is it possible to set common passwords such as Password1 or 123456?"*

### O problema a resolver
`Password1!a` passa todas as regras estruturais — mas é uma das 100 senhas
mais usadas do mundo. Atacantes começam por aí.

### Design proposto

**Novo output port:**

```kotlin
interface CommonPasswordRepository {
    fun contains(rawPassword: String): Boolean
}
```

**Adapter em memória:**

```kotlin
@Component
class InMemoryCommonPasswordRepository : CommonPasswordRepository {
    private val blacklist: Set<String> by lazy {
        javaClass.getResourceAsStream("/blacklist/top-10k-passwords.txt")
            .bufferedReader().readLines().toHashSet()
    }
    override fun contains(rawPassword: String) = blacklist.contains(rawPassword)
}
```

**Nova `PasswordRule` simples (não precisa do contexto contextual):**

```kotlin
@Component
class PasswordIsNotCommonRule(
    private val repository: CommonPasswordRepository
) : PasswordRule {
    override val violation = RuleViolation.COMMON_PASSWORD
    override fun isSatisfiedBy(password: Password) = !repository.contains(password.value)
}
```

### Impacto em testes
- Unit test da regra com `mock<CommonPasswordRepository>`.
- Integration test verificando que `Password1` retorna `COMMON_PASSWORD`.
- Resource: `src/main/resources/blacklist/top-10k-passwords.txt` (público,
  Have I Been Pwned ou SecLists).
- Sem mudança no contrato da API — regra entra na `List<PasswordRule>` do
  validator existente.

### Esforço estimado
**~½ dia.**

---

## 4. Cenário 3 — Senha + breach check (HaveIBeenPwned)

### Motivação OWASP
> ASVS V2.1.7: *"Verify that passwords are checked against a set of breached
> passwords."*

### O problema a resolver
Mesmo senhas únicas podem já ter sido vazadas em breaches conhecidos. HIBP
mantém um índice de ~600 milhões de senhas comprometidas. K-anonymity
permite consultar sem expor a senha completa.

### Design proposto

**Novo output port:**

```kotlin
interface BreachedPasswordChecker {
    fun isBreached(rawPassword: String): Boolean
}
```

**Adapter HTTP (Spring `RestClient` + k-anonymity SHA-1):**

```kotlin
@Component
class HibpBreachedPasswordChecker(
    private val restClient: RestClient,
    @Value("\${validation.hibp.fail-open:true}") private val failOpen: Boolean
) : BreachedPasswordChecker {

    override fun isBreached(rawPassword: String): Boolean = try {
        val hash = sha1(rawPassword).uppercase()
        val prefix = hash.substring(0, 5)
        val suffix = hash.substring(5)
        val response = restClient.get()
            .uri("https://api.pwnedpasswords.com/range/{prefix}", prefix)
            .retrieve().body(String::class.java).orEmpty()
        response.lineSequence().any { it.startsWith(suffix) }
    } catch (ex: Exception) {
        if (failOpen) false else throw ex
    }
}
```

**Nova `PasswordRule`:** análoga à `PasswordIsNotCommonRule`, injetando o
checker.

### Política de resiliência
- **`fail-open` configurável:** se o HIBP estiver fora, a regra **não bloqueia**
  o usuário (default: `true`). Decisão de produto — discutir com o time de
  segurança.
- **Resilience4j:** envolver o checker com `@CircuitBreaker` + `@TimeLimiter`
  para não amarrar requests em chamadas externas lentas.

### Impacto em testes
- Unit test da regra com `mock<BreachedPasswordChecker>`.
- Test do adapter com `MockWebServer` (OkHttp) — fixtures de respostas reais
  do HIBP.
- Integration test com fake checker que sempre devolve `true`/`false`
  (`@TestConfiguration` overriding o bean).
- Documentar a política `fail-open` em `docs/02-design-decisions.md`.

### Esforço estimado
**1-2 dias** (adapter + testes + resilience4j + docs).

---

## 5. Comparativo

| Critério | Cenário 1 (PII) | Cenário 2 (Blacklist) | Cenário 3 (HIBP) |
|---|---|---|---|
| Esforço | Médio | Baixo | Alto |
| Dependência externa | Não (interno ao request) | Não (resource local) | Sim (HTTP) |
| Demonstra hexagonal | Médio (novo controller) | Baixo (regra simples) | Alto (port + adapter de saída + resilience) |
| Valor de segurança | Alto | Médio | Alto |
| Quebra contrato atual | Sim (novo endpoint) | Não | Não |

## 6. Recomendação de ordem

**2 → 1 → 3.**

Justificativa:
1. **Cenário 2 (blacklist)** entrega valor imediato com pouquíssimo código e
   nenhum acoplamento externo. É o melhor ROI.
2. **Cenário 1 (PII)** muda a forma do contrato — vale fazer cedo, antes que o
   contrato v1 esteja sendo usado por muitos clientes (forçando v2).
3. **Cenário 3 (HIBP)** introduz dependência externa e carga operacional
   (monitoring, fallback, latência). Vale por último, quando os anteriores já
   estiverem maduros.

## 7. Outros próximos passos não-funcionais

- **Métricas Prometheus** via `micrometer-registry-prometheus` — `password_validations_total{result=valid|invalid}` e histograma de latência.
- **CI** com GitHub Actions: `./gradlew check` em cada push, badge de cobertura.
- **Dockerfile** multi-stage para imagem produção (build com JDK, runtime com JRE).
- **Helm chart** se for empacotar para Kubernetes.
- **Rate limiting** no endpoint via Bucket4j ou um API gateway upstream.
- **Trace distribuído** com OpenTelemetry quando integrações externas entrarem.
