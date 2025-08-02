FROM gradle:7.6-jdk17 AS builder

WORKDIR /workspace

COPY . /workspace

RUN gradle build --no-daemon

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=builder workspace/build/libs/*.jar app.jar

# Environment variable for Spring profiles (default to prod for containers)
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-Xms128m", "-Xmx128m", "-Duser.timezone=UTC", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]