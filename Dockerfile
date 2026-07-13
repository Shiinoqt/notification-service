FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/notification-service-0.0.1-SNAPSHOT.jar notification.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "notification.jar"]