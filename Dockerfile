FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -X

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/clickhouse-rate-limiter-0.0.1-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]

