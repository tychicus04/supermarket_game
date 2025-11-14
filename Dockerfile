# Multi-stage build for Supermarket Game Server
# Using Liberica JDK 8
# Stage 1: Build
FROM bellsoft/liberica-openjdk-alpine:8 AS builder

# Install build tools
RUN apk add --no-cache wget

WORKDIR /build

# Download SQLite JDBC driver
RUN mkdir -p lib

RUN wget https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.46.0.0/sqlite-jdbc-3.46.0.0.jar -O lib/sqlite-jdbc.jar

RUN wget https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar -O lib/slf4j-api.jar

RUN wget https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar -O lib/slf4j-simple.jar

# Copy server and shared source code
COPY SupermarketServer/src ./server-src
COPY Shared/src/models ./shared-models
COPY Shared/src/constants ./shared-constants

# Compile Java source files
RUN mkdir -p classes && \
    javac -cp "sqlite-jdbc.jar" \
    -d classes \
    shared-constants/*.java \
    shared-models/*.java \
    server-src/database/*.java \
    server-src/server/*.java

# Stage 2: Runtime (JRE for minimal size)
FROM bellsoft/liberica-openjre-alpine:8

WORKDIR /app

# Copy compiled classes and dependencies from builder
COPY --from=builder /build/classes ./classes

# Create lib directory and copy SQLite JDBC
RUN mkdir -p /app/lib
COPY --from=builder /build/lib ./lib

# Create directory for database
RUN mkdir -p /app/data

# Expose server port
EXPOSE 8888

# Set environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD nc -z localhost 8888 || exit 1

# Run the server
CMD java $JAVA_OPTS -cp "classes:lib/*" server.GameServer
