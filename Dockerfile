# ----------------------------------
# Stage 1: Build the JAR (Compiler)
# ----------------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy generic Pom.xml
COPY pom.xml .
# Copy source code
COPY src ./src

# Build it (skip tests to be faster)
RUN mvn clean package -DskipTests

# ----------------------------------
# Stage 2: Run the JAR (Runtime)
# ----------------------------------
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Create the logs directory so the app can write to it
RUN mkdir -p /app/logs

# Copy from Stage 1 (the built jar)
COPY --from=build /app/target/*.jar app.jar

# Run — default port is 8080 if PORT env var is not set
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
