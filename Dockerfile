# ─────────────────────────────────────────────
# Single-stage: Copy pre-built JAR into image
# Build the JAR first with: mvn package -DskipTests
# ─────────────────────────────────────────────
FROM amazoncorretto:17-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the pre-built JAR
COPY target/wrapper-api-0.0.1-SNAPSHOT.jar app.jar

# Change ownership to non-root user
RUN chown appuser:appgroup app.jar

USER appuser

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/api/rag/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
