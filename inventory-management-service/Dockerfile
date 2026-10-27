FROM maven:3.9.16-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY inventory-management-service/pom.xml inventory-management-service/pom.xml
COPY supplier-availability-service/pom.xml supplier-availability-service/pom.xml
RUN mvn -B -ntp -pl inventory-management-service -am dependency:go-offline

COPY inventory-management-service/src inventory-management-service/src
COPY supplier-availability-service/src supplier-availability-service/src
RUN mvn -B -ntp -pl inventory-management-service -am clean package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system --gid 10001 stockbridge \
    && useradd --system --uid 10001 --gid stockbridge --no-create-home stockbridge
COPY --from=build --chown=10001:10001 /workspace/inventory-management-service/target/inventory-management-service-0.0.1-SNAPSHOT.jar /app/app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
