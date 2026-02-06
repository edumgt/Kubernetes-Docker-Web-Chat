FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/*.jar api-gateway.jar

EXPOSE 9999

ENTRYPOINT ["java", "-jar", "api-gateway.jar"]
