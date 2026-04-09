# ---- build stage ----
FROM gradle:9.4-jdk17 AS build
WORKDIR /workspace

# 1. Копируем "инфраструктуру" (мозги и словарь проекта)
COPY gradle/ gradle/
COPY buildSrc/ buildSrc/
COPY build.gradle.kts settings.gradle.kts ./

# 2. Копируем исходники конкретного модуля, многомодульном проекте важно сохранять структуру папок
COPY routing-engine/ routing-engine/

# 3. Собираем модуль, --no-daemon для CI/CD и Docker окружений
RUN gradle :routing-engine:bootJar --no-daemon

# ---- runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/routing-engine/build/libs/*.jar /app/routing-engine.jar

EXPOSE 8001
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/routing-engine.jar"]
