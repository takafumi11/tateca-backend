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

# Build arguments for metadata (injected by GitHub Actions)
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION=latest

# OCI standard labels for better image metadata
LABEL org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.authors="Tateca Team" \
      org.opencontainers.image.url="https://github.com/takafumi11/tateca-backend" \
      org.opencontainers.image.source="https://github.com/takafumi11/tateca-backend" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.title="Tateca Backend" \
      org.opencontainers.image.description="Spring Boot backend for Tateca group expense management"

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

# Health check for container orchestration platforms
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

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