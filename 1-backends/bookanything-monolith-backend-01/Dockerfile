# ==============================================================================
# Build Stage - Spring Boot 3.5 Kotlin Maven Multi-Stage Builder (Java 21)
# ==============================================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -B

# ==============================================================================
# Runtime Stage (Non-root, Minimal Temurin 21 JRE)
# ==============================================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Run as unprivileged UID 10001 (PSS Restricted Compliance)
RUN groupadd -g 10001 darueira && \
    useradd -u 10001 -g darueira -s /bin/sh -m darueira && \
    mkdir -p /app/data && \
    chown -R darueira:darueira /app

USER 10001:10001

COPY --from=builder --chown=10001:10001 /app/target/bookanything-monolith-backend-01-*.jar /app/app.jar

EXPOSE 8060

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE="darueira-k8s"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar /app/app.jar"]
