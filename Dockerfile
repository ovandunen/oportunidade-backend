# =============================================================================
# Multi-stage Dockerfile for Oportunidade Recruiting (Quarkus)
# =============================================================================
# Build:  docker build -t oportunidade/recruiting .
# Run:    docker run -p 8080:8080 oportunidade/recruiting
#
# Override DB at runtime:
#   docker run -p 8080:8080 -e DB_URL=jdbc:postgresql://host.docker.internal:5432/odoo_payments oportunidade/recruiting
# =============================================================================

# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and pom
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached unless pom changes)
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests -B

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine

WORKDIR /deployments

# Create non-root user
RUN addgroup -g 1000 app && adduser -u 1000 -G app -s /bin/sh -D app

# Copy Quarkus app from build stage
COPY --from=build /app/target/quarkus-app/lib/ lib/
COPY --from=build /app/target/quarkus-app/*.jar .
COPY --from=build /app/target/quarkus-app/app/ app/
COPY --from=build /app/target/quarkus-app/quarkus/ quarkus/

RUN chown -R app:app /deployments

USER app

EXPOSE 8080

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0"

# Use shell form so JAVA_OPTS is applied
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar quarkus-run.jar"]
