FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradle ./gradle
COPY gradlew build.gradle settings.gradle ./
# Also supports a build context prepared from a Windows checkout.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
COPY src/main ./src/main
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /workspace/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
