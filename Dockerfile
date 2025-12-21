FROM gradle:8.10-jdk21 AS builder

WORKDIR /workspace

COPY . /workspace

# Skip tests as they are already run in CI pipeline
RUN gradle build -x test --no-daemon

FROM eclipse-temurin:21-jdk

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