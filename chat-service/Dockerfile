FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/*.jar chat-service.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "chat-service.jar"]
