FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

# Copy Gradle wrapper and configuration files first for better layer caching
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# Download dependencies separately to leverage Docker layer caching
# This layer will be cached unless build files change
# Retry logic for transient network failures
RUN for i in 1 2 3; do \
        ./gradlew dependencies --no-daemon --refresh-dependencies && break || \
        (echo "Attempt $i failed, retrying..." && sleep 10); \
    done || true

# Copy source code
COPY src src

# Build application (skip tests for faster builds)
# Retry logic for transient network failures
RUN for i in 1 2 3; do \
        ./gradlew build -x test --no-daemon && break || \
        (echo "Build attempt $i failed, retrying..." && sleep 10); \
    done

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