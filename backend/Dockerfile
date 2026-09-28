# Multi-stage build so the final image ships only the built jar and a JRE,
# not the whole Maven toolchain and dependency cache.

# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy just the wrapper + POM first so Docker can cache the downloaded
# dependencies in their own layer, and only re-download when pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render (and most PaaS hosts) inject PORT at runtime; application.yml already
# reads it via server.port: ${PORT:8080}. 8080 here is just the local default.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
