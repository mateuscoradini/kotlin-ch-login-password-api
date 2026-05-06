# syntax=docker/dockerfile:1.7

# --- build stage -------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copia primeiro só o necessário para resolver dependências (cache friendly)
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Copia o código e empacota o jar (sem testes — testes rodam no CI)
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

# --- runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuário não-root (boa prática de segurança em containers)
RUN groupadd --system app && useradd --system --gid app --home /app app
USER app

COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""

# Healthcheck via Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

