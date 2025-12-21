# Build stage
FROM gradle:8.10-jdk21 AS builder

WORKDIR /workspace

# Copy gradle wrapper and build files first for better caching
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src src

# Skip tests during Docker build as Testcontainers requires Docker-in-Docker
# Tests are already run in CI/CD pipeline (GitHub Actions)
RUN gradle build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre

# Create non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /workspace/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

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