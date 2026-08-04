# Build with the Gradle wrapper, so the build always uses the version this repo is pinned to.
FROM eclipse-temurin:21 AS build
WORKDIR /home/gradle/gropius-backend
ADD . .
ARG module
RUN ./gradlew --no-daemon clean ${module}:build

FROM eclipse-temurin:21
ARG module
WORKDIR /home/java
COPY --from=build /home/gradle/gropius-backend/${module}/build/libs/${module}.jar app.jar
CMD java -jar ./app.jar
