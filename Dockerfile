# Multi-stage Dockerfile for EcoCommute Web on Google Cloud Run (Java 21)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Install Maven
RUN apk add --no-cache curl tar && \
    curl -sL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz | tar -xz -C /opt && \
    ln -s /opt/apache-maven-3.9.6/bin/mvn /usr/bin/mvn

# Copy source code and build
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Runtime Stage (Minimal JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Expose standard Cloud Run Port
ENV PORT=8080
EXPOSE 8080

COPY --from=builder /app/target/ecocommute-backend-1.0.0.jar app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
