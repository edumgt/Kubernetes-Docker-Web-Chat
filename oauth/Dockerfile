FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/*.jar auth-service.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "auth-service.jar"]
