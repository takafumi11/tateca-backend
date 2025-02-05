FROM openjdk:17-jdk-slim

WORKDIR /app

COPY build/libs/*.jar app.jar

COPY serviceAccountKey.json /app/serviceAccountKey.json

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]