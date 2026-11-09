# Stage 1: Build the backend distribution using Gradle & JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Copy Gradle wrapper and configuration files first for layer caching
COPY gradlew .
COPY gradle/ gradle/
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY gradle/libs.versions.toml gradle/

# Copy required module source trees
COPY core/common/ core/common/
COPY core/domain/ core/domain/
COPY backend/ backend/

# Ensure execution permission on gradlew and build the application distribution
RUN chmod +x gradlew && ./gradlew :backend:installDist --no-daemon --stacktrace

# Stage 2: Minimal, secure JRE 21 Alpine runtime image
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="YAAN Backend CRDT Sync Relay"
LABEL org.opencontainers.image.description="Self-hosted zero-knowledge CRDT sync relay and backup server for YAAN"
LABEL org.opencontainers.image.source="https://github.com/developmentwithyash/yaan"
LABEL org.opencontainers.image.licenses="MIT"

WORKDIR /app

# Run as non-root user for container security hardening
RUN addgroup -S yaan && adduser -S yaan -G yaan

# Copy built distribution from builder stage
COPY --from=builder --chown=yaan:yaan /workspace/backend/build/install/backend /app

USER yaan

# Default ports and environment
ENV PORT=8080
ENV HOST=0.0.0.0
ENV ENVIRONMENT=production

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q -O - http://127.0.0.1:8080/healthz || exit 1

ENTRYPOINT ["/app/bin/backend"]
