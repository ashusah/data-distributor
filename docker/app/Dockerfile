FROM maven:3.9.0-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY data-distributor/pom.xml ./
COPY data-distributor/src ./src
COPY data-distributor/appsettings ./appsettings
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/target/data-distributor-0.0.1-SNAPSHOT.jar ./app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
