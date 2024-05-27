 FROM openjdk:17-jdk

 VOLUME /tmp

 EXPOSE 8080

 ARG JAR_FILE=build/libs/*.jar
 COPY build/libs/money-me-backend-0.0.1-SNAPSHOT.jar app.jar
 COPY money-me-ab62c-firebase-adminsdk-8ljd1-98d54fc4cb.json /app/serviceAccountKey.json
 ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]