# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy POM first for caching
COPY src/financeforge-api/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src/financeforge-api/src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar ./financeforge-api.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "financeforge-api.jar"]
