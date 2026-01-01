FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

# Layer 1: Copy Gradle wrapper and config files (changes rarely)
COPY gradle gradle
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY config config

# Layer 2: Download dependencies (cached unless build.gradle.kts changes)
RUN ./gradlew dependencies --no-daemon || true

# Layer 3: Copy source code (changes frequently)
COPY src src

# Layer 4: Build application (tests run in CI, skip here)
RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder workspace/build/libs/*.jar app.jar

# Environment variable for Spring profiles (default to prod for containers)
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:TieredStopAtLevel=1", \
    "-XX:+UseG1GC", \
    "-XX:+UnlockExperimentalVMOptions", \
    "-XX:+UseStringDeduplication", \
    "-Xms256m", "-Xmx256m", \
    "-Duser.timezone=UTC", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", \
    "-Dspring.jmx.enabled=false", \
    "-Dspring.main.lazy-initialization=true", \
    "-jar", "app.jar"]