 FROM openjdk:17-jdk

 VOLUME /tmp

 EXPOSE 8080

 ARG JAR_FILE=build/libs/*.jar
 COPY build/libs/money-me-backend-0.0.1-SNAPSHOT.jar app.jar
 ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]