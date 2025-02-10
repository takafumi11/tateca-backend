FROM gradle:7.6-jdk17 AS builder

WORKDIR /workspace

COPY . /workspace

RUN gradle build --no-daemon

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=builder workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms64m", "-Xmx128m", "-jar", "app.jar"]