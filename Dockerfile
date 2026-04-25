FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
RUN groupadd -r catalog && useradd -r -g catalog catalog
USER catalog
COPY --from=builder /workspace/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080
