FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml ./pom.xml
COPY story-common/pom.xml ./story-common/pom.xml
COPY story-auth/pom.xml ./story-auth/pom.xml
COPY story-back/pom.xml ./story-back/pom.xml
COPY story-admin/pom.xml ./story-admin/pom.xml
COPY story-auth/src ./story-auth/src
RUN mvn -B -pl story-auth -am clean verify

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/story-auth/target/*.jar app.jar
EXPOSE 8001
ENTRYPOINT ["java", "-jar", "app.jar"]
