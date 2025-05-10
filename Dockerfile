# Build stage using GraalVM with Java 21
FROM ghcr.io/graalvm/graalvm-community:21 AS builder

# Install necessary packages
RUN microdnf install -y findutils zip unzip

WORKDIR /workspace/app

# Copy gradle files first for better layer caching
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

# Give execution permissions to gradlew
RUN chmod +x ./gradlew

# Download dependencies
RUN ./gradlew dependencies

# Copy source code
COPY src/ src/

# Build the native image
RUN ./gradlew nativeCompile

# Runtime stage
FROM oraclelinux:9-slim

# Install minimal runtime dependencies
RUN microdnf install -y freetype fontconfig curl \
    && microdnf clean all

WORKDIR /app

# Copy the native executable
COPY --from=builder /workspace/app/build/native/nativeCompile/outbox_pattern /app/outbox_pattern

# Set the executable as entrypoint
ENTRYPOINT ["/app/outbox_pattern"]

# Expose the application port
EXPOSE 8081