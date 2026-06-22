FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN apk add --no-cache netcat-openbsd
RUN addgroup -S interniq && adduser -S interniq -G interniq
COPY --from=build /app/target/*.jar /app/app.jar
RUN mkdir -p /app/uploads/resumes && chown -R interniq:interniq /app

USER interniq
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
