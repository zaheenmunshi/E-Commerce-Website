# syntax=docker/dockerfile:1
# One image for the whole site: the Spring Boot jar serves the compiled frontend.
# Build from the repo root:  docker build -t ecommerce .

# ---- 1. frontend: HTML/CSS/TypeScript -> frontend/dist ----
FROM node:24-alpine AS frontend
WORKDIR /src/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---- 2. backend: Spring Boot jar (Java 25, Amazon Corretto) with the frontend bundled under /static ----
FROM amazoncorretto:27-alpine AS backend
WORKDIR /src/backend
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn
RUN ./mvnw -B -q dependency:go-offline
COPY backend/src src
COPY --from=frontend /src/frontend/dist /src/frontend/dist
RUN ./mvnw -B -q -DskipTests package && cp target/ecommerce-backend-*.jar /src/app.jar

# ---- 3. runtime: Corretto 25, non-root, no build tools, sources or secrets ----
FROM amazoncorretto:27-alpine
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app
WORKDIR /app
COPY --from=backend --chown=app:app /src/app.jar app.jar
# On AWS the root filesystem is read-only; /tmp is the one writable mount (Tomcat, JVM).
# Declaring it here makes Fargate seed the mount with the image's world-writable /tmp.
VOLUME ["/tmp"]
USER 10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
