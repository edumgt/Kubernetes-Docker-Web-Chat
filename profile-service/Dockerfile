FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/*.jar profile-service.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "profile-service.jar"]
