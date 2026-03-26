# Multi-stage build for Spring Boot application on Render
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:resolve

COPY backend/src ./src
RUN mvn clean package -DskipTests

# Final stage
FROM eclipse-temurin:21-jre-jammy

# Set working directory
WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Render will set PORT env variable)
EXPOSE 8080

# Enable container to accept a PORT environment variable
ENV PORT=8080
ENV SERVER_PORT=8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:${PORT}/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Xmx512m", "-Xms128m", "-server", "-jar", "app.jar"]
