# Build both JARs with Maven verify before running this isolated HTTP suite.
FROM eclipse-temurin:21-jre-jammy
ARG MODULE
WORKDIR /app
COPY ${MODULE}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
