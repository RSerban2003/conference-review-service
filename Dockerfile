# Build stage
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon bootJar

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/reviews-microservice/build/libs/*.jar app.jar
# The demo profile stubs the external Users/Submissions microservices and
# seeds demo data, so the container is self-contained out of the box.
# Override with -e SPRING_PROFILES_ACTIVE= to run against real services.
ENV SPRING_PROFILES_ACTIVE=demo
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
