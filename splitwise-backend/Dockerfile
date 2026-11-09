# Session 17: the Splitwise backend as a container image, built in two stages.
# Build from the REPOSITORY ROOT (the module needs the parent pom and the Maven wrapper):
#   docker build -f splitwise-backend/Dockerfile -t splitwise-backend .

# ---- stage 1: build the jar with the full JDK and Maven (this stage is thrown away afterwards) ----
FROM eclipse-temurin:21-jdk AS build
# The Maven wrapper downloads the .zip only when `unzip` exists; without it, it fetches the .tar.gz,
# whose checksum doesn't match the one pinned in maven-wrapper.properties, and the build stops with
# "Failed to validate Maven distribution SHA-256" (found while building this image; CI runners have unzip).
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*
WORKDIR /src
COPY . .
RUN ./mvnw -B -q -pl splitwise-backend -am package -DskipTests

# ---- stage 2: only a Java runtime and the jar: a smaller image with fewer tools for an attacker ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# never run as root inside the container
RUN useradd --system --uid 10001 splitwise
COPY --from=build /src/splitwise-backend/target/splitwise-backend-1.0.0-SNAPSHOT.jar app.jar
USER splitwise
EXPOSE 8080
# the prod profile reads the database, Redis and admin settings from environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
