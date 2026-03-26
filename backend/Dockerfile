FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -q clean package -DskipTests
RUN cp target/*.jar /app/app.jar

FROM eclipse-temurin:21-jre-jammy
COPY --from=build /app/app.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]