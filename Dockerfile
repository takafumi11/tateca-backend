FROM eclipse-temurin:25-jdk AS builder

WORKDIR /workspace

# Layer 1: Copy Gradle wrapper and build files (changes rarely)
COPY gradle gradle
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle.kts .
COPY build.gradle.kts .

# Layer 2: Download dependencies (cached unless build.gradle.kts changes)
# Preserve Gradle cache in layer for faster subsequent builds
RUN ./gradlew dependencies --no-daemon || true

# Layer 3: Copy config and source code
COPY config config
COPY src src

# Layer 4: Build application (tests run in CI, skip here)
# Skip static analysis for faster CI builds
RUN ./gradlew build -x test -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest --no-daemon

FROM eclipse-temurin:25-jre

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