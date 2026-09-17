# ==============================================================================
# Build Stage - GraalVM 25 Native Image Multi-Stage Builder (Spring Boot 4 / Kotlin 2.4)
# ==============================================================================
FROM container-registry.oracle.com/graalvm/native-image:latest AS builder

# Inject Maven 3.9 from official image
COPY --from=maven:3.9-eclipse-temurin-25 /usr/share/maven /usr/share/maven
ENV MAVEN_HOME=/usr/share/maven
ENV PATH="/usr/share/maven/bin:${PATH}"

WORKDIR /app

# 1. Dependency caching layer
COPY pom.xml .
RUN mvn dependency:go-offline -B -Pnative || true

# 2. Source compilation & GraalVM Native Image generation
COPY src ./src
RUN mvn -Pnative native:compile -DskipTests -B

# ==============================================================================
# Runtime Stage (Non-root Minimal Debian Slim)
# ==============================================================================
FROM debian:bookworm-slim

WORKDIR /app

# Install runtime dependencies (glibc, zlib, CA certificates)
RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates libc6 zlib1g && \
    rm -rf /var/lib/apt/lists/*

# Run as unprivileged UID 10001 (PSS Restricted Compliance)
RUN groupadd -g 10001 darueira && \
    useradd -u 10001 -g darueira -s /bin/sh -m darueira && \
    mkdir -p /app/data && \
    chown -R darueira:darueira /app

USER 10001:10001

# Copy the native image binary from the builder stage
COPY --from=builder --chown=10001:10001 /app/target/bookanything-monolith-backend-01 /app/bookanything-monolith-backend-01

EXPOSE 8060

ENV SPRING_PROFILES_ACTIVE="darueira-k8s"

ENTRYPOINT ["/app/bookanything-monolith-backend-01"]
