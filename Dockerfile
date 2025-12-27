FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY . /workspace

# Use gradle wrapper to ensure version consistency with gradle-wrapper.properties
# Skip tests for faster Docker image builds (TODO: Re-enable tests after fixing)
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